package com.workguard.home.data

import com.workguard.core.location.LocationSnapshot
import com.workguard.core.network.ApiResult
import com.workguard.core.network.TrackingPingResponse

interface HomeRepository {
    suspend fun loadHome(companyCode: String? = null): ApiResult<HomeSummary>
    suspend fun sendTrackingPing(location: LocationSnapshot?): ApiResult<TrackingPingResponse>
}

data class HomeSummary(
    val displayName: String,
    val role: String?,
    val photoUrl: String?,
    val employeeId: Int?,
    val companyId: Int?,
    val companyName: String?,
    val companyLogoUrl: String?,
    val todayTaskSummary: HomeTaskSummary = HomeTaskSummary(),
    val todayTasks: List<HomeTaskItem> = emptyList(),
    val recentActivities: List<HomeActivityItem> = emptyList(),
    val quickStats: HomeQuickStats = HomeQuickStats(),
    val errorMessage: String? = null
)

data class HomeTaskSummary(
    val started: Int = 0,
    val completed: Int = 0,
    val cancelled: Int = 0
)

data class HomeTaskItem(
    val id: Int? = null,
    val title: String? = null,
    val date: String? = null,
    val time: String? = null,
    val description: String? = null,
    val status: String? = null
)

data class HomeActivityItem(
    val title: String? = null,
    val date: String? = null,
    val time: String? = null,
    val status: String? = null,
    val statusColor: String? = null,
    val type: String? = null
)

data class HomeQuickStats(
    val attendanceStatus: String? = null,
    val checkInAt: String? = null,
    val checkOutAt: String? = null,
    val pendingPermits: Int = 0,
    val pendingOvertimes: Int = 0,
    val violationsToday: Int = 0
)
