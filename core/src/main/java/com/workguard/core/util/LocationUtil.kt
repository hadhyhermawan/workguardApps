package com.workguard.core.util

object LocationUtil {
    fun isAccuracyAcceptable(accuracyMeters: Float, maxAccuracyMeters: Float): Boolean {
        return accuracyMeters <= maxAccuracyMeters
    }
}
