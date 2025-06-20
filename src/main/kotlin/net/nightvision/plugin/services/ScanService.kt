package net.nightvision.plugin.services

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.nightvision.plugin.Constants
import net.nightvision.plugin.Constants.Companion.NIGHTVISION
import net.nightvision.plugin.ScanInfo
import net.nightvision.plugin.PaginatedResult
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit

object ScanService {
    val httpClient = HttpClient.newBuilder().build()
    val gson = GsonBuilder().create()

    fun getScans(): List<ScanInfo> {
        val token = LoginService.token
        val request = HttpRequest.newBuilder()
            .uri(Constants.getApiUrlFor("scans", mapOf("project" to ProjectService.getCurrentProjectId())))
            .header("Authorization", "Token $token")
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val type = object : TypeToken<PaginatedResult<ScanInfo>>() {}.type
        val responseData: PaginatedResult<ScanInfo> = gson.fromJson(response.body(), type)
        //println(responseData.results)
        return responseData.results ?: listOf() // TODO: Results are only for the FIRST page of pagination here - Improve
    }

    fun startScan(targetName: String, authenticationName: String?) {
        if (targetName.isBlank()) {
            throw IllegalArgumentException("Target name can't be empty.");
        }

        var cmd = ArrayList<String>(listOf(NIGHTVISION, "scan", targetName))
        if (authenticationName != null) {
            cmd.add("--auth")
            cmd.add(authenticationName)
        }

        val response = CommandRunnerService.runCommandSync(*cmd.toTypedArray())
        val t = response.output
        val id = t.trim().takeIf { Regex("INFO Scan Details").containsMatchIn(it) } ?: ""
        if (id.isBlank()) {
            // TODO: Improve error message details
            throw RuntimeException("Some error happened when creating your scan.")
        }
    }
}