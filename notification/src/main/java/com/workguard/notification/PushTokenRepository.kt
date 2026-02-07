package com.workguard.notification

import com.workguard.core.network.ApiResult
import com.workguard.core.network.ApiService
import com.workguard.core.network.PushTokenRequest
import com.workguard.core.network.PushTokenResponse
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException

class PushTokenRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun registerToken(token: String, deviceId: String?): ApiResult<PushTokenResponse> {
        return try {
            val response = apiService.registerPushToken(
                PushTokenRequest(
                    fcmToken = token,
                    deviceId = deviceId,
                    platform = "android"
                )
            )
            if (response.success == false) {
                ApiResult.Error(IllegalStateException(response.message ?: "Gagal daftar token"))
            } else {
                ApiResult.Success(response.data ?: PushTokenResponse())
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal daftar token (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }
}
