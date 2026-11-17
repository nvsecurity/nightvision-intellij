package net.nightvision.plugin.services

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.nightvision.plugin.PaginatedResult
import java.net.URI
import java.net.URLDecoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

internal object PaginatedApiService {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()
    private val gson = Gson()

    // NV-7001: return a complete snapshot or fail; never expose a partial list.
    fun <T> getAll(firstPage: URI, token: String, itemClass: Class<T>, httpClient: HttpClient = client, parser: Gson = gson, id: (T) -> String): List<T> {
        val type = TypeToken.getParameterized(PaginatedResult::class.java, itemClass).type
        val results = linkedMapOf<String, T>()
        val visited = mutableSetOf<URI>()
        var page: URI? = firstPage
        while (page != null) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException("Loading cancelled")
            check(visited.add(page)) { "The API repeated a resource page. Please retry." }
            val request = HttpRequest.newBuilder(page)
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Token $token")
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            check(response.statusCode() in 200..299) {
                "Could not load resources: HTTP ${response.statusCode()}. Please retry."
            }
            val data: PaginatedResult<T> = parser.fromJson<PaginatedResult<T>?>(response.body(), type)
                ?: error("The API returned an invalid resource page. Please retry.")
            val items = data.results ?: error("The API returned an invalid resource page. Please retry.")
            for (item in items) {
                val key = id(item)
                check(!key.isNullOrBlank()) { "The API returned a resource without an ID." }
                results.putIfAbsent(key, item)
            }
            page = data.next?.takeIf { it.isNotBlank() }?.let { nextPage(firstPage, page, it) }
        }
        return results.values.toList()
    }

    private fun nextPage(first: URI, current: URI, next: String): URI {
        // URI.resolve("?page=2") drops the last path segment on the JDK.
        val resolved = if (next.startsWith("?")) {
            URI.create(current.toString().substringBefore('?') + next)
        } else current.resolve(next)
        check(resolved.scheme == first.scheme && resolved.authority == first.authority &&
            resolved.path == first.path && resolved.fragment == null && resolved.userInfo == null) {
            "The API returned an unexpected pagination URL. Please retry."
        }
        val originalParams = queryParams(first).filterKeys { it != "page" }
        val nextParams = queryParams(resolved)
        check(originalParams.all { (key, value) -> nextParams[key] == value }) {
            "The API changed the resource filter while loading pages. Please retry."
        }
        return resolved
    }

    private fun queryParams(uri: URI): Map<String, List<String>> = uri.rawQuery.orEmpty()
        .split('&').filter { it.isNotEmpty() }.map {
            val parts = it.split('=', limit = 2)
            URLDecoder.decode(parts[0], StandardCharsets.UTF_8) to
                URLDecoder.decode(parts.getOrElse(1) { "" }, StandardCharsets.UTF_8)
        }.groupBy({ it.first }, { it.second })
}
