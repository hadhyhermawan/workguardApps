package com.workguard.core.util

import javax.inject.Inject

interface Clock {
    fun nowMillis(): Long
}

class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
