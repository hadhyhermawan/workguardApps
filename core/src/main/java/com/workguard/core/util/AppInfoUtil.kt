package com.workguard.core.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

object AppInfoUtil {
    private const val TAG = "AppInfoUtil"

    fun resolveAppVersionInfo(context: Context): Pair<String?, Long?> {
        return try {
            val packageManager = context.packageManager
            val packageName = context.packageName
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            val versionName = info.versionName?.takeIf { it.isNotBlank() }
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            versionName to versionCode
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve app version", e)
            null to null
        }
    }
}
