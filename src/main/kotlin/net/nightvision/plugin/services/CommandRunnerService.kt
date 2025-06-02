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
import com.intellij.execution.process.ProcessOutput
import net.nightvision.plugin.exceptions.NotLoggedException
import java.io.File
import java.io.IOException
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
        val destDir = getDestinationDirForPlatform()
        val originalPath = System.getenv("PATH") ?: ""
        val newPath = if (originalPath.startsWith(destDir)) {
            originalPath
        } else {
            destDir + File.pathSeparator + originalPath
        }
        return destDir
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

    fun getCLIVersion(): String {
        val r = runCommandSync(NIGHTVISION, "version")
        val message = r.output.ifBlank { r.error }
        val regex = Regex("""(\d+\.\d+\.\d+)""")
        val match = regex.find(message)
        return match?.groups?.get(1)?.value
            ?: ""
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
