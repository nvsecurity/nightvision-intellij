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

    private fun handleProcessResponse(
        command: String,
        output: ProcessOutput
    ): ExecutionResponse {
        if (!output.isTimeout && output.exitCode == 0) {
            println("Success: ${output.stdout.trim()}")
        } else {
            LOG.warn("Command exited with ${output.exitCode}: ${command}. Error: ${output.stderr.trim()}")
            if (output.isTimeout) {
                throw RuntimeException("The command timed out")
            } else {
                throw RuntimeException(
                    "The command failed (exit=${output.exitCode}):\n${output.stderr.trim()}"
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
            "token has expired. Please try to log in again" in msg ->
                return NotLoggedException(command)
            else -> return e
        }
    }

}
