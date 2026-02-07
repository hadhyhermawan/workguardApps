package com.workguard.core.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

data class DeviceSecurityCheck(
    val isViolation: Boolean,
    val reasons: List<String>
)

object DeviceSecurityChecker {
    private val suPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/su",
        "/system/bin/.ext/.su",
        "/system/usr/we-need-root/su-backup",
        "/system/app/Superuser.apk",
        "/system/xbin/daemonsu",
        "/su/bin/su"
    )

    private val magiskPackages = setOf(
        "com.topjohnwu.magisk",
        "com.topjohnwu.magisk.debug"
    )

    private val hookPackages = setOf(
        "de.robv.android.xposed.installer",
        "org.meowcat.edxposed.manager",
        "com.solohsu.android.edxp.manager",
        "com.lsposed.manager",
        "com.saurik.substrate"
    )

    fun check(context: Context): DeviceSecurityCheck {
        val reasons = mutableListOf<String>()

        if (isDebuggable(context)) {
            reasons.add("Debuggable build")
        }
        if (hasSuBinary()) {
            reasons.add("su binary ditemukan")
        }
        if (hasTestKeys()) {
            reasons.add("test-keys terdeteksi")
        }
        if (hasMagiskApp(context)) {
            reasons.add("Magisk terdeteksi")
        }
        if (hasHookFramework(context)) {
            reasons.add("Hook framework terdeteksi")
        }
        if (isEmulator()) {
            reasons.add("Emulator terdeteksi")
        }

        return DeviceSecurityCheck(
            isViolation = reasons.isNotEmpty(),
            reasons = reasons
        )
    }

    private fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun hasSuBinary(): Boolean {
        if (suPaths.any { File(it).exists() }) {
            return true
        }
        return hasSuInPath()
    }

    private fun hasSuInPath(): Boolean {
        val whichLocations = listOf("/system/xbin/which", "/system/bin/which", "which")
        return whichLocations.any { which ->
            try {
                val process = Runtime.getRuntime().exec(arrayOf(which, "su"))
                val output = process.inputStream.bufferedReader().use { it.readLine() }
                output != null && output.isNotBlank()
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun hasTestKeys(): Boolean {
        return Build.TAGS?.contains("test-keys") == true
    }

    private fun hasMagiskApp(context: Context): Boolean {
        return magiskPackages.any { isPackageInstalled(context.packageManager, it) }
    }

    private fun hasHookFramework(context: Context): Boolean {
        if (hookPackages.any { isPackageInstalled(context.packageManager, it) }) {
            return true
        }
        if (hasXposedFiles()) {
            return true
        }
        if (hasHookClasses()) {
            return true
        }
        return hasSuspiciousLibraries()
    }

    private fun hasXposedFiles(): Boolean {
        val paths = listOf(
            "/system/framework/XposedBridge.jar",
            "/system/lib/libxposed_art.so",
            "/system/lib64/libxposed_art.so"
        )
        return paths.any { File(it).exists() }
    }

    private fun hasHookClasses(): Boolean {
        return try {
            Class.forName("de.robv.android.xposed.XposedBridge")
            true
        } catch (_: Exception) {
            try {
                Class.forName("com.saurik.substrate.MS")
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun hasSuspiciousLibraries(): Boolean {
        return try {
            val maps = File("/proc/self/maps").readText()
            maps.contains("frida", ignoreCase = true) ||
                maps.contains("substrate", ignoreCase = true) ||
                maps.contains("xposed", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    private fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val brand = Build.BRAND.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()
        val hardware = Build.HARDWARE.lowercase()

        return fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator") ||
            fingerprint.contains("unknown") ||
            model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("android sdk built for x86") ||
            brand.startsWith("generic") && device.startsWith("generic") ||
            product.contains("sdk") ||
            product.contains("emulator") ||
            product.contains("simulator") ||
            product.contains("vbox") ||
            product.contains("nox") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            hardware.contains("vbox86")
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
