package com.workguard.face

data class FaceEnrollmentState(
    val currentSlot: Int = 1,
    val totalSlots: Int = 5,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isCompleted: Boolean = false
) {
    val progress: Float
        get() = if (isCompleted) {
            1f
        } else {
            ((currentSlot - 1).coerceAtLeast(0)).toFloat() / totalSlots.toFloat()
        }
}
