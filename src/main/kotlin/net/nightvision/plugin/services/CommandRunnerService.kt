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
import java.util.concurrent.CompletableFuture
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
            if (output.isTimeout) {
                throw RuntimeException("The command timed out")
            } else if (detail.isEmpty()) {
                throw RuntimeException("The command failed (exit=${output.exitCode}) without reporting a reason.")
            } else {
                throw RuntimeException(
                    "The command failed (exit=${output.exitCode}):\n${detail}"
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
