package com.workguard.core.security

import com.workguard.core.model.enums.CameraFacing
import javax.inject.Inject

class CameraValidator @Inject constructor() {
    fun requireFacing(expected: CameraFacing, actual: CameraFacing): ValidationResult {
        return if (expected == actual) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Camera must be $expected")
        }
    }
}
