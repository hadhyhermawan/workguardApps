package com.workguard.core.util

object FileUtil {
    fun sanitizeFileName(value: String): String {
        return value.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}
