package com.workguard.attendance.data

import android.content.Context
import android.util.Log
import com.workguard.core.datastore.FaceSessionStore
import com.workguard.core.location.LocationProvider
import com.workguard.core.model.Attendance
import com.workguard.core.model.enums.AttendanceStatus
import com.workguard.core.network.ApiResult
import com.workguard.core.network.ApiService
import com.workguard.core.network.AttendanceRequest
import com.workguard.core.network.AttendanceResponse
import com.workguard.core.network.AttendanceRulesResponse
import com.workguard.core.network.AttendanceTodayResponse
import com.workguard.core.security.DeviceInfoProvider
import com.workguard.core.util.AppInfoUtil
import com.workguard.core.util.BatteryStatusProvider
import com.workguard.core.util.Clock
import com.workguard.core.util.IsoTimeUtil
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException
import dagger.hilt.android.qualifiers.ApplicationContext

class AttendanceRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val faceSessionStore: FaceSessionStore,
    private val locationProvider: LocationProvider,
    private val deviceInfoProvider: DeviceInfoProvider,
    @ApplicationContext private val context: Context,
    private val clock: Clock
) : AttendanceRepository {
    companion object {
        private const val TAG = "AttendanceRepository"
    }

    override suspend fun checkIn(): ApiResult<Attendance> {
        return submitAttendance(action = "CHECK_IN")
    }

    override suspend fun checkOut(): ApiResult<Attendance> {
        return submitAttendance(action = "CHECK_OUT")
    }

    override suspend fun getTodayStatus(date: String?): ApiResult<AttendanceTodayResponse> {
        return try {
            val response = apiService.getAttendanceToday(date)
            if (response.success == false) {
                ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal mengambil status absensi")
                )
            } else {
                val data = response.data
                    ?: return ApiResult.Error(IllegalStateException("Status absensi kosong"))
                ApiResult.Success(data)
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal mengambil status absensi (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun getRules(date: String?): ApiResult<AttendanceRulesResponse> {
        return try {
            val response = apiService.getAttendanceRules(date)
            if (response.success == false) {
                ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal mengambil aturan absensi")
                )
            } else {
                val data = response.data
                    ?: return ApiResult.Error(IllegalStateException("Aturan absensi kosong"))
                ApiResult.Success(data)
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal mengambil aturan absensi (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    private suspend fun submitAttendance(action: String): ApiResult<Attendance> {
        val faceSession = faceSessionStore.getSession()
            ?: return ApiResult.Error(IllegalStateException("Face session belum tersedia"))
        val nowMillis = clock.nowMillis()
        val request = buildAttendanceRequest(faceSession.sessionId, action, nowMillis)
        return try {
            val response = apiService.submitAttendance(request)
            if (response.success == false) {
                Log.w(TAG, "Attendance submit failed: ${response.message} action=$action request=$request")
                ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal mengirim absensi")
                )
            } else {
                val data = response.data
                    ?: return ApiResult.Error(IllegalStateException("Respons absensi kosong"))
                ApiResult.Success(mapAttendance(data, faceSession.sessionId, nowMillis))
            }
        } catch (e: HttpException) {
            val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val serverMessage = extractServerMessage(errorBody)
            Log.w(
                TAG,
                "Attendance submit http error: code=${e.code()} body=$errorBody action=$action request=$request"
            )
            val message = serverMessage ?: "Gagal mengirim absensi (${e.code()})"
            ApiResult.Error(IllegalStateException(message))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    private suspend fun buildAttendanceRequest(
        faceSessionId: Long,
        action: String,
        recordedAtMillis: Long
    ): AttendanceRequest {
        val location = runCatching { locationProvider.getLastKnownLocation() }.getOrNull()
        val batteryStatus = BatteryStatusProvider.getStatus(context)
        val (appVersion, appVersionCode) = AppInfoUtil.resolveAppVersionInfo(context)
        return AttendanceRequest(
            action = action,
            faceSessionId = faceSessionId,
            latitude = location?.latitude,
            longitude = location?.longitude,
            accuracy = location?.accuracyMeters,
            isMockLocation = location?.isMocked,
            provider = location?.provider,
            batteryLevel = batteryStatus?.level,
            isCharging = batteryStatus?.isCharging,
            recordedAt = IsoTimeUtil.formatUtc(recordedAtMillis),
            deviceModel = deviceInfoProvider.deviceModel().toValidValue(),
            deviceManufacturer = deviceInfoProvider.deviceManufacturer().toValidValue(),
            appVersion = appVersion.toValidValue(),
            appVersionCode = appVersionCode
        )
    }

    private fun mapAttendance(
        response: AttendanceResponse,
        faceSessionId: Long,
        fallbackMillis: Long
    ): Attendance {
        val status = mapStatus(response.status)
        val timestamp = when (status) {
            AttendanceStatus.CHECKED_OUT -> IsoTimeUtil.parseMillis(response.checkOutAt)
            AttendanceStatus.CHECKED_IN -> IsoTimeUtil.parseMillis(response.checkInAt)
            AttendanceStatus.UNKNOWN -> null
        } ?: fallbackMillis
        val id = response.attendanceId?.toString() ?: faceSessionId.toString()
        return Attendance(id = id, status = status, timestampMillis = timestamp)
    }

    private fun mapStatus(status: String?): AttendanceStatus {
        return when (status?.trim()?.uppercase()) {
            "CHECKED_IN", "CHECK_IN" -> AttendanceStatus.CHECKED_IN
            "CHECKED_OUT", "CHECK_OUT" -> AttendanceStatus.CHECKED_OUT
            else -> AttendanceStatus.UNKNOWN
        }
    }

    private fun String?.toValidValue(): String? {
        val value = this?.trim().orEmpty()
        return if (value.isBlank() || value.equals("unknown", ignoreCase = true)) {
            null
        } else {
            value
        }
    }

    private fun extractServerMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val match = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body) ?: return null
        return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
    }
}
