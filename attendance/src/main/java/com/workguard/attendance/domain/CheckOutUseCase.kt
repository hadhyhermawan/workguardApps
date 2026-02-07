package com.workguard.attendance.domain

import com.workguard.attendance.data.AttendanceRepository
import com.workguard.core.model.Attendance
import com.workguard.core.network.ApiResult
import javax.inject.Inject

class CheckOutUseCase @Inject constructor(
    private val repository: AttendanceRepository
) {
    suspend operator fun invoke(): ApiResult<Attendance> {
        return repository.checkOut()
    }
}
