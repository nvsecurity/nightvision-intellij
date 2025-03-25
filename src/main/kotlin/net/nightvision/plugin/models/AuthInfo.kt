package net.nightvision.plugin.models

import com.google.gson.annotations.SerializedName

data class Auth(
    val id: String,
    val name: String,
    val type: String, // TODO: Consider Enum of 'COOKIE' | 'HEADER' | 'SCRIPT'
    val description: String?,
    val headers: List<AuthHeader>?,
    val url: String?,
)

data class AuthHeader(
    val name: String,
    val value: String,
)

data class AuthInfo(
    val id: String,
    val name: String,
    val type: String, // TODO: Consider Enum of 'COOKIE' | 'HEADER' | 'SCRIPT'
    val description: String?,
    val headers: List<AuthHeader>?,
    @SerializedName("script_first_url")
    val url: String?,
    val project: String,
    @SerializedName("project_name")
    val projectName: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("last_updated_at")
    val lastUpdatedAt: String?,
    @SerializedName("script_content")
    val scriptContent: String?,
)