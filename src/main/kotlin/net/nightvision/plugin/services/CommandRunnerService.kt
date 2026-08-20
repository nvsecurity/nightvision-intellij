package net.nightvision.plugin.services

import com.intellij.execution.ExecutionException
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.diagnostic.Logger
import net.nightvision.plugin.Constants.Companion.NIGHTVISION
import net.nightvision.plugin.exceptions.CommandNotFoundException
import net.nightvision.plugin.exceptions.PermissionDeniedException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.EnvironmentUtil
import net.nightvision.plugin.exceptions.NotLoggedException
import net.nightvision.plugin.services.InstallCLIService.userCliVersion
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit

/**
 * A centralized service for running external shell commands with optional async/sync execution.
 */
@Service(Level.APP)
object CommandRunnerService {
    private val LOG = Logger.getInstance(CommandRunnerService::class.java)

    fun getDestinationDirForPlatform(): String {
        return if (System.getProperty("os.name").startsWith("Windows")) {
            "${System.getProperty("user.home")}\\AppData\\Local\\NightVision\\bin"
        } else {
            "${System.getProperty("user.home")}/.local/nightvision/bin"
        }
    }

    data class ExecutionResponse(val output: String, val error: String)

    /**
     * The phrase an expired login is reclassified on. Named here because two
     * things depend on it: getSpecificRuntimeException matches it, and
     * significantOutput must never drop the line carrying it.
     */
    const val EXPIRED_TOKEN_PHRASE = "token has expired. Please try to log in again"

    /**
     * How many lines of CLI output a message keeps. No screen in this plugin
     * scrolls, so an unbounded detail runs off the tool window and takes the
     * reason with it. The tail is what survives, because the CLI narrates
     * forward and fails last.
     */
    private const val MAX_DETAIL_LINES = 12

    /** The CLI's progress narration: timestamped, INFO level, one per step. */
    private val PROGRESS_LINE = Regex("""^\[[^\]]*]\s+INFO\b""")

    /**
     * The part of the CLI's output worth putting in front of a user. A scan
     * that fails prints a page of INFO lines covering DNS, TCP and TLS before
     * saying what went wrong, which pushed the reason off the screen entirely.
     * Fatal errors are printed plain rather than logged (NV-4827), so dropping
     * the INFO narration keeps every reason while removing the noise.
     *
     * If nothing survives, the narration was all the CLI said, so its tail is
     * kept rather than claiming the CLI gave no reason at all.
     */
    fun significantOutput(text: String): String {
        val lines = text.trim().lines().map { it.trim() }.filter { it.isNotEmpty() }
        val kept = lines.filter {
            !PROGRESS_LINE.containsMatchIn(it) || EXPIRED_TOKEN_PHRASE in it
        }
        val chosen = if (kept.isNotEmpty()) kept else lines
        return chosen.takeLast(MAX_DETAIL_LINES).joinToString("\n")
    }

    /**
     * The CLI's own account of a failure, preferring stderr but falling back to
     * stdout. Several CLI errors are printed to stdout rather than logged, so a
     * message built from stderr alone is empty for exactly the failures a user
     * most needs explained (NV-4827).
     *
     * The result reaches getSpecificRuntimeException, which reclassifies on the
     * expired-token phrase, so the fallback could in principle widen that match
     * to stdout. It does not, on either of the two caller shapes. runCommandSync
     * hands the streams over separately, and the CLI emits that phrase only
     * through its logger, which writes to stderr, so stderr is non-empty
     * whenever the phrase is present and the fallback is not reached for it.
     * The scan-startup caller has no separate stderr to hand over, since
     * awaitStartup merges both streams into one buffer, so it passes that
     * buffer as stdout and an empty stderr and the fallback runs every time.
     * What it returns still carries the CLI's stderr, which is the only place
     * the phrase can have come from, so the match is no wider there either.
     */
    fun failureDetail(stdout: String, stderr: String): String {
        val err = stderr.trim()
        if (err.isNotEmpty()) {
            return significantOutput(err)
        }
        return significantOutput(stdout)
    }

    /**
     * Message for a CLI run that ended without the record the caller needed,
     * where [what] names the action in the infinitive ("create the project").
     * The CLI's own output carries the reason, so it is reported rather than
     * replaced with a generic failure (NV-4827).
     *
     * The runCommandSync callers only arrive here after a zero exit, since
     * handleProcessResponse throws on anything else, but the scan-startup
     * caller arrives on any exit that precedes the marker. Nothing below reads
     * the exit status, so the two are formatted alike; do not add a branch that
     * assumes a clean exit.
     */
    fun missingRecordMessage(what: String, stdout: String, stderr: String): String {
        val detail = failureDetail(stdout, stderr)
        if (detail.isEmpty()) {
            return "The NightVision CLI did not $what, and reported no reason."
        }
        return "The NightVision CLI did not $what:\n$detail"
    }

