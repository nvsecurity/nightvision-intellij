package net.nightvision.plugin.intellij.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

object ApiDiscoveryService {
    data class ApiDiscoveryResults(val path: Int, val classes: Int)

    var project: Project? = null
    const val fileName: String = "nv-swagger-extraction-results.yml"

    fun extract(dirPath: String, lang: String, project: Project): ApiDiscoveryResults {
        return runBlocking {
            extractBlocking(dirPath, lang, project)
        }
    }

    suspend fun extractBlocking(dirPath: String, lang: String, project: Project): ApiDiscoveryResults {
        this.project = project
        val process = ProcessBuilder(
                "nightvision",
                "swagger",
                "extract",
                dirPath,
                "--lang",
                lang,
                "--no-upload",
                "--output",
                fileName
            )
            .directory(File(dirPath))
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        process.waitFor(30, TimeUnit.SECONDS)

        val filePath = Paths.get(dirPath, fileName).toString()
        val data = readFile(filePath)
        val document = createDocument(data)
        openDocument(document)
        deleteFile(filePath)
        return parseResults(process.errorStream.bufferedReader().readText())
    }

    fun parseResults(message: String): ApiDiscoveryResults {
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
        return ApplicationManager.getApplication().runReadAction<Document> {
            val editorFactory = EditorFactory.getInstance()
            editorFactory.createDocument(content)
        }
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
        val project = this.project ?: return
        val fileEditorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)

        ApplicationManager.getApplication().invokeLater {
            fileEditorManager.openTextEditor(
                com.intellij.openapi.fileEditor.OpenFileDescriptor(project, createVirtualFile(document)),
                true
            )
        }
    }

    private fun createVirtualFile(document: Document): VirtualFile {
        return ApplicationManager.getApplication().runWriteAction<VirtualFile> {
            val file = File.createTempFile(fileName, "yml")
            file.writeText(document.text)
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file) ?: throw IllegalStateException("Cannot find virtual file")
        }
    }
}