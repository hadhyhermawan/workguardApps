package com.workguard.core.network

data class EmployeeHomeResponse(
    val displayName: String? = null,
    val role: String? = null,
    val photoUrl: String? = null,
    val employeeId: Int? = null,
    val companyId: Int? = null,
    val companyName: String? = null,
    val companyLogoUrl: String? = null,
    val todayTask: HomeTaskSectionResponse? = null,
    val recentActivity: HomeRecentActivityResponse? = null,
    val quickStats: HomeQuickStatsResponse? = null,
    val errorMessage: String? = null
)

data class HomeTaskSectionResponse(
    val summary: HomeTaskSummaryResponse? = null,
    val items: List<HomeTaskItemResponse>? = null
)

data class HomeTaskSummaryResponse(
    val started: Int? = null,
    val completed: Int? = null,
    val cancelled: Int? = null
)

data class HomeTaskItemResponse(
    val id: Int? = null,
    val title: String? = null,
    val date: String? = null,
    val time: String? = null,
    val description: String? = null,
    val status: String? = null
)

data class HomeRecentActivityResponse(
    val items: List<HomeActivityItemResponse>? = null
)

data class HomeActivityItemResponse(
    val title: String? = null,
    val date: String? = null,
    val time: String? = null,
    val status: String? = null,
    val statusColor: String? = null,
    val type: String? = null
)

data class HomeQuickStatsResponse(
    val attendanceStatus: String? = null,
    val checkInAt: String? = null,
    val checkOutAt: String? = null,
    val pendingPermits: Int? = null,
    val pendingOvertimes: Int? = null,
    val violationsToday: Int? = null
)
