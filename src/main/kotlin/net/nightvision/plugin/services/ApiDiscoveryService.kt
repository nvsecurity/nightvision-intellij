package net.nightvision.plugin.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import net.nightvision.plugin.Constants.Companion.NIGHTVISION
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

object ApiDiscoveryService {
    data class ApiDiscoveryResults(val path: Int, val classes: Int)

    fun extract(dirPath: String, lang: String, project: Project): ApiDiscoveryResults {
        val directory = makeFilePathAbsolute(dirPath, project)
        val dirName = File(directory).name.ifEmpty { "project" }
        val displayName = "${dirName}-openapi.yml"
        val cliOutputFileName = "nv-swagger-extraction-${UUID.randomUUID()}.yml"

        val command = mutableListOf(NIGHTVISION, "swagger", "extract", directory)
        if (lang.isNotEmpty() && lang != "all") {
            command.add("--lang")
            command.add(lang)
        }
        command.addAll(listOf("--no-upload", "--output", cliOutputFileName))

        val response = CommandRunnerService.runCommandSync(
            *command.toTypedArray(),
            workingDirectory = directory
        )

        val errorOutput = response.error
        val normalOutput = response.output
        val logMessage = errorOutput.ifEmpty { normalOutput }

        val cliOutputPath = Paths.get(directory, cliOutputFileName).toString()
        val cliOutputFile = File(cliOutputPath)
        try {
            val results: ApiDiscoveryResults = parseResults(logMessage)
            if (cliOutputFile.exists()) {
                val content = Files.readString(Paths.get(cliOutputPath))
                openInEditor(project, displayName, content)
            }
            return results
        } finally {
            if (cliOutputFile.exists()) {
                cliOutputFile.delete()
            }
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

    private fun openInEditor(project: Project, displayName: String, content: String) {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val yamlFileType = FileTypeManager.getInstance().getFileTypeByExtension("yml")
        val virtualFile = LightVirtualFile(displayName, yamlFileType, content)

        ApplicationManager.getApplication().invokeLater {
            fileEditorManager.openTextEditor(
                OpenFileDescriptor(project, virtualFile),
                true
            )
        }
    }
}
