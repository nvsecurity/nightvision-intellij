package net.nightvision.plugin.services

import com.sun.net.httpserver.HttpServer
import net.nightvision.plugin.models.AuthInfo
import net.nightvision.plugin.models.ProjectInfo
import net.nightvision.plugin.models.TargetInfo
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.net.InetSocketAddress
import java.net.URI

class PaginatedApiServiceTest {
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply { start() }
    private val base = "http://127.0.0.1:${server.address.port}"

    @After
    fun stopServer() {
        server.stop(0)
    }

    private fun respond(path: String, handler: (URI) -> Pair<Int, String>) {
        server.createContext(path) { exchange ->
            assertEquals("Token fixture-token", exchange.requestHeaders.getFirst("Authorization"))
            val (status, body) = handler(exchange.requestURI)
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }

    @Test
    fun `all three resource types include later pages in API order and deduplicate by ID`() {
        for (path in listOf("projects", "targets/url", "credentials")) {
            respond("/api/v1/$path/") { uri ->
                assertTrue(uri.rawQuery.contains("project=selected"))
                200 to if (uri.rawQuery.contains("page=2")) {
                    """{"results":[{"id":"one","name":"repeated"},{"id":"two","name":"Later page"}],"next":null,"count":2}"""
                } else {
                    """{"results":[{"id":"one","name":"First page"}],"next":"?project=selected&page=2","count":2}"""
                }
            }
        }
        val projects = PaginatedApiService.getAll(URI("$base/api/v1/projects/?project=selected"),
            "fixture-token", ProjectInfo::class.java) { it.id }
        val targets = PaginatedApiService.getAll(URI("$base/api/v1/targets/url/?project=selected"),
            "fixture-token", TargetInfo::class.java) { it.id }
        val auths = PaginatedApiService.getAll(URI("$base/api/v1/credentials/?project=selected"),
            "fixture-token", AuthInfo::class.java) { it.id }
        assertEquals(listOf("one", "two"), projects.map { it.id })
        assertEquals(listOf("First page", "Later page"), targets.map { it.name })
        assertEquals(listOf("one", "two"), auths.map { it.id })
    }

    @Test
    fun `absolute and path-relative next links are followed`() {
        respond("/api/v1/projects/") { uri ->
            200 to when (uri.rawQuery) {
                null -> """{"results":[{"id":"one"}],"next":"$base/api/v1/projects/?page=2"}"""
                "page=2" -> """{"results":[{"id":"two"}],"next":"/api/v1/projects/?page=3"}"""
                else -> """{"results":[{"id":"three"}],"next":null}"""
            }
        }
        assertEquals(listOf("one", "two", "three"), load().map { it.id })
    }

    @Test
    fun `later-page failure throws rather than returning the first page`() {
        respond("/api/v1/projects/") { uri ->
            if (uri.rawQuery == null) 200 to """{"results":[{"id":"one"}],"next":"?page=2"}"""
            else 503 to "Service unavailable"
        }
        val error = assertThrows(IllegalStateException::class.java) { load() }
        assertTrue(error.message!!.contains("HTTP 503"))
    }

    @Test
    fun `empty results are valid but missing results are an error`() {
        respond("/api/v1/projects/") { 200 to """{"results":[],"next":null}""" }
        assertTrue(load().isEmpty())
        server.removeContext("/api/v1/projects/")
        respond("/api/v1/projects/") { 200 to """{"detail":"unexpected response"}""" }
        assertThrows(IllegalStateException::class.java) { load() }
    }

    @Test
    fun `pagination cannot send the token to another host or change the resource endpoint`() {
        for (next in listOf("http://example.invalid/api/v1/projects/?page=2", "/api/v1/credentials/?page=2")) {
            respond("/api/v1/projects/") { 200 to """{"results":[],"next":"$next"}""" }
            assertThrows(IllegalStateException::class.java) { load() }
            server.removeContext("/api/v1/projects/")
        }
    }

    @Test
    fun `pagination cannot drop the selected project filter`() {
        respond("/api/v1/projects/") { 200 to """{"results":[],"next":"?page=2"}""" }
        assertThrows(IllegalStateException::class.java) {
            PaginatedApiService.getAll(URI("$base/api/v1/projects/?project=selected"),
                "fixture-token", ProjectInfo::class.java) { it.id }
        }
    }

    @Test
    fun `repeated next page fails instead of loading forever`() {
        respond("/api/v1/projects/") { 200 to """{"results":[],"next":"?page=2"}""" }
        assertThrows(IllegalStateException::class.java) { load() }
    }

    @Test
    fun `interruption cancels before requesting another page`() {
        Thread.currentThread().interrupt()
        try {
            assertThrows(InterruptedException::class.java) { load() }
        } finally {
            Thread.interrupted()
        }
    }

    private fun load() = PaginatedApiService.getAll(URI("$base/api/v1/projects/"),
        "fixture-token", ProjectInfo::class.java) { it.id }
}
