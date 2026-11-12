package net.nightvision.plugin.services

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApiUrlServiceTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `env var takes precedence over config file`() {
        val configFile = tempDir.newFile("nightvision.yml")
        configFile.writeText("api-url: https://api.config.example.com/api/v1/")

        val result = ApiUrlService.resolveApiUrl(
            envUrl = "https://api.env.example.com",
            configPath = configFile.absolutePath
        )
        assertEquals("https://api.env.example.com", result)
    }

    @Test
    fun `config file used when env var is null`() {
        val configFile = tempDir.newFile("nightvision.yml")
        configFile.writeText("api-url: https://api.test.nightviz.ai/api/v1/")

        val result = ApiUrlService.resolveApiUrl(
            envUrl = null,
            configPath = configFile.absolutePath
        )
        assertEquals("https://api.test.nightviz.ai", result)
    }

    @Test
    fun `config file used when env var is blank`() {
        val configFile = tempDir.newFile("nightvision.yml")
        configFile.writeText("api-url: https://api.test.nightvision.net/api/v1/")

        val result = ApiUrlService.resolveApiUrl(
            envUrl = "  ",
            configPath = configFile.absolutePath
        )
        assertEquals("https://api.test.nightvision.net", result)
    }

    @Test
    fun `falls back to default when no env var or config`() {
        val result = ApiUrlService.resolveApiUrl(
            envUrl = null,
            configPath = "/nonexistent/path/nightvision.yml"
        )
        assertEquals("https://api.nightviz.ai", result)
    }

    @Test
    fun `strips api v1 suffix from env var`() {
        val result = ApiUrlService.resolveApiUrl(
            envUrl = "https://api.example.com/api/v1/",
            configPath = "/nonexistent"
        )
        assertEquals("https://api.example.com", result)
    }

    @Test
    fun `strips api v1 suffix without trailing slash`() {
        val result = ApiUrlService.resolveApiUrl(
            envUrl = "https://api.example.com/api/v1",
            configPath = "/nonexistent"
        )
        assertEquals("https://api.example.com", result)
    }

    @Test
    fun `strips trailing slash from base URL`() {
        val result = ApiUrlService.resolveApiUrl(
            envUrl = "https://api.example.com/",
            configPath = "/nonexistent"
        )
        assertEquals("https://api.example.com", result)
    }

    @Test
    fun `url without suffix is returned as-is`() {
        val result = ApiUrlService.resolveApiUrl(
            envUrl = "https://api.example.com",
            configPath = "/nonexistent"
        )
        assertEquals("https://api.example.com", result)
    }

    @Test
    fun `config file with multiple fields parses api-url`() {
        val configFile = tempDir.newFile("nightvision.yml")
        configFile.writeText("""
            some-other-key: some-value
            api-url: https://api.test.nightvision.net/api/v1/
            another-key: another-value
        """.trimIndent())

        val result = ApiUrlService.resolveApiUrl(
            envUrl = null,
            configPath = configFile.absolutePath
        )
        assertEquals("https://api.test.nightvision.net", result)
    }

    @Test
    fun `config file naming the former nightvision net host is kept`() {
        val configFile = tempDir.newFile("nightvision.yml")
        configFile.writeText("api-url: https://api.nightvision.net/api/v1/")

        val result = ApiUrlService.resolveApiUrl(
            envUrl = null,
            configPath = configFile.absolutePath
        )
        assertEquals("https://api.nightvision.net", result)
    }

    @Test
    fun `default api url opens app links on nightviz ai`() {
        val apiUrl = ApiUrlService.resolveApiUrl(
            envUrl = null,
            configPath = "/nonexistent/path/nightvision.yml"
        )
        assertEquals("https://app.nightviz.ai", ApiUrlService.resolveAppUrl(apiUrl))
    }

    @Test
    fun `config file without api-url falls back to default`() {
        val configFile = tempDir.newFile("nightvision.yml")
        configFile.writeText("some-other-key: some-value")

        val result = ApiUrlService.resolveApiUrl(
            envUrl = null,
            configPath = configFile.absolutePath
        )
        assertEquals("https://api.nightviz.ai", result)
    }

    @Test
    fun `empty config file falls back to default`() {
        val configFile = tempDir.newFile("nightvision.yml")
        configFile.writeText("")

        val result = ApiUrlService.resolveApiUrl(
            envUrl = null,
            configPath = configFile.absolutePath
        )
        assertEquals("https://api.nightviz.ai", result)
    }

    @Test
    fun `app url is on nightviz ai for a nightviz ai api host`() {
        assertEquals("https://app.nightviz.ai", ApiUrlService.resolveAppUrl("https://api.nightviz.ai"))
        assertEquals("https://app.nightviz.ai", ApiUrlService.resolveAppUrl("https://api.nightviz.ai/api/v1/"))
    }

    @Test
    fun `app url stays on nightvision net for a nightvision net api host`() {
        assertEquals("https://app.nightvision.net", ApiUrlService.resolveAppUrl("https://api.nightvision.net"))
        assertEquals("https://app.nightvision.net", ApiUrlService.resolveAppUrl("https://API.NightVision.net"))
    }

    @Test
    fun `app url follows the environment of the api host`() {
        assertEquals("https://test.nightviz.ai", ApiUrlService.resolveAppUrl("https://api.test.nightviz.ai"))
        assertEquals("https://dev.nightviz.ai", ApiUrlService.resolveAppUrl("https://api.dev.nightviz.ai"))
        assertEquals("https://test.nightvision.net", ApiUrlService.resolveAppUrl("https://api.test.nightvision.net"))
    }

    @Test
    fun `app url defaults to nightviz ai for other hosts`() {
        assertEquals("https://app.nightviz.ai", ApiUrlService.resolveAppUrl("https://api.example.com"))
        assertEquals("https://app.nightviz.ai", ApiUrlService.resolveAppUrl("http://localhost:8000"))
        assertEquals("https://app.nightviz.ai", ApiUrlService.resolveAppUrl("https://notnightvision.net"))
        assertEquals("https://app.nightviz.ai", ApiUrlService.resolveAppUrl("not a url"))
        assertEquals("https://app.nightviz.ai", ApiUrlService.resolveAppUrl("https://api.a.b.nightviz.ai"))
        assertEquals("https://app.nightviz.ai", ApiUrlService.resolveAppUrl("https://www.nightvision.net"))
    }
}
