package net.nightvision.plugin.intellij.services

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object ApiDiscoveryService {
    data class ApiDiscoveryResults(val path: Int, val classes: Int)

    fun extract(dirPath: String, lang: String): ApiDiscoveryResults {
        val fileName = "nv-swagger-extraction-results.yml";

        val process = ProcessBuilder("nightvision", "swagger", "extract", dirPath, "--lang", lang, "--no-upload", "--output",  fileName)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        var res = ""
        val outputReader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        while (outputReader.readLine().also { line = it } != null) {
            println("Output: $line")
            res += line  + "\n"
        }

        val errorReader = BufferedReader(InputStreamReader(process.errorStream))
        var errorLine: String?
        while (errorReader.readLine().also { errorLine = it } != null) {
            println("Error: $errorLine")
            res += errorLine + "\n"
        }

        process.waitFor(30, TimeUnit.SECONDS)
        val t = process.errorStream.bufferedReader().readText()
        return parseResults(res)
    }

    fun parseResults(message: String): ApiDiscoveryResults {
        val matchedPaths = Regex("Number of discovered paths:\\s*(.*)").find(message)
        val extractedPaths = matchedPaths?.groups?.get(1)?.value?.toInt() ?: 0

        val matchedClasses = Regex("Number of discovered classes:\\s*(.*)").find(message)
        val extractedClasses = matchedClasses?.groups?.get(1)?.value?.toInt() ?: 0

        return ApiDiscoveryResults(extractedPaths, extractedClasses)
    }

//    private fun createDocument(content: String): Document {
//        return ApplicationManager.getApplication().runReadAction<Document> {
//            val editorFactory = EditorFactory.getInstance()
//            editorFactory.createDocument(content)
//        }
//    }
}