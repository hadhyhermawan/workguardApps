package com.workguard.home

data class WorkScheduleDay(
    val date: String,
    val shiftName: String? = null,
    val shiftStart: String? = null,
    val shiftEnd: String? = null,
    val reason: String? = null
)

data class WorkScheduleMonthState(
    val year: Int = 0,
    val month: Int = 0, // 1-12
    val days: List<WorkScheduleDay> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

