package com.workguard.tracking.data

import com.workguard.core.network.ApiResult
import com.workguard.core.network.ApiService
import com.workguard.core.network.MonitoringPoint
import com.workguard.core.network.MonitoringViolation
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException

class MonitoringRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MonitoringRepository {
    override suspend fun getLatest(): ApiResult<MonitoringPoint> {
        return try {
            val response = apiService.getMonitoringLatest()
            if (response.success == false) {
                ApiResult.Error(IllegalStateException(response.message ?: "Gagal mengambil lokasi terbaru"))
            } else {
                val data = response.data
                if (data == null) {
                    ApiResult.Error(IllegalStateException("Lokasi terbaru belum tersedia"))
                } else {
                    ApiResult.Success(data)
                }
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal mengambil lokasi terbaru (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun getHistory(
        taskId: Int?,
        startDate: String?,
        endDate: String?,
        days: Int?,
        limit: Int?
    ): ApiResult<List<MonitoringPoint>> {
        return try {
            val response = apiService.getMonitoringHistory(
                taskId = taskId,
                startDate = startDate,
                endDate = endDate,
                days = days,
                limit = limit
            )
            if (response.success == false) {
                ApiResult.Error(IllegalStateException(response.message ?: "Gagal mengambil histori lokasi"))
            } else {
                ApiResult.Success(response.data ?: emptyList())
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal mengambil histori lokasi (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun getViolations(
        type: String?,
        resolved: Boolean?,
        startDate: String?,
        endDate: String?,
        limit: Int?
    ): ApiResult<List<MonitoringViolation>> {
        return try {
            val response = apiService.getMonitoringViolations(
                type = type,
                resolved = resolved,
                startDate = startDate,
                endDate = endDate,
                limit = limit
            )
            if (response.success == false) {
                ApiResult.Error(IllegalStateException(response.message ?: "Gagal mengambil pelanggaran tracking"))
            } else {
                ApiResult.Success(response.data ?: emptyList())
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal mengambil pelanggaran tracking (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }
}
