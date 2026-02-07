package com.workguard.home

import com.workguard.home.data.HomeActivityItem
import com.workguard.home.data.HomeQuickStats
import com.workguard.home.data.HomeTaskItem
import com.workguard.home.data.HomeTaskSummary

data class HomeState(
    val displayName: String = "",
    val role: String? = null,
    val photoUrl: String? = null,
    val companyName: String? = null,
    val companyLogoUrl: String? = null,
    val employeeId: Int? = null,
    val companyId: Int? = null,
    val todayTaskSummary: HomeTaskSummary = HomeTaskSummary(),
    val todayTasks: List<HomeTaskItem> = emptyList(),
    val recentActivities: List<HomeActivityItem> = emptyList(),
    val quickStats: HomeQuickStats = HomeQuickStats(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
