package com.workguard.navigation

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object NewsTimeFormatter {
    private val parseFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )

    fun formatRelativeTime(publishedAt: String?): String {
        if (publishedAt.isNullOrBlank()) {
            return ""
        }
        val millis = parseIsoMillis(publishedAt) ?: return publishedAt
        val now = System.currentTimeMillis()
        if (millis > now) {
            return "Baru saja"
        }
        val diff = now - millis
        val minutes = diff / 60000
        if (minutes < 1) return "Baru saja"
        if (minutes < 60) return "$minutes menit lalu"
        val hours = diff / 3600000
        if (hours < 24) return "$hours jam lalu"
        val days = diff / 86400000
        if (days == 1L) return "Kemarin"
        if (days < 7) return "$days hari lalu"
        val formatter = SimpleDateFormat("d MMM yyyy", Locale("id", "ID"))
        return formatter.format(java.util.Date(millis))
    }

    fun isOlderThanDays(publishedAt: String?, days: Long): Boolean {
        if (publishedAt.isNullOrBlank()) {
            return false
        }
        val millis = parseIsoMillis(publishedAt) ?: return false
        val diff = System.currentTimeMillis() - millis
        return diff >= days * 86_400_000L
    }

    private fun parseIsoMillis(value: String): Long? {
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
}