    private fun handleProcessResponse(
        command: String,
        output: ProcessOutput
    ): ExecutionResponse {
        if (!output.isTimeout && output.exitCode == 0) {
            // The command line only, deliberately, never output.stdout. This is
            // the shared success path for every runCommandSync caller, and
            // TokenService.createToken reads a live API token off it, so
            // logging the output here would put credentials in idea.log. The
            // println this replaced did exactly that on every login.
            //
            // The failure branch below logs failureDetail, which can fall back
            // to stdout, and that is not the same exposure: the CLI prints a
            // token as the very last statement of a successful run, after every
            // failure path has already exited, so a run that failed has no
            // token on stdout to fall back to. Note that plenty of other CLI
            // errors do reach stdout, which is why the fallback exists at all.
            LOG.debug("Command succeeded: ${command}")
        } else {
            val detail = failureDetail(output.stdout, output.stderr)
            LOG.warn("Command exited with ${output.exitCode}: ${command}. Detail: ${detail}")
            // Worded for the person who clicked something, not for whoever
            // reads the log: "the command" names nothing they did, and an exit
            // code tells them nothing they can act on. Both are already in the
            // LOG.warn above, which is where they are useful. This is also the
            // wording missingRecordMessage uses for the failures that exit 0,
            // so the two shapes of the same failure now read alike (NV-4827).
            if (output.isTimeout) {
                throw RuntimeException("The NightVision CLI did not respond in time.")
            } else if (detail.isEmpty()) {
                throw RuntimeException("The NightVision CLI failed without reporting a reason.")
            } else {
                throw RuntimeException(
                    "The NightVision CLI reported an error:\n${detail}"
                )
            }
        }

        return ExecutionResponse(
            output=output.stdout.trim(),
            error=output.stderr.trim()
        )
    }

    fun getPathForGeneralCommandLine(): String {
        // Resolve the base PATH from the login shell rather than the raw process
        // environment. A GUI-launched IDE (Dock, Finder, Toolbox) inherits a
        // minimal launchd PATH, so System.getenv("PATH") would miss a CLI on the
        // user's shell PATH and wrongly show the Install CLI screen. EnvironmentUtil
        // captures the login-shell environment, matching how the rest of the IDE
        // resolves PATH-sensitive tools. NV-4586.
        val basePath = EnvironmentUtil.getValue("PATH") ?: ""
        return prependCliDir(getDestinationDirForPlatform(), basePath)
    }

    /**
     * Prepend the CLI install dir [destDir] to [basePath] unless it is already
     * first. Kept separate from the environment lookup so it can be unit tested
     * without a live platform.
     */
    fun prependCliDir(destDir: String, basePath: String): String {
        return if (basePath.startsWith(destDir)) {
            basePath
        } else {
            destDir + File.pathSeparator + basePath
        }
    }

    /**
     * Executes the given command synchronously, waiting up to [timeout] [unit].
     * Returns stdout as a trimmed String, or throws an exception on failure.
     */
    @Throws(CommandNotFoundException::class, PermissionDeniedException::class, IOException::class, Exception::class)
    fun runCommandSync(
        vararg command: String,
        workingDirectory: String = "",
        timeout: Long = 30,
        unit: TimeUnit = TimeUnit.SECONDS
    ): ExecutionResponse {
        try {
            var cmd = GeneralCommandLine(*command)
                .withEnvironment("PATH", getPathForGeneralCommandLine())
            if (workingDirectory.isNotEmpty()) {
                cmd = cmd.withWorkDirectory(workingDirectory)
            }
            val capHandler = CapturingProcessHandler(cmd)
            val output: ProcessOutput = capHandler.runProcess(unit.toMillis(timeout).toInt())

            return handleProcessResponse(command.joinToString(" "), output)
        } catch (e: IOException) {
            throw getSpecificException(command.toList(), e)
        } catch (e: RuntimeException) {
            throw getSpecificException(command.toList(), e)
        } catch (e: ExecutionException) {
            throw e
        }

    }

    /**
     * Names a nightvision executable can take on Windows, in the relative order
     * PATHEXT gives them by default, so that where a directory holds more than
     * one the resolved path names the same file a shell would run. Getting the
     * order wrong would misreport exactly the ambiguous case the NV-4873
     * tooltip exists to settle. PATHEXT ranks .COM ahead of all of these, but
     * nothing ships nightvision in the DOS .com format, so it is not looked up.
     */
    private val WINDOWS_CLI_NAMES = listOf("nightvision.exe", "nightvision.bat", "nightvision.cmd")

