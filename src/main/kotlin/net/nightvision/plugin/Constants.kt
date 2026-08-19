package net.nightvision.plugin

import net.nightvision.plugin.services.ApiUrlService
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class Constants {
    companion object {
        const val NIGHTVISION = "nightvision"
        const val API_V1_SUFFIX = "api/v1"
        val API_URL: String = ApiUrlService.resolveApiUrl()
        val API_V1_URL: String = "$API_URL/$API_V1_SUFFIX"

        const val APP_URL = "https://app.nightvision.net" // TODO: derive from API_URL if environments diverge

        const val CONTACT_EMAIL = "support@nightviz.ai"

        // Oldest NightVision CLI the plugin will use without prompting for an
        // update. Raise this when the plugin starts relying on newer CLI
        // behaviour.
        //
        // 0.15.0 is the oldest version observed to complete a scan of a target
        // that is not reachable from the internet: the QUIC relay transport and
        // its yamux fallback arrived in 0.13.0, and older CLIs fail to bring up
        // the relay tunnel (NV-4827). This was pinned at 0.9.5 long enough for
        // that to reach a customer of the VS Code extension, so NV-4872 tracks
        // replacing the literal with something that stays current.
        const val CLI_VERSION = "0.15.0"

        fun getApiUrlFor(suffix: String): URI {
            return getUrlFor(API_V1_URL, suffix);
        }

        fun getApiUrlFor(suffix: String, params: Map<String, String> = emptyMap()): URI {
            return getUrlFor(API_V1_URL, suffix, params);
        }

        fun getAppUrlFor(suffix: String): URI {
            return getUrlFor(APP_URL, suffix);
        }

        fun getAppUrlFor(suffix: String, params: Map<String, String> = emptyMap()): URI {
            return getUrlFor(APP_URL, suffix, params);
        }

        fun getUrlFor(baseUrl: String, suffix: String): URI {
            return URI.create("${baseUrl}/${suffix}/")
        }

        fun getUrlFor(baseUrl: String, suffix: String, params: Map<String, String> = emptyMap()): URI {
            var base = "${baseUrl}/${suffix}/"
            val query = params.entries
                .filter { it.value.isNotBlank() } // Only include non-empty values
                .joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, StandardCharsets.UTF_8)}" }
            val url = if (query.isNotEmpty()) "$base?$query" else base
            return URI.create(url)
        }
    }
}