package com.workguard.face

import com.workguard.core.model.enums.FaceContext

data class FaceScanState(
    val context: FaceContext = FaceContext.ATTENDANCE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isCompleted: Boolean = false
)
