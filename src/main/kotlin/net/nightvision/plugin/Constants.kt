package net.nightvision.plugin

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class Constants {
    companion object {
        const val NIGHTVISION = "nightvision"
        const val API_V1_SUFFIX = "api/v1"
        const val API_URL = "https://api.nightvision.net"
        const val API_V1_URL = "$API_URL/$API_V1_SUFFIX"

        const val APP_URL = "https://app.nightvision.net"

        const val CONTACT_EMAIL = "support@nightvision.net"

        const val CLI_VERSION = "0.9.5"

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