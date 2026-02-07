package com.workguard.tracking.data

import com.workguard.core.network.ApiResult
import com.workguard.core.network.MonitoringPoint
import com.workguard.core.network.MonitoringViolation

interface MonitoringRepository {
    suspend fun getLatest(): ApiResult<MonitoringPoint>

    suspend fun getHistory(
        taskId: Int? = null,
        startDate: String? = null,
        endDate: String? = null,
        days: Int? = null,
        limit: Int? = null
    ): ApiResult<List<MonitoringPoint>>

    suspend fun getViolations(
        type: String? = null,
        resolved: Boolean? = null,
        startDate: String? = null,
        endDate: String? = null,
        limit: Int? = null
    ): ApiResult<List<MonitoringViolation>>
}
