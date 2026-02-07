package com.workguard.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workguard.core.datastore.FaceSessionStore
import com.workguard.core.model.enums.CameraFacing
import com.workguard.core.model.enums.FaceContext
import com.workguard.core.network.ApiResult
import com.workguard.core.security.CameraValidator
import com.workguard.core.security.GpsReading
import com.workguard.core.security.GpsValidator
import com.workguard.core.security.ValidationResult
import com.workguard.core.util.Clock
import com.workguard.task.domain.CompleteTaskUseCase
import com.workguard.task.domain.StartTaskUseCase
import com.workguard.task.domain.UploadTaskMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val startTaskUseCase: StartTaskUseCase,
    private val uploadTaskMediaUseCase: UploadTaskMediaUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val faceSessionStore: FaceSessionStore,
    private val clock: Clock,
    private val cameraValidator: CameraValidator,
    private val gpsValidator: GpsValidator
) : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<TaskEvent>()
    val events = _events.asSharedFlow()

    private var pendingAction: TaskAction? = null
    private var pendingGps: GpsReading? = null
    private var pendingCameraFacing: CameraFacing? = null

    fun onStartTaskRequested(gpsReading: GpsReading?) {
        pendingGps = gpsReading
        if (!hasValidFaceSession()) {
            pendingAction = TaskAction.START
            requestFaceScan()
            return
        }
        val gpsResult = gpsValidator.validate(gpsReading)
        if (gpsResult is ValidationResult.Invalid) {
            _state.update { it.copy(errorMessage = gpsResult.reason) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = startTaskUseCase()) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            taskId = result.data.id,
                            currentStep = TaskStep.CAMERA
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.throwable.message) }
                }
            }
        }
    }

    fun onUploadMediaRequested(cameraFacing: CameraFacing, gpsReading: GpsReading?) {
        pendingCameraFacing = cameraFacing
        pendingGps = gpsReading
        if (!hasValidFaceSession()) {
            pendingAction = TaskAction.UPLOAD_MEDIA
            requestFaceScan()
            return
        }

        val cameraResult = cameraValidator.requireFacing(CameraFacing.BACK, cameraFacing)
        if (cameraResult is ValidationResult.Invalid) {
            _state.update { it.copy(errorMessage = cameraResult.reason) }
            return
        }

        val gpsResult = gpsValidator.validate(gpsReading)
        if (gpsResult is ValidationResult.Invalid) {
            _state.update { it.copy(errorMessage = gpsResult.reason) }
            return
        }

        val taskId = state.value.taskId
        if (taskId.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "Task not started") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = uploadTaskMediaUseCase(taskId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isLoading = false, currentStep = TaskStep.COMPLETE) }
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.throwable.message) }
                }
            }
        }
    }

    fun onCompleteTaskRequested(gpsReading: GpsReading?) {
        pendingGps = gpsReading
        if (!hasValidFaceSession()) {
            pendingAction = TaskAction.COMPLETE
            requestFaceScan()
            return
        }

        val gpsResult = gpsValidator.validate(gpsReading)
        if (gpsResult is ValidationResult.Invalid) {
            _state.update { it.copy(errorMessage = gpsResult.reason) }
            return
        }

        val taskId = state.value.taskId
        if (taskId.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "Task not started") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = completeTaskUseCase(taskId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isLoading = false, currentStep = TaskStep.DONE) }
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.throwable.message) }
                }
            }
        }
    }

    fun onFaceScanCompleted() {
        val action = pendingAction ?: return
        pendingAction = null
        when (action) {
            TaskAction.START -> onStartTaskRequested(pendingGps)
            TaskAction.UPLOAD_MEDIA -> onUploadMediaRequested(pendingCameraFacing ?: CameraFacing.BACK, pendingGps)
            TaskAction.COMPLETE -> onCompleteTaskRequested(pendingGps)
        }
    }

    private fun requestFaceScan() {
        viewModelScope.launch {
            _events.emit(TaskEvent.RequireFaceScan(FaceContext.TASK))
        }
    }

    private fun hasValidFaceSession(): Boolean {
        val session = faceSessionStore.getSession() ?: return false
        return session.isValidFor(FaceContext.TASK, clock.nowMillis())
    }
}

enum class TaskAction {
    START,
    UPLOAD_MEDIA,
    COMPLETE
}

sealed interface TaskEvent {
    data class RequireFaceScan(val context: FaceContext) : TaskEvent
}
