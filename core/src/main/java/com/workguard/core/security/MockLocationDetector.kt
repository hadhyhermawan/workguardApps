package com.workguard.core.security

import javax.inject.Inject

class MockLocationDetector @Inject constructor() {
    fun isMocked(reading: GpsReading?): Boolean = reading?.isMocked == true
}
