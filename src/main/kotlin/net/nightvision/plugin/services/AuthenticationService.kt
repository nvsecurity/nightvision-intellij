package net.nightvision.plugin.services

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.nightvision.plugin.Constants
import net.nightvision.plugin.Constants.Companion.NIGHTVISION
import net.nightvision.plugin.PaginatedResult
import net.nightvision.plugin.models.AuthInfo
import java.net.http.HttpClient
import java.time.Duration
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit

object AuthenticationService {
    val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()
    val gson = GsonBuilder().create()

    @JvmOverloads
    fun getAuthInfos(projectId: String = ProjectService.getCurrentProjectId()): List<AuthInfo> {
        if (projectId.isBlank()) return emptyList()
        return PaginatedApiService.getAll(Constants.getApiUrlFor("credentials", mapOf("project" to projectId)),
            LoginService.token, AuthInfo::class.java, httpClient, gson) { it.id }
    }

    fun createPlaywrightAuth(authName: String, authURL: String, description: String?) {
        if (authName.isBlank()) {
            throw IllegalArgumentException("Authentication name can't be empty.");
        }
        if (authURL.isBlank()) {
            throw IllegalArgumentException("Authentication URL can't be empty.");
        }

        var cmd = ArrayList<String>(listOf(NIGHTVISION, "auth", "playwright", "create", authName, authURL))
        if (!description.isNullOrBlank()) {
            cmd.add("--description")
            cmd.add(description)
        }
        val response = CommandRunnerService.runCommandSync(*cmd.toTypedArray(), timeout=200L)
        val t = response.output
        val id = t.trim().takeIf { Regex("Id:").containsMatchIn(it) } ?: ""
        if (id.isBlank()) {
            throw RuntimeException(
                CommandRunnerService.missingRecordMessage("create the authentication", response.output, response.error)
            )
        }
    }
}