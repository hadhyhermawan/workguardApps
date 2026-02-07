package com.workguard.core.network

import com.squareup.moshi.Json

data class FaceTemplateSlotStatus(
    val slot: Int? = null,
    @Json(name = "doc_type")
    val docType: String? = null,
    @Json(name = "is_registered")
    val isRegistered: Boolean? = null,
    @Json(name = "file_url")
    val fileUrl: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null
)

data class FaceTemplateStatus(
    @Json(name = "employee_id")
    val employeeId: Int? = null,
    @Json(name = "total_slots")
    val totalSlots: Int? = null,
    @Json(name = "registered_count")
    val registeredCount: Int? = null,
    @Json(name = "is_complete")
    val isComplete: Boolean? = null,
    val slots: List<FaceTemplateSlotStatus>? = null
)
