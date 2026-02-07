package com.workguard.core.network

import com.squareup.moshi.Json

data class FaceVerifyResponse(
    @Json(name = "session_id")
    val sessionId: Long? = null,
    @Json(name = "face_session_id")
    val faceSessionId: Long? = null,
    val context: String? = null,
    @Json(name = "face_score")
    val faceScore: Double? = null,
    @Json(name = "issued_at")
    val issuedAt: String? = null,
    @Json(name = "verified_at")
    val verifiedAt: String? = null,
    @Json(name = "expires_at")
    val expiresAt: String? = null,
    @Json(name = "match_score")
    val matchScore: Double? = null
)
