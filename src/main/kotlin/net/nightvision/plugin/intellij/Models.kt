package net.nightvision.plugin.intellij

import com.google.gson.annotations.SerializedName

data class PaginatedResult<T>(
    val results: List<T>
)

data class Scan(
    val id: String,
    val location: String,

    @SerializedName("target_name")
    val targetName: String,

    val credentials: Credentials?,
    val project: Project,

    @SerializedName("created_at")
    val createdAt: String
)

data class Credentials(
    val id: String,
    val name: String,
)

data class Project(
    val id: String,
    val name: String,
)