    /**
     * Absolute path of the nightvision the plugin will actually run, or null if
     * none is on the search path.
     *
     * getPathForGeneralCommandLine prepends the plugin's own install directory,
     * so a copy it installed once takes precedence over any newer CLI the user
     * has installed since. Nothing surfaced which binary was in use, leaving a
     * user running a current CLI in their terminal no way to see that the
     * plugin was using an old one (NV-4873).
     */
    fun resolveCliPath(): String? = resolveCliPathIn(getPathForGeneralCommandLine())

    /**
     * First nightvision executable on [searchPath]. Split from the environment
     * lookup so it can be unit tested against a constructed path.
     */
    fun resolveCliPathIn(
        searchPath: String,
        windows: Boolean = System.getProperty("os.name").startsWith("Windows"),
        isExecutable: (File) -> Boolean = { it.isFile && it.canExecute() }
    ): String? {
        // On Windows canExecute() is true for any readable file, so the name
        // carries the executability and the extension list is what matters.
        val names = if (windows) WINDOWS_CLI_NAMES else listOf(NIGHTVISION)
        for (dir in searchPath.split(File.pathSeparator)) {
            if (dir.isBlank()) {
                continue
            }
            for (name in names) {
                val candidate = File(dir, name)
                if (isExecutable(candidate)) {
                    return candidate.path
                }
            }
        }
        return null
    }

    /**
     * How often the startup wait re-checks whether the process is still alive.
     */
    private const val STARTUP_POLL_MS = 100L

    /**
     * Outcome of a command run under a startup deadline. Exactly one of
     * [started], [exited] and [timedOut] is true.
     *
     * @param started the marker appeared, so the scan was accepted. The
     *                process is normally still running; a marker seen as it
     *                ended on its own counts too, since the acceptance stands
     * @param exited  the process ended before the marker appeared
     * @param timedOut the deadline passed with the process still running and no
     *                 marker, so the process was destroyed
     * @param output   everything the process printed, both streams interleaved
     */
    data class StartupOutcome(
        val started: Boolean,
        val exited: Boolean,
        val timedOut: Boolean,
        val output: String
    )

    /**
     * Runs [command] and waits only until [marker] appears in its output rather
     * than for the process to finish, then leaves it running.
     *
     * A long-running command cannot be run through [runCommandSync]: that waits
     * for the whole process and destroys it at the timeout, so a scan lasting
     * longer than the timeout was killed mid-run. Killing the CLI sends SIGTERM,
     * which it handles by cancelling the scan server-side, so the cap did not
     * merely time out the wait, it cancelled a scan that was running perfectly
     * well. Bounding startup instead means the deadline can only ever fire while
     * there is no scan to cancel (NV-4827).
     *
     * Both streams are drained on background threads for the life of the
     * process, so a caller that returns early cannot leave the child blocked on
     * a full pipe.
     */
    fun runCommandUntilStarted(
        vararg command: String,
        marker: Regex,
        startupTimeoutMs: Long,
        workingDirectory: String = ""
    ): StartupOutcome {
        var cmd = GeneralCommandLine(*command)
            .withEnvironment("PATH", getPathForGeneralCommandLine())
        if (workingDirectory.isNotEmpty()) {
            cmd = cmd.withWorkDirectory(workingDirectory)
        }
        return awaitStartup(cmd, marker, startupTimeoutMs, command.toList())
    }

