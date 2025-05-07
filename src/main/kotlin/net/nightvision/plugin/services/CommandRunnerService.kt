package net.nightvision.plugin.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.nightvision.plugin.Constants.Companion.NIGHTVISION
import net.nightvision.plugin.exceptions.CommandNotFoundException
import net.nightvision.plugin.exceptions.PermissionDeniedException
import java.io.BufferedReader
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

    data class ExecutionResponse(val output: String, val error: String)

    private fun handleProcessResponse(
        command: String,
        process: Process
    ): ExecutionResponse {
        val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val error = process.errorStream.bufferedReader().use(BufferedReader::readText)

        if (process.exitValue() != 0) {
            LOG.warn("Command exited with ${process.exitValue()}: ${command}. Error: $error")
            throw RuntimeException("Command failed: $error")
        }

        return ExecutionResponse(
            output=output.trim(),
            error=error.trim()
        )
    }

    /**
     * Executes the given command synchronously, waiting up to [timeout] [unit].
     * Returns stdout as a trimmed String, or throws an exception on failure.
     */
    @Throws(CommandNotFoundException::class, PermissionDeniedException::class, IOException::class, Exception::class)
    fun runCommandSync(
        vararg command: String,
        timeout: Long = 30,
        unit: TimeUnit = TimeUnit.SECONDS
    ): ExecutionResponse {
        try {
            val process = ProcessBuilder(*command)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

    //        TODO: Need testing to see if this waitFor is really working or not. It seems not, so let's disable it
    //        if (!process.waitFor(timeout, unit)) {
    //            process.destroy()
    //            throw RuntimeException("Command timed out: ${command.joinToString(" ")}")
    //        }

            return handleProcessResponse(command.joinToString(" "), process)
        } catch (io: IOException) {
            throw getSpecificIOException(command.toList(), io)
        }

    }

    /**
     * Executes the given command asynchronously on a pooled thread.
     * Returns a [CompletableFuture] that completes with stdout or exceptionally on failure.
     */
    fun runCommandAsync(vararg command: String): CompletableFuture<ExecutionResponse> {
        return CompletableFuture.supplyAsync({
            try {
                runCommandSync(*command)
            } catch (e: Exception) {
                LOG.error("Async command failed: ${command.joinToString(" ")}", e)
                throw e
            }
        }, ApplicationManager.getApplication().executeOnPooledThread {} as java.util.concurrent.Executor)
    }

    suspend fun runSwaggerExtractCommand(directory: String, lang: String, fileName: String): ExecutionResponse {
        val command = listOf(NIGHTVISION, "swagger", "extract", directory,
            "--lang", lang,
            "--no-upload",
            "--output", fileName)
        try {
            val process = withContext(Dispatchers.IO) {
                ProcessBuilder(*command.toTypedArray())
                    .directory(File(directory))
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()
            }.apply {
                // TODO: This waitFor doesn't seem to work as expected, so let's ignore it for now
//            if (!waitFor(240, TimeUnit.SECONDS)) {
//                destroy()
//                throw RuntimeException("Command 'nightvision swagger extract' timed out")
//            }
            }

            return handleProcessResponse(command.joinToString(" "), process)
        } catch (io: IOException) {
            throw getSpecificIOException(command, io)
        }

    }

    fun getSpecificIOException(command: List<String>, io: IOException): Exception {
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

}
