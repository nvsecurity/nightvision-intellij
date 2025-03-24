package net.nightvision.plugin.intellij

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class Constants {
    companion object {
        const val NIGHTVISION = "nightvision"
        const val API_V1_SUFFIX = "api/v1"
        const val API_URL = "https://api.nightvision.net"
        const val CONTACT_EMAIL = "support@nightvision.net"

        fun getUrlFor(suffix: String): URI {
            return URI.create("${API_URL}/${API_V1_SUFFIX}/${suffix}/")
        }

        fun getUrlFor(suffix: String, params: Map<String, String> = emptyMap()): URI {
            var base = "${API_URL}/${API_V1_SUFFIX}/${suffix}/"
            val query = params.entries
                .filter { it.value.isNotBlank() } // Only include non-empty values
                .joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, StandardCharsets.UTF_8)}" }
            val url = if (query.isNotEmpty()) "$base?$query" else base
            return URI.create(url)
        }
    }
}