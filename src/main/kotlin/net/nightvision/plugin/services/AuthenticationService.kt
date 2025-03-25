package net.nightvision.plugin.services

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.nightvision.plugin.Constants
import net.nightvision.plugin.Constants.Companion.NIGHTVISION
import net.nightvision.plugin.PaginatedResult
import net.nightvision.plugin.models.AuthInfo
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit

object AuthenticationService {
    val httpClient = HttpClient.newBuilder().build()
    val gson = GsonBuilder().create()

    fun getAuthInfos(): List<AuthInfo> {
        // TODO: Cache responses
        val token = LoginService.token
        val request = HttpRequest.newBuilder()
            .uri(Constants.getApiUrlFor("credentials", mapOf("project" to ProjectService.getCurrentProjectId())))
            .header("Authorization", "Token $token")
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val type = object : TypeToken<PaginatedResult<AuthInfo>>() {}.type
        val responseData: PaginatedResult<AuthInfo> = gson.fromJson(response.body(), type)
        //println(responseData.results)
        return responseData.results // TODO: Results are only for the FIRST page of pagination here - Improve
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
        val process = ProcessBuilder(cmd)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(200, TimeUnit.SECONDS)
        val t = process.inputStream.bufferedReader().readText()
        val id = t.trim().takeIf { Regex("Id:").containsMatchIn(it) } ?: ""
        if (id.isBlank()) {
            // TODO: Improve error message details
            throw RuntimeException("Some error happened when creating your authentication.")
        }
    }
}