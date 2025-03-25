package net.nightvision.plugin.intellij.models

import com.google.gson.annotations.SerializedName

data class TargetInfo(
    val id: String,
    val name: String,
    @SerializedName("own_user")
    val owner: UserInfo,
    val project: String,
    @SerializedName("project_name")
    val projectName: String,
    val location: String,
    val type: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("last_scanned_at")
    val lastScannedAt: String?,
    @SerializedName("last_updated_at")
    val lastUpdatedAt: String?,
    @SerializedName("internet_accessible")
    val internetAccessible: Boolean,
    @SerializedName("swaggerfile_name")
    val swaggerFileName: String?,
    @SerializedName("swaggerfile_url")
    val swaggerFileURL: String?,

//    val specUrlForDownload: String?, // TODO: Need to load using ${API_URL}/api/v1/targets/openapi/${targetId}/get-spec-url/

    @SerializedName("last_spec_uploaded_at")
    val lastSpecUploadedAt: String?,
    @SerializedName("spec_status")
    val specStatus: String, // TODO: Use Enum?
    @SerializedName("is_ready_to_scan")
    val isReadyToScan: Boolean,
    val configuration: ConfigurationInfo?,

    @SerializedName("has_spec_uploaded")
    val hasSpecUploaded: Boolean,
)

data class TargetURL(
    val url: String,
)

data class ConfigurationInfo(
    @SerializedName("excluded_url_patterns")
    val excludedUrlPatterns: List<String>,
    @SerializedName("excluded_x_paths")
    val excludedXPaths: List<String>,
)