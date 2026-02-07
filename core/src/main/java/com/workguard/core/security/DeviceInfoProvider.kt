package com.workguard.core.security

import android.os.Build
import javax.inject.Inject

interface DeviceInfoProvider {
    fun deviceModel(): String
    fun deviceManufacturer(): String
}

class DefaultDeviceInfoProvider @Inject constructor() : DeviceInfoProvider {
    override fun deviceModel(): String = Build.MODEL ?: "unknown"

    override fun deviceManufacturer(): String = Build.MANUFACTURER ?: "unknown"
}
