package com.workguard.patrol

import com.workguard.patrol.model.PatrolPoint

data class PatrolState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val taskId: String? = null,
    val patrolSessionId: Long? = null,
    val points: List<PatrolPoint> = emptyList(),
    val selectedPoint: PatrolPoint? = null,
    val completedSessions: Int = 0,
    val maxSessionsPerShift: Int = 4,
    val remainingPoints: Int? = null,
    val sessionComplete: Boolean = false,
    val shiftInfo: String? = null
)

enum class PatrolAction {
    START
}
