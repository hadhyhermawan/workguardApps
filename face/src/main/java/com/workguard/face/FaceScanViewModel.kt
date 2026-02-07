package com.workguard.face

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workguard.core.model.enums.CameraFacing
import com.workguard.core.model.enums.FaceContext
import com.workguard.core.network.ApiResult
import com.workguard.core.security.CameraValidator
import com.workguard.core.security.ValidationResult
import com.workguard.face.domain.CreateFaceSessionUseCase
import java.io.File
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FaceScanViewModel @Inject constructor(
    private val createFaceSessionUseCase: CreateFaceSessionUseCase,
    private val cameraValidator: CameraValidator
) : ViewModel() {
    private val _state = MutableStateFlow(FaceScanState())
    val state = _state.asStateFlow()

    fun setContext(context: FaceContext) {
        _state.update { it.copy(context = context, errorMessage = null) }
    }

    fun onConfirmScan(cameraFacing: CameraFacing, photo: File) {
        val validation = cameraValidator.requireFacing(CameraFacing.FRONT, cameraFacing)
        if (validation is ValidationResult.Invalid) {
            _state.update { it.copy(errorMessage = validation.reason) }
            return
        }
        if (!photo.exists() || photo.length() <= 0L) {
            _state.update { it.copy(errorMessage = "Foto belum tersedia") }
            return
        }

        val context = state.value.context
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = createFaceSessionUseCase(context, photo)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isLoading = false, isCompleted = true) }
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.throwable.message) }
                }
            }
        }
    }
}
