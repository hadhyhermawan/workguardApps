package com.workguard.core.security

import com.workguard.core.model.enums.FaceContext

data class FaceSessionConfig(
    val attendanceTtlMillis: Long,
    val taskTtlMillis: Long
) {
    fun ttlFor(context: FaceContext): Long {
        return when (context) {
            FaceContext.ATTENDANCE -> attendanceTtlMillis
            FaceContext.TASK -> taskTtlMillis
        }
    }
}
