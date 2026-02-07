package com.workguard.attendance.data

import com.workguard.core.model.Attendance
import com.workguard.core.network.ApiResult
import com.workguard.core.network.AttendanceRulesResponse
import com.workguard.core.network.AttendanceTodayResponse

interface AttendanceRepository {
    suspend fun checkIn(): ApiResult<Attendance>
    suspend fun checkOut(): ApiResult<Attendance>
    suspend fun getTodayStatus(date: String? = null): ApiResult<AttendanceTodayResponse>
    suspend fun getRules(date: String? = null): ApiResult<AttendanceRulesResponse>
}
