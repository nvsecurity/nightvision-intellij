package net.nightvision.plugin.intellij.services

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.nightvision.plugin.intellij.Constants
import net.nightvision.plugin.intellij.Constants.Companion.NIGHTVISION
import net.nightvision.plugin.intellij.PaginatedResult
import net.nightvision.plugin.intellij.models.ProjectInfo
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit

object ProjectService {
    val httpClient = HttpClient.newBuilder().build()
    val gson = GsonBuilder().create()

    fun getProjectInfos(): List<ProjectInfo> {
        // TODO: Cache responses
        val token = LoginService.token
        val request = HttpRequest.newBuilder()
            .uri(Constants.getUrlFor("projects"))
            .header("Authorization", "Token $token")
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val type = object : TypeToken<PaginatedResult<ProjectInfo>>() {}.type
        val responseData: PaginatedResult<ProjectInfo> = gson.fromJson(response.body(), type)
        return responseData.results
    }

    fun createProject(projectName: String) {
        if (projectName.isBlank()) {
            throw IllegalArgumentException("Project name can't be empty.");
        }

        var cmd = ArrayList<String>(listOf(NIGHTVISION, "project", "create", projectName))
        val process = ProcessBuilder(cmd)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(30, TimeUnit.SECONDS)
        val t = process.inputStream.bufferedReader().readText()
        val id = t.trim().takeIf { Regex("Id:").containsMatchIn(it) } ?: ""
        if (id.isBlank()) {
            // TODO: Improve error message details
            throw RuntimeException("Some error happened when creating your project.")
        }
    }
}