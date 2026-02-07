package com.workguard.core.network

import com.squareup.moshi.Json

data class FaceTemplateMeta(
    @Json(name = "camera_source")
    val cameraSource: String? = null,
    @Json(name = "camera_facing")
    val cameraFacing: String? = null,
    val notes: String? = null
)

data class FaceTemplate(
    val id: Int? = null,
    @Json(name = "employee_id")
    val employeeId: Int? = null,
    @Json(name = "doc_type")
    val docType: String? = null,
    val slot: Int? = null,
    @Json(name = "file_url")
    val fileUrl: String? = null,
    val meta: FaceTemplateMeta? = null,
    @Json(name = "created_at")
    val createdAt: String? = null
)
