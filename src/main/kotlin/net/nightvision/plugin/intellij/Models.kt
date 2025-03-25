package net.nightvision.plugin.intellij

import com.google.gson.annotations.SerializedName

data class PaginatedResult<T>(
    val results: List<T>
)

data class Scan(
    val id: String,
    val location: String,
    val credentials: Credentials?,
    val project: Project,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("started_at")
    val startedAt: String,

    @SerializedName("ended_at")
    val endedAt: String,

    @SerializedName("status_value")
    val statusValue: String,

    @SerializedName("vulnerable_paths_statistics")
    val vulnPathStatistics: VulnerablePathStatistics,

    @SerializedName("internet_accessible")
    val accessibility: String?,

    @SerializedName("target_name")
    val targetName: String,

    @SerializedName("target_type")
    val targetType: String,

    @SerializedName("target_id")
    val targetId: String?,
)

data class Credentials(
    val id: String,
    val name: String,
)

data class Project(
    val id: String,
    val name: String,
)

data class VulnerablePathStatistics(
    val Critical: Int,
    val High: Int,
    val Medium: Int,
    val Low: Int,
    val Informational: Int,
)
