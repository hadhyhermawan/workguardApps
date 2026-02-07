package com.workguard.patrol.model

data class PatrolPoint(
    val id: Int,
    val name: String,
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Double? = null,
    val isScanned: Boolean = false
)
