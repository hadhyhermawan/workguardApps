package com.workguard.core.network

import com.squareup.moshi.Json

data class TaskCreateRequest(
    @Json(name = "task_type")
    val taskType: String,
    val title: String? = null,
    @Json(name = "face_session_id")
    val faceSessionId: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class TaskCompleteRequest(
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class TaskResponse(
    val id: Long? = null,
    @Json(name = "task_type")
    val taskType: String? = null,
    val title: String? = null,
    val status: String? = null
)

data class TaskMediaResponse(
    val id: Long? = null,
    @Json(name = "task_id")
    val taskId: Long? = null,
    @Json(name = "photo_url")
    val photoUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class PatrolSessionStartRequest(
    @Json(name = "face_score")
    val faceScore: Double? = null
)

data class PatrolSessionResponse(
    val id: Long? = null,
    @Json(name = "session_id")
    val sessionId: Long? = null,
    @Json(name = "patrol_session_id")
    val patrolSessionId: Long? = null
)

data class PatrolPointResponse(
    val id: Int? = null,
    val name: String? = null,
    val title: String? = null,
    val code: String? = null,
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "lat")
    val lat: Double? = null,
    @Json(name = "lng")
    val lng: Double? = null,
    val radius: Double? = null,
    @Json(name = "radius_m")
    val radiusM: Double? = null
)

data class PatrolScanRequest(
    @Json(name = "patrol_point_id")
    val patrolPointId: Int,
    @Json(name = "patrol_session_id")
    val patrolSessionId: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "photo_url")
    val photoUrl: String? = null
)

data class PatrolScanResponse(
    val id: Long? = null,
    val status: String? = null,
    @Json(name = "patrol_point_id")
    val patrolPointId: Int? = null,
    @Json(name = "remaining_points")
    val remainingPoints: Int? = null,
    @Json(name = "session_complete")
    val sessionComplete: Boolean? = null
)
