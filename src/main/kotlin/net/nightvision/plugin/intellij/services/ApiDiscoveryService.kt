package net.nightvision.plugin.intellij.services

import java.util.concurrent.TimeUnit

object ApiDiscoveryService {
    data class ApiDiscoveryResults(val path: Int, val classes: Int)

    fun extract(dirPath: String, lang: String): ApiDiscoveryResults {
        val fileName = "nv-swagger-extraction-results.yml"

        val process = ProcessBuilder("nightvision", "swagger", "extract", dirPath, "--lang", lang, "--no-upload", "--output",  fileName)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(30, TimeUnit.SECONDS)
        return parseResults(process.errorStream.bufferedReader().readText())
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