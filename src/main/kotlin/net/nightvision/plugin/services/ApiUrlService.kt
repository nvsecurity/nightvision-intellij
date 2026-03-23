package net.nightvision.plugin.services

import java.io.File
import java.nio.file.Paths

/**
 * Resolves the NightVision API URL using the same precedence as the CLI:
 * 1. NIGHTVISION_API_URL environment variable
 * 2. api-url from ~/.nightvision/nightvision.yml
 * 3. https://api.nightvision.net (default)
 */
object ApiUrlService {
    private const val DEFAULT_API_URL = "https://api.nightvision.net"

    private val defaultConfigPath: String = Paths.get(
        System.getProperty("user.home"), ".nightvision", "nightvision.yml"
    ).toString()

    fun resolveApiUrl(): String {
        return resolveApiUrl(
            envUrl = System.getenv("NIGHTVISION_API_URL"),
            configPath = defaultConfigPath
        )
    }

    internal fun resolveApiUrl(envUrl: String?, configPath: String): String {
        // 1. Check environment variable
        if (!envUrl.isNullOrBlank()) {
            return stripApiSuffix(envUrl)
        }

        // 2. Check CLI config file
        val configUrl = readConfigApiUrl(configPath)
        if (!configUrl.isNullOrBlank()) {
            return stripApiSuffix(configUrl)
        }

        // 3. Default
        return DEFAULT_API_URL
    }

    internal fun readConfigApiUrl(configPath: String): String? {
        return try {
            val content = File(configPath).readText()
            val match = Regex("""^api-url:\s*(.+)$""", RegexOption.MULTILINE).find(content)
            match?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            null
        }
    }

    internal fun stripApiSuffix(url: String): String {
        return url
            .replace(Regex("""/api/v1/?$"""), "")
            .trimEnd('/')
    }
}
