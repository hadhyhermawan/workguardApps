package com.workguard.home.data

import android.content.Context
import android.util.Log
import com.workguard.core.datastore.AuthDataStore
import com.workguard.core.location.LocationSnapshot
import com.workguard.core.network.ApiResult
import com.workguard.core.network.ApiService
import com.workguard.core.network.EmployeeHomeResponse
import com.workguard.core.network.HomeActivityItemResponse
import com.workguard.core.network.HomeTaskItemResponse
import com.workguard.core.network.TrackingPingRequest
import com.workguard.core.network.TrackingPingResponse
import com.workguard.core.util.UrlUtil
import com.workguard.core.util.BatteryStatusProvider
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import dagger.hilt.android.qualifiers.ApplicationContext

class HomeRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val authDataStore: AuthDataStore,
    @ApplicationContext private val context: Context
) : HomeRepository {
    companion object {
        private const val TAG = "HomeRepository"
    }

    override suspend fun loadHome(companyCode: String?): ApiResult<HomeSummary> {
        val endpointResult = loadHomeFromEndpoint()
        if (endpointResult is ApiResult.Success) {
            return endpointResult
        }
        if (endpointResult is ApiResult.Error) {
            Log.w(TAG, "Employee home endpoint failed: ${endpointResult.throwable.message}")
        }
        return loadHomeLegacy(companyCode)
    }

    override suspend fun sendTrackingPing(location: LocationSnapshot?): ApiResult<TrackingPingResponse> {
        if (location == null) {
            return ApiResult.Error(IllegalStateException("Lokasi belum tersedia"))
        }
        val batteryStatus = BatteryStatusProvider.getStatus(context)
        val request = TrackingPingRequest(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracyMeters,
            isMockLocation = location.isMocked,
            batteryLevel = batteryStatus?.level,
            isCharging = batteryStatus?.isCharging
        )
        val validationError = validatePingRequest(request)
        if (validationError != null) {
            Log.w(TAG, "Tracking ping skipped: $validationError request=$request")
            return ApiResult.Error(IllegalStateException(validationError))
        }
        return try {
            val response = apiService.trackingPing(request)
            if (response.success == false) {
                Log.w(TAG, "Tracking ping failed: ${response.message} request=$request")
                ApiResult.Error(IllegalStateException(response.message ?: "Tracking gagal"))
            } else {
                ApiResult.Success(response.data ?: TrackingPingResponse())
            }
        } catch (e: HttpException) {
            val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            Log.w(
                TAG,
                "Tracking ping http error: code=${e.code()} body=$errorBody request=$request"
            )
            ApiResult.Error(IllegalStateException("Tracking gagal (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    private fun validatePingRequest(request: TrackingPingRequest): String? {
        if (request.latitude !in -90.0..90.0 || request.longitude !in -180.0..180.0) {
            return "Koordinat lokasi tidak valid"
        }
        val accuracy = request.accuracy
        if (accuracy == null || accuracy <= 0f) {
            return "Akurasi lokasi tidak valid"
        }
        if (request.isMockLocation == true) {
            return "Lokasi terdeteksi palsu"
        }
        val batteryLevel = request.batteryLevel
        if (batteryLevel == null || batteryLevel !in 0..100) {
            return "Level baterai tidak valid"
        }
        if (request.isCharging == null) {
            return "Status charging tidak valid"
        }
        return null
    }

    private suspend fun loadHomeFromEndpoint(): ApiResult<HomeSummary> {
        return try {
            val response = apiService.getEmployeeHome()
            if (response.success == false) {
                return ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal mengambil data home")
                )
            }
            val data = response.data
                ?: return ApiResult.Error(IllegalStateException("Data home kosong"))
            ApiResult.Success(mapHomeResponse(data))
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal mengambil data home (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    private suspend fun loadHomeLegacy(companyCode: String?): ApiResult<HomeSummary> {
        return try {
            coroutineScope {
                val profileDeferred = async { apiService.getProfile() }
                val meDeferred = async { apiService.getMe() }
                val resolvedCompanyCode = companyCode
                    ?.takeIf { it.isNotBlank() }
                    ?: authDataStore.companyCode()?.takeIf { it.isNotBlank() }
                val settingsDeferred = async { apiService.getPublicSettings(resolvedCompanyCode) }

                val profile = profileDeferred.await()
                val me = meDeferred.await()
                val settings = settingsDeferred.await()

                if (profile.success == false) {
                    return@coroutineScope ApiResult.Error(
                        IllegalStateException(profile.message ?: "Gagal mengambil profil")
                    )
                }
                if (me.success == false) {
                    return@coroutineScope ApiResult.Error(
                        IllegalStateException(me.message ?: "Gagal mengambil data akun")
                    )
                }
                if (settings.success == false) {
                    return@coroutineScope ApiResult.Error(
                        IllegalStateException(settings.message ?: "Gagal mengambil data perusahaan")
                    )
                }

                val profileData = profile.data
                val displayName = profileData?.fullName?.takeIf { it.isNotBlank() }
                    ?: profileData?.username?.takeIf { it.isNotBlank() }
                    ?: "Employee"
                val photoUrl = UrlUtil.resolveAssetUrl(
                    profileData?.employeePhotoUrl?.takeIf { it.isNotBlank() }
                        ?: profileData?.photoUrl?.takeIf { it.isNotBlank() }
                )
                val companyName = settings.data?.companyName ?: settings.data?.name
                val companyLogo = UrlUtil.resolveAssetUrl(
                    settings.data?.logoUrl?.takeIf { it.isNotBlank() }
                        ?: settings.data?.logo?.takeIf { it.isNotBlank() }
                )

                ApiResult.Success(
                    HomeSummary(
                        displayName = displayName,
                        role = profileData?.role,
                        photoUrl = photoUrl,
                        employeeId = me.data?.employeeId,
                        companyId = me.data?.companyId,
                        companyName = companyName,
                        companyLogoUrl = companyLogo
                    )
                )
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal mengambil data home (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    private fun mapHomeResponse(data: EmployeeHomeResponse): HomeSummary {
        val summary = data.todayTask?.summary
        val taskSummary = HomeTaskSummary(
            started = summary?.started ?: 0,
            completed = summary?.completed ?: 0,
            cancelled = summary?.cancelled ?: 0
        )
        val taskItems = mapTaskItems(data.todayTask?.items)
        val activityItems = mapActivityItems(data.recentActivity?.items)
        val quickStats = data.quickStats?.let {
            HomeQuickStats(
                attendanceStatus = it.attendanceStatus,
                checkInAt = it.checkInAt,
                checkOutAt = it.checkOutAt,
                pendingPermits = it.pendingPermits ?: 0,
                pendingOvertimes = it.pendingOvertimes ?: 0,
                violationsToday = it.violationsToday ?: 0
            )
        } ?: HomeQuickStats()

        return HomeSummary(
            displayName = data.displayName?.takeIf { it.isNotBlank() } ?: "Employee",
            role = data.role,
            photoUrl = UrlUtil.resolveAssetUrl(data.photoUrl),
            employeeId = data.employeeId,
            companyId = data.companyId,
            companyName = data.companyName,
            companyLogoUrl = UrlUtil.resolveAssetUrl(data.companyLogoUrl),
            todayTaskSummary = taskSummary,
            todayTasks = taskItems,
            recentActivities = activityItems,
            quickStats = quickStats,
            errorMessage = data.errorMessage?.takeIf { it.isNotBlank() }
        )
    }

    private fun mapTaskItems(items: List<HomeTaskItemResponse>?): List<HomeTaskItem> {
        if (items.isNullOrEmpty()) {
            return emptyList()
        }
        return items.mapIndexed { index, item ->
            HomeTaskItem(
                id = item.id,
                title = item.title?.takeIf { it.isNotBlank() } ?: "Tugas ${index + 1}",
                date = item.date,
                time = item.time,
                description = item.description,
                status = item.status
            )
        }
    }

    private fun mapActivityItems(items: List<HomeActivityItemResponse>?): List<HomeActivityItem> {
        if (items.isNullOrEmpty()) {
            return emptyList()
        }
        return items.mapIndexed { index, item ->
            HomeActivityItem(
                title = item.title?.takeIf { it.isNotBlank() } ?: "Aktivitas ${index + 1}",
                date = item.date,
                time = item.time,
                status = item.status,
                statusColor = item.statusColor,
                type = item.type
            )
        }
    }
}
