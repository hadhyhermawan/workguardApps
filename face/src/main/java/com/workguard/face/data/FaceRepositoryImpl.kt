package com.workguard.face.data

import android.content.Context
import android.util.Log
import com.workguard.core.location.LocationProvider
import com.workguard.core.model.FaceSession
import com.workguard.core.model.enums.FaceContext
import com.workguard.core.network.ApiResult
import com.workguard.core.network.ApiService
import com.workguard.core.network.FaceTemplate
import com.workguard.core.network.FaceTemplateStatus
import com.workguard.core.security.DeviceInfoProvider
import com.workguard.core.util.AppInfoUtil
import com.workguard.core.util.BatteryStatusProvider
import com.workguard.core.util.FileUtil
import com.workguard.core.util.IsoTimeUtil
import java.io.File
import java.io.IOException
import javax.inject.Inject
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import dagger.hilt.android.qualifiers.ApplicationContext

class FaceRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val locationProvider: LocationProvider
) : FaceRepository {
    companion object {
        private const val TAG = "FaceRepository"
        private const val CAMERA_SOURCE = "live"
        private const val CAMERA_FACING = "front"
        private val TEXT_PLAIN: MediaType = "text/plain".toMediaType()
    }

    override suspend fun createFaceSession(
        faceContext: FaceContext,
        photo: File,
        issuedAtMillis: Long,
        expiresAtMillis: Long
    ): ApiResult<FaceSession> {
        if (!photo.exists() || photo.length() <= 0L) {
            return ApiResult.Error(IllegalStateException("Foto belum tersedia"))
        }
        val fileName = FileUtil.sanitizeFileName(photo.name.ifBlank { "face_scan.jpg" })
        val mimeType = resolveMimeType(photo)
        val photoPart = MultipartBody.Part.createFormData(
            "photo",
            fileName,
            photo.asRequestBody(mimeType)
        )
        val contextPart = faceContext.name.toRequestBody(TEXT_PLAIN)
        val sourcePart = CAMERA_SOURCE.toRequestBody(TEXT_PLAIN)
        val facingPart = CAMERA_FACING.toRequestBody(TEXT_PLAIN)
        val (appVersion, appVersionCode) = AppInfoUtil.resolveAppVersionInfo(this.context)
        val batteryStatus = BatteryStatusProvider.getStatus(this.context)
        val locationSnapshot = runCatching { locationProvider.getLastKnownLocation() }.getOrNull()

        return try {
            val response = apiService.verifyFace(
                photo = photoPart,
                context = contextPart,
                cameraSource = sourcePart,
                cameraFacing = facingPart,
                deviceModel = deviceInfoProvider.deviceModel().toTextBody(),
                deviceManufacturer = deviceInfoProvider.deviceManufacturer().toTextBody(),
                appVersion = appVersion.toTextBody(),
                appVersionCode = appVersionCode?.toString()?.toTextBody(),
                batteryLevel = batteryStatus?.level?.toString()?.toTextBody(),
                isMockLocation = locationSnapshot?.isMocked?.toString()?.toTextBody()
            )
            if (response.success == false) {
                Log.w(TAG, "Face verify failed: ${response.message} context=$faceContext")
                return ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal verifikasi wajah")
                )
            }
            val data = response.data
                ?: return ApiResult.Error(IllegalStateException("Respons verifikasi kosong"))
            val sessionId = data.faceSessionId ?: data.sessionId
                ?: return ApiResult.Error(IllegalStateException("Face session ID tidak valid"))
            val issuedAt = IsoTimeUtil.parseMillis(data.issuedAt ?: data.verifiedAt) ?: issuedAtMillis
            val expiresAt = IsoTimeUtil.parseMillis(data.expiresAt) ?: expiresAtMillis
            ApiResult.Success(
                FaceSession(
                    sessionId = sessionId,
                    context = faceContext,
                    issuedAtMillis = issuedAt,
                    expiresAtMillis = expiresAt,
                    matchScore = data.matchScore ?: data.faceScore
                )
            )
        } catch (e: HttpException) {
            val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val serverMessage = extractServerMessage(errorBody)
            Log.w(
                TAG,
                "Face verify http error: code=${e.code()} body=$errorBody context=$faceContext"
            )
            val message = serverMessage ?: "Gagal verifikasi wajah (${e.code()})"
            ApiResult.Error(IllegalStateException(message))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun registerFaceTemplate(
        photo: File,
        slot: Int,
        notes: String?
    ): ApiResult<FaceTemplate> {
        if (slot !in 1..5) {
            return ApiResult.Error(IllegalArgumentException("Slot harus 1-5"))
        }
        if (!photo.exists() || photo.length() <= 0L) {
            return ApiResult.Error(IllegalStateException("Foto belum tersedia"))
        }

        val fileName = FileUtil.sanitizeFileName(photo.name.ifBlank { "face_${slot}.jpg" })
        val mimeType = resolveMimeType(photo)
        val photoPart = MultipartBody.Part.createFormData(
            "photo",
            fileName,
            photo.asRequestBody(mimeType)
        )
        val slotPart = slot.toString().toRequestBody(TEXT_PLAIN)
        val sourcePart = CAMERA_SOURCE.toRequestBody(TEXT_PLAIN)
        val facingPart = CAMERA_FACING.toRequestBody(TEXT_PLAIN)
        val notesPart = notes?.takeIf { it.isNotBlank() }?.toRequestBody(TEXT_PLAIN)

        return try {
            val response = apiService.createFaceTemplate(
                photo = photoPart,
                slot = slotPart,
                cameraSource = sourcePart,
                cameraFacing = facingPart,
                notes = notesPart
            )
            if (response.success == false) {
                ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal menyimpan master wajah")
                )
            } else {
                val data = response.data
                if (data == null) {
                    ApiResult.Error(IllegalStateException("Respons master wajah kosong"))
                } else {
                    ApiResult.Success(data)
                }
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal menyimpan master wajah (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun getFaceTemplateStatus(): ApiResult<FaceTemplateStatus> {
        return try {
            val response = apiService.getFaceTemplateStatus()
            if (response.success == false) {
                ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal memuat status master wajah")
                )
            } else {
                val data = response.data
                if (data == null) {
                    ApiResult.Error(IllegalStateException("Status master wajah kosong"))
                } else {
                    ApiResult.Success(data)
                }
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal memuat status master wajah (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    private fun resolveMimeType(file: File): MediaType {
        val ext = file.extension.lowercase()
        return when (ext) {
            "png" -> "image/png".toMediaType()
            "webp" -> "image/webp".toMediaType()
            else -> "image/jpeg".toMediaType()
        }
    }

    private fun String?.toTextBody(): okhttp3.RequestBody? {
        val value = this?.trim().orEmpty()
        return if (value.isBlank() || value.equals("unknown", ignoreCase = true)) {
            null
        } else {
            value.toRequestBody(TEXT_PLAIN)
        }
    }

    private fun extractServerMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val match = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body) ?: return null
        return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
    }
}
