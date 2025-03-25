package net.nightvision.plugin.models

import com.google.gson.annotations.SerializedName

data class UserInfo(
    val id: String,
    val username: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("avatar_url")
    val avatarUrl: String,
    @SerializedName("is_staff")
    val isStaff: String
)