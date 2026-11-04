package net.nightvision.plugin.services

import java.io.File
import java.net.URI
import java.nio.file.Paths

/**
 * Resolves the NightVision API URL using the same precedence as the CLI:
 * 1. NIGHTVISION_API_URL environment variable
 * 2. api-url from ~/.nightvision/nightvision.yml
 * 3. https://api.nightviz.ai (default)
 *
 * The plugin calls the API itself with this URL and never hands it to the
 * CLI. A CLI released before NV-6111 defaults to api.nightvision.net
 * instead; both hosts serve the same API, so a token the CLI creates is
 * accepted on either.
 */
object ApiUrlService {
    private const val DEFAULT_API_URL = "https://api.nightviz.ai"

    // NightVision serves under nightviz.ai and keeps serving its former
    // domain, nightvision.net (NV-6111). App links stay on the domain of
    // the configured API host, so a setup that names a nightvision.net
    // host keeps opening the app there.
    private const val DEFAULT_APP_URL = "https://app.nightviz.ai"
    private val NIGHTVISION_DOMAINS = listOf("nightviz.ai", "nightvision.net")
    private val ENV_API_HOST = Regex("""^api\.([a-z0-9-]+)\.""")

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

    /**
     * Returns the web app that belongs to [apiUrl], so links open in the
     * same environment and on the same domain as the API the plugin calls
     * (NV-4934):
     *
     * - api.<domain>       -> app.<domain>
     * - api.<env>.<domain> -> <env>.<domain>
     *
     * Any other host, including one that does not parse, falls back to the
     * production app on nightviz.ai. The VS Code extension maps the same way.
     */
    fun resolveAppUrl(apiUrl: String): String {
        val host = try {
            URI(apiUrl).host?.lowercase()
        } catch (e: Exception) {
            null
        } ?: return DEFAULT_APP_URL

        for (domain in NIGHTVISION_DOMAINS) {
            if (host == "api.$domain") {
                return "https://app.$domain"
            }
            val env = ENV_API_HOST.find(host)?.groupValues?.get(1)
            if (env != null && host == "api.$env.$domain") {
                return "https://$env.$domain"
            }
        }
        return DEFAULT_APP_URL
    }
}
