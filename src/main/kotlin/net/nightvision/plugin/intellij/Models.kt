package net.nightvision.plugin.intellij

data class ScansPage(
    val results: List<Scan>
)

data class Scan(
    val id: String,
    val location: String,
    val target_name: String,
    val credentials: Credentials?,
    val created_at: String
)

data class Credentials(
    val id: String,
    val name: String,
)
