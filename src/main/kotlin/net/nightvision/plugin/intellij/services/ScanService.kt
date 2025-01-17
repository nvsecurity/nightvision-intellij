package net.nightvision.plugin.intellij.services

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.nightvision.plugin.intellij.Scan
import net.nightvision.plugin.intellij.PaginatedResult
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object ScanService {
    private val httpClient = HttpClient.newBuilder().build()
    private val gson = GsonBuilder().create()

    fun getScans(): List<Scan> {
        val token = LoginService.token
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.nightvision.net/api/v1/scans/"))
            .header("Authorization", "Token $token")
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val type = object : TypeToken<PaginatedResult<Scan>>() {}.type
        val responseData: PaginatedResult<Scan> = gson.fromJson(response.body(), type)
        println(responseData.results)
        return responseData.results
    }
}