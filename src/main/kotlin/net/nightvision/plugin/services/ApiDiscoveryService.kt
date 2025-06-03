package net.nightvision.plugin.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.nightvision.plugin.Constants.Companion.NIGHTVISION
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

object ApiDiscoveryService {
    data class ApiDiscoveryResults(val path: Int, val classes: Int)

    private var project: Project? = null
    private const val fileName: String = "nv-swagger-extraction-results.yml"

    fun extract(dirPath: String, lang: String, project: Project): ApiDiscoveryResults {
        return runBlocking {
            extractBlocking(dirPath, lang, project)
        }
    }

    private fun makeFilePathAbsolute(filePath: String, project: Project): String {
        val virtualFile: VirtualFile? = ApplicationManager.getApplication().runReadAction<VirtualFile?> {
            project.basePath?.let { VfsUtil.findFile(Paths.get(it), true) }
        }

        return if (Paths.get(filePath).isAbsolute) {
            filePath
        } else {
            virtualFile?.path?.let { Paths.get(it, filePath).toString() } ?: Paths.get(filePath).toAbsolutePath().toString()
        }
    }

    private suspend fun extractBlocking(dirPath: String, lang: String, project: Project): ApiDiscoveryResults {
        ApiDiscoveryService.project = project

        val directory = makeFilePathAbsolute(dirPath, project)

        val response = CommandRunnerService.runCommandSync(
            NIGHTVISION, "swagger", "extract", directory, "--lang", lang, "--no-upload", "--output", fileName,
            workingDirectory = directory
        )

        val errorOutput = response.error
        val normalOutput = response.output
        val logMessage = errorOutput.ifEmpty { normalOutput }
        println("Process output: $normalOutput")
        println("Process errors: $errorOutput")

        val results: ApiDiscoveryResults = parseResults(logMessage)
        val filePath = Paths.get(directory, fileName).toString()

        val data = readFile(filePath)
        val document = ApplicationManager.getApplication().runReadAction<Document> {
            createDocument(data)
        }

        openDocument(document)
        deleteFile(filePath)
        return results
    }

    private fun parseResults(message: String): ApiDiscoveryResults {
        val hasErrors = Regex(".*ERROR error.*").containsMatchIn(message);
        if (hasErrors) {
            throw Exception()
        }

        val matchedPaths = Regex("Number of discovered paths:\\s*(.*)").find(message)
        val extractedPaths = matchedPaths?.groups?.get(1)?.value?.toInt() ?: 0

        val matchedClasses = Regex("Number of discovered classes:\\s*(.*)").find(message)
        val extractedClasses = matchedClasses?.groups?.get(1)?.value?.toInt() ?: 0

        return ApiDiscoveryResults(extractedPaths, extractedClasses)
    }

    private suspend fun readFile(filePath: String): String {
        return withContext(Dispatchers.IO) {
            val path = Paths.get(filePath)
            Files.readString(path)
        }
    }

    private fun createDocument(content: String): Document {
        val editorFactory = EditorFactory.getInstance()
        return editorFactory.createDocument(content)
    }

    private suspend fun deleteFile(filePath: String) {
        withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun openDocument(document: Document) {
        val project = project ?: return
        val fileEditorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)

        ApplicationManager.getApplication().invokeLater {
            val virtualFile = ApplicationManager.getApplication().runWriteAction<VirtualFile> {
                createVirtualFile(document)
            }

            fileEditorManager.openTextEditor(
                com.intellij.openapi.fileEditor.OpenFileDescriptor(project, virtualFile),
                true
            )
        }
    }

    private fun createVirtualFile(document: Document): VirtualFile {
        return ApplicationManager.getApplication().runWriteAction<VirtualFile> {
            val file = File.createTempFile(fileName.removeSuffix(".yml") + "-", ".yml")
            file.writeText(document.text)
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file) ?: throw IllegalStateException("Cannot find virtual file")
        }
    }
}