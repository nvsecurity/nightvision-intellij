package net.nightvision.plugin.services

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.nightvision.plugin.Constants
import net.nightvision.plugin.Constants.Companion.NIGHTVISION
import net.nightvision.plugin.PaginatedResult
import net.nightvision.plugin.models.ProjectInfo
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit

object ProjectService {
    val httpClient = HttpClient.newBuilder().build()
    val gson = GsonBuilder().create()
    private var currentProjectName = ""
    private var currentProjectId = ""

    fun getProjectInfos(): List<ProjectInfo> {
        // TODO: Cache responses
        val token = LoginService.token
        val request = HttpRequest.newBuilder()
            .uri(Constants.getApiUrlFor("projects"))
            .header("Authorization", "Token $token")
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val type = object : TypeToken<PaginatedResult<ProjectInfo>>() {}.type
        if (response.body() == null) {
            return listOf()
        }
        val responseData: PaginatedResult<ProjectInfo> = gson.fromJson(response.body(), type)
        return responseData.results ?: listOf() // TODO: Results are only for the FIRST page of pagination here - Improve
    }

    fun createProject(projectName: String) {
        if (projectName.isBlank()) {
            throw IllegalArgumentException("Project name can't be empty.");
        }

        var cmd = ArrayList<String>(listOf(NIGHTVISION, "project", "create", projectName))
        val response = CommandRunnerService.runCommandSync(*cmd.toTypedArray())
        val t = response.output
        val id = t.trim().takeIf { Regex("Id:").containsMatchIn(it) } ?: ""
        if (id.isBlank()) {
            // TODO: Improve error message details
            throw RuntimeException("Some error happened when creating your project.")
        }
    }

    fun getCurrentProjectName(): String {
        return currentProjectName
    }

    fun getCurrentProjectId(): String {
        return currentProjectId
    }

    fun fetchCurrentProjectName() : String {
        val cmd = ArrayList<String>(listOf(NIGHTVISION, "project", "show"))

        val response = CommandRunnerService.runCommandSync(*cmd.toTypedArray())
        val t = response.output
        val regexProjName = Regex("""(?m)^Name:\s*(.+)$""")
        val matchProjName = regexProjName.find(t)
        currentProjectName = matchProjName?.groupValues?.get(1) ?: ""

        val regexProjId = Regex("""(?m)^Id:\s*(.+)$""")
        val matchProjId = regexProjId.find(t)
        currentProjectId = matchProjId?.groupValues?.get(1) ?: ""

        return currentProjectName
    }

    fun setCurrentProjectName(projectName: String) {
        if (projectName.isBlank()) {
            return
        }
        var cmd = ArrayList<String>(listOf(NIGHTVISION, "project", "set", projectName))

        val response = CommandRunnerService.runCommandSync(*cmd.toTypedArray())
        val t = response.output
        val success = Regex("Current project changed").containsMatchIn(t.trim())
        if (success) {
            fetchCurrentProjectName()
            return
        }
        throw RuntimeException("Unable to set project to '${projectName}'")
    }

}