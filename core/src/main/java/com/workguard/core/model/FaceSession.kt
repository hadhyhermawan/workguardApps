package com.workguard.core.model

import com.workguard.core.model.enums.FaceContext

data class FaceSession(
    val sessionId: Long,
    val context: FaceContext,
    val issuedAtMillis: Long,
    val expiresAtMillis: Long,
    val matchScore: Double? = null
) {
    fun isValidFor(required: FaceContext, nowMillis: Long): Boolean {
        return context == required && nowMillis < expiresAtMillis
    }
}