    /**
     * The process half of [runCommandUntilStarted], split out so it can be
     * exercised against real processes without a live platform supplying the
     * login-shell PATH.
     */
    fun awaitStartup(
        commandLine: GeneralCommandLine,
        marker: Regex,
        startupTimeoutMs: Long,
        command: List<String>
    ): StartupOutcome {
        try {
            val process = commandLine.createProcess()
            val buffer = StringBuilder()
            val sawMarker = CountDownLatch(1)
            // Cleared once the outcome has been reported. The streams still have
            // to be drained for the life of the process, but past that point the
            // buffer is nobody's: a scan runs for as long as it takes, and
            // appending its whole output would grow the IDE's heap for nothing.
            val capturing = AtomicBoolean(true)

            fun pump(stream: InputStream): Thread {
                val t = Thread {
                    try {
                        stream.bufferedReader().forEachLine { line ->
                            // Checked inside the lock, not before it: outside,
                            // a line could pass the check and then lose the
                            // race to stopCapturing, dropping the last
                            // diagnostic the process managed to print.
                            synchronized(buffer) {
                                if (capturing.get()) {
                                    buffer.append(line).append('\n')
                                }
                            }
                            if (marker.containsMatchIn(line)) {
                                sawMarker.countDown()
                            }
                        }
                    } catch (e: IOException) {
                        LOG.debug("Output stream closed for ${command.firstOrNull()}", e)
                    }
                }
                t.isDaemon = true
                t.start()
                return t
            }

            val pumps = listOf(pump(process.inputStream), pump(process.errorStream))

            val deadline = System.currentTimeMillis() + startupTimeoutMs
            var started = false
            while (true) {
                if (sawMarker.await(STARTUP_POLL_MS, TimeUnit.MILLISECONDS)) {
                    started = true
                    break
                }
                if (!process.isAlive) {
                    break
                }
                if (System.currentTimeMillis() >= deadline) {
                    break
                }
            }

            if (started) {
                // Deliberately left running: the scan is under way and the
                // process must see it through.
                return StartupOutcome(started = true, exited = false, timedOut = false,
                    output = stopCapturing(capturing, buffer))
            }

            // The marker can land between the loop's last check and here, and on
            // the timeout path the process is still healthy: destroying it first
            // would cancel the very scan the marker just announced, and report
            // that as a timeout. Testing before the destroy keeps the deadline
            // from firing on a scan that started.
            if (sawMarker.count == 0L) {
                return StartupOutcome(started = true, exited = false, timedOut = false,
                    output = stopCapturing(capturing, buffer))
            }

            val alive = process.isAlive
            if (alive) {
                stopProcess(process)
            }
            // Let the pumps finish so the reported output is not cut short.
            pumps.forEach { it.join(STARTUP_POLL_MS * 5) }
            // A marker arriving during the drain still counts, but only where the
            // process ended on its own: once it has been destroyed the scan is
            // gone whatever the marker said.
            if (sawMarker.count == 0L && !alive) {
                return StartupOutcome(started = true, exited = false, timedOut = false,
                    output = stopCapturing(capturing, buffer))
            }
            return StartupOutcome(
                started = false,
                exited = !alive,
                timedOut = alive,
                output = stopCapturing(capturing, buffer)
            )
        } catch (e: IOException) {
            throw getSpecificException(command, e)
        } catch (e: ProcessNotCreatedException) {
            throw getSpecificException(command, e)
        }
    }

    /**
     * How long a process gets to act on the SIGTERM before it is killed
     * outright.
     */
    private const val TERMINATE_GRACE_MS = 2_000L

    /**
     * Ends [process], escalating if it does not go. destroy() is a SIGTERM, and
     * a CLI wedged badly enough to miss its startup deadline is exactly the one
     * that may not act on it; without the escalation the deadline would report
     * a scan as stopped while the process ran on.
     */
    private fun stopProcess(process: Process) {
        process.destroy()
        if (!process.waitFor(TERMINATE_GRACE_MS, TimeUnit.MILLISECONDS)) {
            LOG.warn("CLI did not exit on terminate within ${TERMINATE_GRACE_MS}ms; killing it")
            process.destroyForcibly()
        }
    }

    /**
     * Final read of the output, after which the pumps stop appending. Called on
     * every exit from [awaitStartup], since nothing reads the buffer again.
     */
    private fun stopCapturing(capturing: AtomicBoolean, buffer: StringBuilder): String {
        // Clearing the flag and reading the buffer happen together, so a pump
        // holding the lock has already appended and no line is lost between
        // the two.
        return synchronized(buffer) {
            capturing.set(false)
            buffer.toString().trim()
        }
    }

    @Throws(ProcessNotCreatedException::class)
    fun getCLIVersion(): String {
        if (userCliVersion.isNotBlank()) {
            return userCliVersion; // Some caching to not call the command every time we go to Overview page...
        }
        val r = runCommandSync(NIGHTVISION, "version")
        val message = r.output.ifBlank { r.error }
        val regex = Regex("""Version\s+(\d+\.\d+\.\d+)""")
        val match = regex.find(message)
        val v = match?.groups?.get(1)?.value
            ?: ""
        if (v.isNotBlank()) {
            userCliVersion = v
        }
        return userCliVersion;
    }

    fun getSpecificException(command: List<String>, e: Exception): Exception {
        var k = getSpecificIOException(command, e)
        if (k == e) {
            k = getSpecificRuntimeException(command, e)
        }
        return k
    }

    fun getSpecificIOException(command: List<String>, io: Exception): Exception {
        val msg = io.message ?: ""
        when {
            "error=2" in msg || "No such file or directory" in msg ->
                return CommandNotFoundException(command)
            "error=13" in msg || "Permission denied" in msg ->
                return PermissionDeniedException(command)
            else ->
                return io
        }
    }

    fun getSpecificRuntimeException(command: List<String>, e: Exception): Exception {
        val msg = e.message ?: ""
        when {
            EXPIRED_TOKEN_PHRASE in msg ->
                return NotLoggedException(command)
            else -> return e
        }
    }

}
