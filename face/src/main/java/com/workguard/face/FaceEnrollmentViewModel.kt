package com.workguard.face

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workguard.core.network.ApiResult
import com.workguard.core.network.FaceTemplateStatus
import com.workguard.face.domain.GetFaceTemplateStatusUseCase
import com.workguard.face.domain.RegisterFaceTemplateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FaceEnrollmentViewModel @Inject constructor(
    private val registerFaceTemplateUseCase: RegisterFaceTemplateUseCase,
    private val getFaceTemplateStatusUseCase: GetFaceTemplateStatusUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(FaceEnrollmentState())
    val state = _state.asStateFlow()

    init {
        refreshStatus()
    }

    fun onPhotoCaptured(photo: File) {
        if (state.value.isCompleted || state.value.isLoading) {
            return
        }
        val slot = state.value.currentSlot
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = registerFaceTemplateUseCase(photo, slot)) {
                is ApiResult.Success -> {
                    photo.delete()
                    val nextSlot = slot + 1
                    val completed = nextSlot > state.value.totalSlots
                    _state.update {
                        it.copy(
                            isLoading = false,
                            currentSlot = if (completed) it.totalSlots else nextSlot,
                            isCompleted = completed,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update {
                        it.copy(isLoading = false, errorMessage = result.throwable.message)
                    }
                }
            }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getFaceTemplateStatusUseCase()) {
                is ApiResult.Success -> {
                    val status = result.data
                    _state.update { current ->
                        applyStatus(current, status)
                    }
                }
                is ApiResult.Error -> {
                    _state.update {
                        it.copy(isLoading = false, errorMessage = result.throwable.message)
                    }
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun applyStatus(
        current: FaceEnrollmentState,
        status: FaceTemplateStatus
    ): FaceEnrollmentState {
        val totalSlots = status.totalSlots ?: current.totalSlots
        val slots = status.slots.orEmpty()
        val registeredCount = status.registeredCount
            ?: slots.count { it.isRegistered == true }
        val isComplete = status.isComplete == true || registeredCount >= totalSlots
        val nextSlot = slots.firstOrNull { it.isRegistered != true }?.slot
            ?: (registeredCount + 1).coerceAtMost(totalSlots)
        return current.copy(
            totalSlots = totalSlots,
            currentSlot = if (isComplete) totalSlots else nextSlot,
            isCompleted = isComplete,
            isLoading = false,
            errorMessage = null
        )
    }
}
