package net.nightvision.plugin.intellij.login

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.nightvision.plugin.intellij.Scan
import net.nightvision.plugin.intellij.PaginatedResult
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit


object LoginService {
    private var token = ""
    private val httpClient = HttpClient.newBuilder().build()
    private val gson = GsonBuilder()
//        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    fun createToken(): String {
        val process = ProcessBuilder("nightvision", "token", "create")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(30, TimeUnit.SECONDS)
        val t = process.inputStream.bufferedReader().readText()
        val token = t.trim().takeIf { it.matches(Regex("^\\S{64}$")) } ?: ""
        println(token)
        return token
    }

    fun login(): Boolean {
        if (token != "") {
            return true;
        }
        val process = ProcessBuilder("nightvision", "login")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(30, TimeUnit.SECONDS)
        println(process.inputStream.bufferedReader().readText())

        token = createToken()
        return token != "";
    }

    fun getScans(): List<Scan> {
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