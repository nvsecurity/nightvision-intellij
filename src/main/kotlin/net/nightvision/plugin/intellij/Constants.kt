package net.nightvision.plugin.intellij

import java.net.URI

class Constants {
    companion object {
        const val NIGHTVISION = "nightvision"
        const val API_V1_SUFFIX = "api/v1"
        const val API_URL = "https://api.nightvision.net"
        const val CONTACT_EMAIL = "support@nightvision.net"

        fun getUrlFor(suffix: String): URI {
            return URI.create("${API_URL}/${API_V1_SUFFIX}/${suffix}/")
        }
    }
}