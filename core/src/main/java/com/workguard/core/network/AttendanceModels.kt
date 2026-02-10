package com.workguard.core.network

import com.squareup.moshi.Json

data class AttendanceRequest(
    val action: String? = null,
    @Json(name = "face_session_id")
    val faceSessionId: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    @Json(name = "is_mock_location")
    val isMockLocation: Boolean? = null,
    val provider: String? = null,
    @Json(name = "battery_level")
    val batteryLevel: Int? = null,
    @Json(name = "is_charging")
    val isCharging: Boolean? = null,
    @Json(name = "recorded_at")
    val recordedAt: String? = null,
    @Json(name = "device_model")
    val deviceModel: String? = null,
    @Json(name = "device_manufacturer")
    val deviceManufacturer: String? = null,
    @Json(name = "app_version")
    val appVersion: String? = null,
    @Json(name = "app_version_code")
    val appVersionCode: Long? = null
)

data class AttendanceResponse(
    @Json(name = "attendance_id")
    val attendanceId: Long? = null,
    val status: String? = null,
    @Json(name = "check_in_at")
    val checkInAt: String? = null,
    @Json(name = "check_out_at")
    val checkOutAt: String? = null,
    val message: String? = null
)

data class AttendanceTodayResponse(
    val status: String? = null,
    @Json(name = "check_in_at")
    val checkInAt: String? = null,
    @Json(name = "check_out_at")
    val checkOutAt: String? = null,
    @Json(name = "check_in_photo_url")
    val checkInPhotoUrl: String? = null,
    @Json(name = "check_out_photo_url")
    val checkOutPhotoUrl: String? = null,
    @Json(name = "can_check_in")
    val canCheckIn: Boolean? = null,
    @Json(name = "can_check_out")
    val canCheckOut: Boolean? = null,
    val reason: String? = null,
    @Json(name = "shift_start")
    val shiftStart: String? = null,
    @Json(name = "shift_end")
    val shiftEnd: String? = null,
    @Json(name = "shift_name")
    val shiftName: String? = null
)

data class AttendanceHistoryItem(
    val date: String,
    @Json(name = "shift_start")
    val shiftStart: String? = null,
    @Json(name = "shift_end")
    val shiftEnd: String? = null,
    @Json(name = "shift_name")
    val shiftName: String? = null,
    @Json(name = "check_in_at")
    val checkInAt: String? = null,
    @Json(name = "check_out_at")
    val checkOutAt: String? = null,
    val reason: String? = null
)

data class AttendanceRulesResponse(
    @Json(name = "allowed_radius_m")
    val allowedRadiusM: Int? = null,
    @Json(name = "center_lat")
    val centerLat: Double? = null,
    @Json(name = "center_lng")
    val centerLng: Double? = null,
    @Json(name = "shift_start")
    val shiftStart: String? = null,
    @Json(name = "shift_end")
    val shiftEnd: String? = null,
    @Json(name = "require_face_scan")
    val requireFaceScan: Boolean? = null
)
