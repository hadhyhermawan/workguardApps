package com.workguard.core.security

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.provider.Settings

data class FakeGpsCheck(
    val isViolation: Boolean,
    val reasons: List<String>
)

object FakeGpsDetector {
    private const val KEY_MOCK_LOCATION_APP = "mock_location_app"
    private val blockedPackages = setOf(
        "com.fakegps.mock",
        "com.lexa.fakegps",
        "org.freegps.mock",
        "com.gpsmock.pro",
        "com.fakegps.location",
        "com.fakegps.gpsjoy",
        "com.fgps.gps",
        "com.fakegpsstudio.mock",
        "com.fakegps.gpseasy",
        "com.gpsspoof.fakegps",
        "com.locationchanger"
    )

    fun check(
        context: Context,
        location: Location? = null,
        reading: GpsReading? = null
    ): FakeGpsCheck {
        val reasons = mutableListOf<String>()

        if (isMockLocationEnabled(context)) {
            reasons.add("Mock location aktif di Developer Options")
        }

        val mockApp = getMockLocationApp(context)
        if (!mockApp.isNullOrBlank()) {
            reasons.add("Mock location app terdeteksi")
        }

        if (isLocationFromMockProvider(location, reading)) {
            reasons.add("Lokasi berasal dari provider palsu")
        }

        val installed = findInstalledFakeGpsApps(context)
        if (installed.isNotEmpty()) {
            reasons.add("Aplikasi Fake GPS terpasang")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isMockLocationOpAllowed(context, mockApp)) {
            reasons.add("Mock location diizinkan pada AppOps")
        }

        return FakeGpsCheck(
            isViolation = reasons.isNotEmpty(),
            reasons = reasons
        )
    }

    private fun isMockLocationEnabled(context: Context): Boolean {
        return try {
            val enabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ALLOW_MOCK_LOCATION,
                0
            )
            enabled != 0
        } catch (_: Exception) {
            false
        }
    }

    private fun getMockLocationApp(context: Context): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.Secure.getString(
                context.contentResolver,
                KEY_MOCK_LOCATION_APP
            )
        } else {
            null
        }
    }

    private fun isLocationFromMockProvider(location: Location?, reading: GpsReading?): Boolean {
        return location?.isFromMockProvider == true || reading?.isMocked == true
    }

    private fun findInstalledFakeGpsApps(context: Context): List<String> {
        val packageManager = context.packageManager
        return blockedPackages.filter { isPackageInstalled(packageManager, it) }
    }

    private fun isMockLocationOpAllowed(context: Context, mockApp: String?): Boolean {
        if (mockApp.isNullOrBlank()) {
            return false
        }
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        return try {
            val uid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(
                    mockApp,
                    PackageManager.ApplicationInfoFlags.of(0)
                ).uid
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(mockApp, 0).uid
            }
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_MOCK_LOCATION,
                uid,
                mockApp
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    private fun isPackageInstalled(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
