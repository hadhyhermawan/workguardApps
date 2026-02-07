package com.workguard.core.model

import com.workguard.core.model.enums.AttendanceStatus

data class Attendance(
    val id: String,
    val status: AttendanceStatus,
    val timestampMillis: Long
)
