package com.workguard.core.util

object UrlUtil {
    private const val ASSET_BASE_URL = "https://k3guard.cloud"

    fun resolveAssetUrl(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) {
            return null
        }
        return when {
            value.startsWith("http", ignoreCase = true) -> value
            value.startsWith("/") -> ASSET_BASE_URL + value
            else -> "$ASSET_BASE_URL/$value"
        }
    }
}
