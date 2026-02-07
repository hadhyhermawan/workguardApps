package com.workguard.core.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object IsoTimeUtil {
    private val parseFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )

    fun parseMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        for (pattern in parseFormats) {
            val formatter = SimpleDateFormat(pattern, Locale.US)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            val date = runCatching { formatter.parse(value) }.getOrNull()
            if (date != null) {
                return date.time
            }
        }
        return null
    }

    fun formatUtc(millis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(java.util.Date(millis))
    }
}
