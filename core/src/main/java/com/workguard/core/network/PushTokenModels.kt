package com.workguard.core.network

import com.squareup.moshi.Json

data class PushTokenRequest(
    @Json(name = "fcm_token")
    val fcmToken: String,
    @Json(name = "device_id")
    val deviceId: String? = null,
    val platform: String? = "android"
)

data class PushTokenResponse(
    val id: Int? = null,
    @Json(name = "fcm_token")
    val fcmToken: String? = null
)
