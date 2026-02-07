package com.workguard.attendance

import com.workguard.core.model.enums.AttendanceStatus

data class AttendanceState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastStatus: AttendanceStatus = AttendanceStatus.UNKNOWN,
    val pendingAction: AttendanceAction? = null,
    val activeAction: AttendanceAction? = null,
    val todayStatus: String? = null,
    val checkInAt: String? = null,
    val checkOutAt: String? = null,
    val checkInPhotoUrl: String? = null,
    val checkOutPhotoUrl: String? = null,
    val canCheckIn: Boolean = true,
    val canCheckOut: Boolean = false,
    val shiftStart: String? = null,
    val shiftEnd: String? = null,
    val shiftName: String? = null,
    val isLoadingStatus: Boolean = false
)

enum class AttendanceAction {
    CHECK_IN,
    CHECK_OUT
}
