package com.workguard.core.security

import javax.inject.Inject

class GpsValidator @Inject constructor(
    private val mockLocationDetector: MockLocationDetector
) {
    fun validate(reading: GpsReading?, maxAccuracyMeters: Float = 25f): ValidationResult {
        if (reading == null) {
            return ValidationResult.Invalid("GPS unavailable")
        }
        if (mockLocationDetector.isMocked(reading)) {
            return ValidationResult.Invalid("Mock location detected")
        }
        if (reading.accuracyMeters > maxAccuracyMeters) {
            return ValidationResult.Invalid("GPS accuracy too low")
        }
        return ValidationResult.Valid
    }
}
