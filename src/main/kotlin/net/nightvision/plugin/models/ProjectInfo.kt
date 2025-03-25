package net.nightvision.plugin.models

import com.google.gson.annotations.SerializedName

data class ProjectInfo(
    val id: String,
    val name: String,
    @SerializedName("own_user")
    val owner: UserInfo,
    @SerializedName("shared_with_users_preview")
    val sharedWithUsers: List<UserInfo>,
    @SerializedName("targets_count")
    val targetsCount: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("last_updated_at")
    val lastUpdatedAt: String?,
    @SerializedName("is_default")
    val isDefault: Boolean,
)