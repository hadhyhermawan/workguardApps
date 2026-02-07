package com.workguard.patrol.data

import android.util.Log
import com.workguard.core.datastore.FaceSessionStore
import com.workguard.core.location.LocationProvider
import com.workguard.core.model.enums.CameraFacing
import com.workguard.core.model.enums.FaceContext
import com.workguard.core.network.ApiResult
import com.workguard.core.network.ApiService
import com.workguard.core.network.PatrolScanRequest
import com.workguard.core.network.PatrolSessionStartRequest
import com.workguard.core.util.FileUtil
import com.workguard.patrol.model.PatrolPoint
import com.workguard.patrol.model.PatrolScanResult
import java.io.File
import java.io.IOException
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class PatrolRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val faceSessionStore: FaceSessionStore,
    private val locationProvider: LocationProvider
) : PatrolRepository {
    companion object {
        private const val TAG = "PatrolRepository"
        private const val CAMERA_SOURCE = "live"
        private val TEXT_PLAIN = "text/plain".toMediaType()
    }

    override suspend fun startSession(faceScore: Double?): ApiResult<Long> {
        return try {
            val request = PatrolSessionStartRequest(faceScore = faceScore)
            val response = apiService.startPatrolSession(request)
            if (response.success == false) {
                ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal memulai sesi patroli")
                )
            } else {
                val data = response.data
                    ?: return ApiResult.Error(IllegalStateException("Respons sesi patroli kosong"))
                val sessionId = data.patrolSessionId ?: data.sessionId ?: data.id
                    ?: return ApiResult.Error(IllegalStateException("Sesi patroli tidak valid"))
                ApiResult.Success(sessionId)
            }
        } catch (e: HttpException) {
            val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val serverMessage = extractServerMessage(errorBody)
            Log.w(TAG, "Start patrol session error: code=${e.code()} body=$errorBody")
            val message = serverMessage ?: "Gagal memulai sesi patroli (${e.code()})"
            ApiResult.Error(IllegalStateException(message))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun getPatrolPoints(): ApiResult<List<PatrolPoint>> {
        return try {
            val response = apiService.getPatrolPoints()
            if (response.success == false) {
                ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal memuat titik patroli")
                )
            } else {
                val data = response.data
                    ?: return ApiResult.Error(IllegalStateException("Titik patroli kosong"))
                ApiResult.Success(mapPoints(data))
            }
        } catch (e: HttpException) {
            val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val serverMessage = extractServerMessage(errorBody)
            Log.w(TAG, "Get patrol points error: code=${e.code()} body=$errorBody")
            val message = serverMessage ?: "Gagal memuat titik patroli (${e.code()})"
            ApiResult.Error(IllegalStateException(message))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun uploadPatrolMedia(
        taskId: String,
        photo: File,
        cameraFacing: CameraFacing
    ): ApiResult<String> {
        if (cameraFacing != CameraFacing.BACK) {
            return ApiResult.Error(IllegalStateException("camera_facing harus rear"))
        }
        if (!photo.exists() || photo.length() <= 0L) {
            return ApiResult.Error(IllegalStateException("Foto belum tersedia"))
        }
        val faceSession = faceSessionStore.getSession()
            ?: return ApiResult.Error(IllegalStateException("Face session belum tersedia"))
        if (faceSession.context != FaceContext.TASK) {
            return ApiResult.Error(IllegalStateException("Face session tidak sesuai konteks"))
        }
        val location = locationProvider.getLastKnownLocation()
            ?: return ApiResult.Error(IllegalStateException("Lokasi belum tersedia"))
        val fileName = FileUtil.sanitizeFileName(photo.name.ifBlank { "patrol_point.jpg" })
        val photoPart = MultipartBody.Part.createFormData(
            "photo",
            fileName,
            photo.asRequestBody(resolveMimeType(photo))
        )
        val cameraFacingValue = if (cameraFacing == CameraFacing.BACK) "rear" else "front"
        return try {
            val response = apiService.uploadTaskMedia(
                taskId = taskId,
                photo = photoPart,
                faceSessionId = faceSession.sessionId.toString().toRequestBody(TEXT_PLAIN),
                latitude = location.latitude.toString().toRequestBody(TEXT_PLAIN),
                longitude = location.longitude.toString().toRequestBody(TEXT_PLAIN),
                accuracy = location.accuracyMeters?.toString()?.toRequestBody(TEXT_PLAIN),
                cameraSource = CAMERA_SOURCE.toRequestBody(TEXT_PLAIN),
                cameraFacing = cameraFacingValue.toRequestBody(TEXT_PLAIN),
                isMockLocation = location.isMocked.toString().toRequestBody(TEXT_PLAIN)
            )
            if (response.success == false) {
                ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal upload foto patroli")
                )
            } else {
                val data = response.data
                    ?: return ApiResult.Error(IllegalStateException("Respons upload kosong"))
                val url = data.photoUrl
                    ?: return ApiResult.Error(IllegalStateException("URL foto tidak tersedia"))
                ApiResult.Success(url)
            }
        } catch (e: HttpException) {
            val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val serverMessage = extractServerMessage(errorBody)
            Log.w(
                TAG,
                "Upload patrol media error: code=${e.code()} body=$errorBody taskId=$taskId"
            )
            val message = serverMessage ?: "Gagal upload foto patroli (${e.code()})"
            ApiResult.Error(IllegalStateException(message))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun scanPatrolPoint(
        patrolPointId: Int,
        patrolSessionId: Long,
        photoUrl: String
    ): ApiResult<PatrolScanResult> {
        val location = locationProvider.getLastKnownLocation()
            ?: return ApiResult.Error(IllegalStateException("Lokasi belum tersedia"))
        val request = PatrolScanRequest(
            patrolPointId = patrolPointId,
            patrolSessionId = patrolSessionId,
            latitude = location.latitude,
            longitude = location.longitude,
            photoUrl = photoUrl
        )
        return try {
            val response = apiService.scanPatrolPoint(request)
            if (response.success == false) {
                ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal scan titik patroli")
                )
            } else {
                val data = response.data
                ApiResult.Success(
                    PatrolScanResult(
                        remainingPoints = data?.remainingPoints,
                        sessionComplete = data?.sessionComplete == true
                    )
                )
            }
        } catch (e: HttpException) {
            val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val serverMessage = extractServerMessage(errorBody)
            Log.w(TAG, "Scan patrol point error: code=${e.code()} body=$errorBody")
            val message = serverMessage ?: "Gagal scan titik patroli (${e.code()})"
            ApiResult.Error(IllegalStateException(message))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    private fun mapPoints(items: List<com.workguard.core.network.PatrolPointResponse>): List<PatrolPoint> {
        if (items.isEmpty()) return emptyList()
        return items.mapIndexed { index, item ->
            PatrolPoint(
                id = item.id ?: index + 1,
                name = item.name
                    ?: item.title
                    ?: item.code
                    ?: "Titik ${index + 1}",
                description = item.description,
                latitude = item.latitude ?: item.lat,
                longitude = item.longitude ?: item.lng,
                radiusMeters = item.radiusM ?: item.radius
            )
        }
    }

    private fun resolveMimeType(file: File): okhttp3.MediaType {
        val ext = file.extension.lowercase()
        return when (ext) {
            "png" -> "image/png".toMediaType()
            "webp" -> "image/webp".toMediaType()
            else -> "image/jpeg".toMediaType()
        }
    }

    private fun extractServerMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val match = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body) ?: return null
        return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
    }
}
