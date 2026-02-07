package com.workguard.core.network

import com.squareup.moshi.Json

data class EmployeeDepartment(
    val id: Int? = null,
    val name: String? = null,
    val code: String? = null
)

data class EmployeePosition(
    val id: Int? = null,
    val name: String? = null,
    val code: String? = null
)

data class EmployeeBranch(
    val id: Int? = null,
    val name: String? = null,
    val code: String? = null,
    val address: String? = null,
    val city: String? = null,
    val phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radius: Int? = null
)

data class EmployeeProfile(
    val id: Int? = null,
    @Json(name = "employee_code")
    val employeeCode: String? = null,
    @Json(name = "full_name")
    val fullName: String? = null,
    @Json(name = "employee_photo_url")
    val employeePhotoUrl: String? = null,
    @Json(name = "user_photo_url")
    val userPhotoUrl: String? = null,
    val role: String? = null,
    @Json(name = "task_type")
    val taskType: String? = null,
    @Json(name = "employment_status")
    val employmentStatus: String? = null,
    @Json(name = "join_date")
    val joinDate: String? = null,
    @Json(name = "birth_place")
    val birthPlace: String? = null,
    @Json(name = "birth_date")
    val birthDate: String? = null,
    val gender: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @Json(name = "lock_location")
    val lockLocation: Boolean? = null,
    @Json(name = "lock_work_hours")
    val lockWorkHours: Boolean? = null,
    @Json(name = "lock_device_login")
    val lockDeviceLogin: Boolean? = null,
    val department: EmployeeDepartment? = null,
    val position: EmployeePosition? = null,
    val branch: EmployeeBranch? = null,
    @Json(name = "company_id")
    val companyId: Int? = null,
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null
)
