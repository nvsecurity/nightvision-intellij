package net.nightvision.plugin.intellij.services

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.nightvision.plugin.intellij.Constants
import net.nightvision.plugin.intellij.Constants.Companion.NIGHTVISION
import net.nightvision.plugin.intellij.PaginatedResult
import net.nightvision.plugin.intellij.models.TargetInfo
import net.nightvision.plugin.intellij.models.TargetURL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit

object TargetService {
    val httpClient = HttpClient.newBuilder().build()
    val gson = GsonBuilder().create()

    fun getTargetInfos(targetType: String = ""): List<TargetInfo> {
        // TODO: Cache responses
        val token = LoginService.token
        var suffix = "targets"
        if (targetType.isNotBlank()) {
            suffix += "/" + targetType.lowercase()
        }
        val request = HttpRequest.newBuilder()
            .uri(Constants.getApiUrlFor(suffix, mapOf("project" to ProjectService.getCurrentProjectId())))
            .header("Authorization", "Token $token")
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val type = object : TypeToken<PaginatedResult<TargetInfo>>() {}.type
        val responseData: PaginatedResult<TargetInfo> = gson.fromJson(response.body(), type)
        return responseData.results // TODO: Results are only for the FIRST page of pagination here - Improve
    }

    fun getTargetSpecificInfo(targetType: String, targetId: String): TargetInfo? {
        if (targetType.isBlank() || targetId.isBlank()) {
            return null;
        }
        val token = LoginService.token
        val suffix = "targets/${targetType.lowercase()}/${targetId}"

        val request = HttpRequest.newBuilder()
            .uri(Constants.getApiUrlFor(suffix))
            .header("Authorization", "Token $token")
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val type = object : TypeToken<TargetInfo>() {}.type
        val responseData: TargetInfo = gson.fromJson(response.body(), type)
        return responseData
    }

    fun getSpecURL(targetId: String) : String {
        if (targetId.isBlank()) {
            return "";
        }
        val token = LoginService.token
        val suffix = "targets/openapi/${targetId}/get-spec-url"

        val request = HttpRequest.newBuilder()
            .uri(Constants.getApiUrlFor(suffix))
            .header("Authorization", "Token $token")
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val type = object : TypeToken<TargetURL>() {}.type
        val responseData: TargetURL = gson.fromJson(response.body(), type)
        return responseData.url
    }

    fun createWebTarget(targetName: String, targetURL: String) {
        commonCreateTarget(targetName, targetURL, listOf("-t", "WEB"))
    }

    fun createApiTarget(targetName: String, targetURL: String, swaggerPath: String, isSwaggerURL: Boolean) {
        if (swaggerPath.isBlank()) {
            throw IllegalArgumentException("URL or filepath for a valid specification must be provided.");
        }
        commonCreateTarget(targetName, targetURL, listOf("-t", "API", if(isSwaggerURL) "-s" else "-f", swaggerPath))
    }

    fun commonCreateTarget(targetName: String, targetURL: String, extraFlags: List<String>) {
        if (targetName.isBlank()) {
            throw IllegalArgumentException("Target name can't be empty.");
        }
        if (targetURL.isBlank()) {
            throw IllegalArgumentException("Target URL can't be empty.");
        }

        var cmd = ArrayList<String>(listOf(NIGHTVISION, "target", "create", targetName, targetURL))
        cmd.addAll(extraFlags);
        val process = ProcessBuilder(cmd)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(30, TimeUnit.SECONDS)
        val t = process.inputStream.bufferedReader().readText()
        val id = t.trim().takeIf { Regex("Id:").containsMatchIn(it) } ?: ""
        if (id.isBlank()) {
            // TODO: Improve error message details
            throw RuntimeException("Some error happened when creating your target.")
        }
    }
}