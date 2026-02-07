package com.workguard.auth.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.workguard.core.datastore.AuthDataStore
import com.workguard.core.model.Employee
import com.workguard.core.model.User
import com.workguard.core.network.ApiResult
import com.workguard.core.network.ApiService
import com.workguard.core.network.DeviceRegisterRequest
import com.workguard.core.network.LoginRequest
import com.workguard.core.network.PushTokenRequest
import com.workguard.core.security.DeviceInfoProvider
import com.workguard.core.util.Clock
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val authDataStore: AuthDataStore,
    private val apiService: ApiService,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val clock: Clock
) : AuthRepository {
    companion object {
        private const val TAG = "AuthRepository"
    }
    override suspend fun login(
        companyCode: String,
        employeeCode: String,
        password: String
    ): ApiResult<User> {
        if (companyCode.isBlank() || employeeCode.isBlank() || password.isBlank()) {
            return ApiResult.Error(IllegalArgumentException("Missing credentials"))
        }
        return try {
            val request = LoginRequest(
                employeeCode = employeeCode,
                password = password,
                companyCode = companyCode
            )
            Log.d(TAG, "Login API request started")
            val response = apiService.loginEmployee(
                request = request,
                userAgent = buildUserAgent()
            )
            val data = response.data
            if (!response.success || data == null) {
                Log.w(TAG, "Login API response failed: ${response.message}")
                return ApiResult.Error(IllegalStateException(response.message ?: "Login failed"))
            }
            val expiresAtMillis = resolveExpiresAtMillis(data.expiresIn)
            authDataStore.saveTokens(
                accessToken = data.accessToken,
                refreshToken = "",
                tokenType = data.tokenType,
                expiresAtMillis = expiresAtMillis
            )
            authDataStore.saveCompanyCode(companyCode)
            syncDeviceLogin()
            syncPushToken()
            val user = User(
                id = data.user.id.toString(),
                employeeCode = data.user.username,
                displayName = data.user.username
            )
            Log.i(TAG, "Login API success: userId=${user.id}")
            ApiResult.Success(user)
        } catch (e: HttpException) {
            Log.w(TAG, "Login API error: http=${e.code()}")
            val message = when (e.code()) {
                400 -> "NIK/password kosong atau company tidak valid"
                401 -> "Kredensial tidak valid"
                429 -> "Terlalu banyak percobaan, coba lagi nanti"
                else -> "Login gagal (${e.code()})"
            }
            ApiResult.Error(IllegalStateException(message))
        } catch (e: IOException) {
            Log.w(TAG, "Login API error: network", e)
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            Log.e(TAG, "Login API error: unexpected", e)
            ApiResult.Error(e)
        }
    }

    override suspend fun getProfile(): ApiResult<Employee> {
        val employee = Employee(
            id = UUID.randomUUID().toString(),
            name = "Employee",
            role = "Staff"
        )
        return ApiResult.Success(employee)
    }

    override suspend fun logout() {
        Log.i(TAG, "Logout requested")
        try {
            val response = apiService.logoutDevice(buildDeviceRequest())
            if (response.success == true) {
                Log.i(TAG, "Device logout success")
            } else {
                Log.w(TAG, "Device logout failed: ${response.message}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Device logout error", e)
        }
        authDataStore.clear()
    }

    private fun buildUserAgent(): String {
        val manufacturer = deviceInfoProvider.deviceManufacturer()
        val model = deviceInfoProvider.deviceModel()
        val androidVersion = Build.VERSION.RELEASE ?: "unknown"
        return "WorkGuard-Android ($manufacturer $model; Android $androidVersion)"
    }

    private fun resolveDeviceId(): String {
        val androidId = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        if (!androidId.isNullOrBlank()) {
            return androidId
        }
        val prefs = appContext.getSharedPreferences("workguard_device", Context.MODE_PRIVATE)
        val cached = prefs.getString("device_id", null)
        if (!cached.isNullOrBlank()) {
            return cached
        }
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString("device_id", generated).apply()
        return generated
    }

    private fun buildDeviceModel(): String? {
        val manufacturer = deviceInfoProvider.deviceManufacturer().trim()
        val model = deviceInfoProvider.deviceModel().trim()
        val combined = listOf(manufacturer, model)
            .filter { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }
            .joinToString(" ")
        return combined.ifBlank { null }
    }

    private fun resolveAppVersionInfo(): Pair<String?, Long?> {
        return try {
            val packageManager = appContext.packageManager
            val packageName = appContext.packageName
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

    private fun buildDeviceRequest(): DeviceRegisterRequest {
        val (appVersion, appVersionCode) = resolveAppVersionInfo()
        return DeviceRegisterRequest(
            deviceId = resolveDeviceId(),
            deviceModel = buildDeviceModel(),
            osVersion = Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString(),
            appVersion = appVersion,
            appVersionCode = appVersionCode
        )
    }

    private fun resolveExpiresAtMillis(expiresIn: String?): Long? {
        val raw = expiresIn?.trim().orEmpty()
        if (raw.isBlank()) {
            return null
        }
        val normalized = raw.lowercase()
        val seconds = normalized.toLongOrNull()
            ?: normalized.removeSuffix("s").toLongOrNull()
        if (seconds == null || seconds <= 0L) {
            return null
        }
        val millisToAdd = seconds * 1000L
        val now = clock.nowMillis()
        val expiresAt = now + millisToAdd
        return if (expiresAt <= now) null else expiresAt
    }

    private suspend fun syncDeviceLogin() {
        val request = buildDeviceRequest()
        try {
            val response = apiService.registerDevice(request)
            if (response.success == true) {
                Log.i(TAG, "Device register success")
            } else {
                Log.w(TAG, "Device register failed: ${response.message}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Device register error", e)
        }
        try {
            val response = apiService.loginDevice(request)
            if (response.success == true) {
                Log.i(TAG, "Device login success")
            } else {
                Log.w(TAG, "Device login failed: ${response.message}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Device login error", e)
        }
    }

    private suspend fun syncPushToken() {
        val token = fetchFcmToken()?.trim()
        if (token.isNullOrBlank()) {
            Log.w(TAG, "FCM token unavailable for registration")
            return
        }
        try {
            val response = apiService.registerPushToken(
                PushTokenRequest(
                    fcmToken = token,
                    deviceId = resolveDeviceId(),
                    platform = "android"
                )
            )
            if (response.success == true) {
                Log.i(TAG, "Push token registered")
            } else {
                Log.w(TAG, "Push token register failed: ${response.message}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Push token register error", e)
        }
    }

    private suspend fun fetchFcmToken(): String? {
        return try {
            withContext(Dispatchers.IO) {
                Tasks.await(FirebaseMessaging.getInstance().token)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch FCM token", e)
            null
        }
    }
}
