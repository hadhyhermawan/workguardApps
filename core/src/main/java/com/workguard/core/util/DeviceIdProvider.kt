package com.workguard.core.util

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface DeviceIdProvider {
    fun getDeviceId(): String
}

class AndroidDeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceIdProvider {
    override fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }
}
