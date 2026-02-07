package com.workguard.profile.data

import com.workguard.core.network.ApiResult
import com.workguard.core.network.ApiService
import com.workguard.core.network.EmployeeProfile
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException

class ProfileRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ProfileRepository {
    override suspend fun getProfile(): ApiResult<EmployeeProfile> {
        return try {
            val response = apiService.getEmployeeProfile()
            if (response.success == false) {
                ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal memuat profil")
                )
            } else {
                val data = response.data
                if (data == null) {
                    ApiResult.Error(IllegalStateException("Profil tidak tersedia"))
                } else {
                    ApiResult.Success(data)
                }
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal memuat profil (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }
}
