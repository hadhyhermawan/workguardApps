package com.workguard.attendance.domain

import com.workguard.attendance.data.AttendanceRepository
import com.workguard.core.network.ApiResult
import com.workguard.core.network.AttendanceTodayResponse
import javax.inject.Inject

class GetAttendanceTodayUseCase @Inject constructor(
    private val repository: AttendanceRepository
) {
    suspend operator fun invoke(date: String? = null): ApiResult<AttendanceTodayResponse> {
        return repository.getTodayStatus(date)
    }
}
