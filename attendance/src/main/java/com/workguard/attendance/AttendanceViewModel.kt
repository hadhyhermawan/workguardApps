package com.workguard.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workguard.attendance.domain.CheckInUseCase
import com.workguard.attendance.domain.CheckOutUseCase
import com.workguard.attendance.domain.GetAttendanceTodayUseCase
import com.workguard.core.datastore.FaceSessionStore
import com.workguard.core.model.enums.FaceContext
import com.workguard.core.network.ApiResult
import com.workguard.core.util.Clock
import com.workguard.core.util.UrlUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val checkInUseCase: CheckInUseCase,
    private val checkOutUseCase: CheckOutUseCase,
    private val getAttendanceTodayUseCase: GetAttendanceTodayUseCase,
    private val faceSessionStore: FaceSessionStore,
    private val clock: Clock
) : ViewModel() {
    private val _state = MutableStateFlow(AttendanceState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<AttendanceEvent>()
    val events = _events.asSharedFlow()

    init {
        loadTodayStatus()
    }

    fun onCheckInClicked() {
        handleAction(AttendanceAction.CHECK_IN)
    }

    fun onCheckOutClicked() {
        handleAction(AttendanceAction.CHECK_OUT)
    }

    fun onFaceScanCompleted() {
        val pending = state.value.pendingAction ?: return
        _state.update { it.copy(pendingAction = null) }
        handleAction(pending)
    }

    private fun handleAction(action: AttendanceAction) {
        if (!hasValidFaceSession()) {
            _state.update { it.copy(pendingAction = action) }
            viewModelScope.launch {
                _events.emit(AttendanceEvent.RequireFaceScan(FaceContext.ATTENDANCE))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, activeAction = action, errorMessage = null) }
            val result = when (action) {
                AttendanceAction.CHECK_IN -> checkInUseCase()
                AttendanceAction.CHECK_OUT -> checkOutUseCase()
            }
            when (result) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            activeAction = null,
                            lastStatus = result.data.status
                        )
                    }
                    loadTodayStatus()
                }
                is ApiResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            activeAction = null,
                            errorMessage = result.throwable.message
                        )
                    }
                }
            }
        }
    }

    fun loadTodayStatus() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingStatus = true) }
            when (val result = getAttendanceTodayUseCase()) {
                is ApiResult.Success -> {
                    val data = result.data
                    _state.update {
                        it.copy(
                            isLoadingStatus = false,
                            todayStatus = data.status,
                            checkInAt = data.checkInAt,
                            checkOutAt = data.checkOutAt,
                            checkInPhotoUrl = UrlUtil.resolveAssetUrl(data.checkInPhotoUrl),
                            checkOutPhotoUrl = UrlUtil.resolveAssetUrl(data.checkOutPhotoUrl),
                            canCheckIn = data.canCheckIn ?: true,
                            canCheckOut = data.canCheckOut ?: false,
                            shiftStart = data.shiftStart,
                            shiftEnd = data.shiftEnd,
                            shiftName = data.shiftName,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoadingStatus = false,
                            errorMessage = result.throwable.message
                        )
                    }
                }
            }
        }
    }

    private fun hasValidFaceSession(): Boolean {
        val session = faceSessionStore.getSession() ?: return false
        return session.isValidFor(FaceContext.ATTENDANCE, clock.nowMillis())
    }
}

sealed interface AttendanceEvent {
    data class RequireFaceScan(val context: FaceContext) : AttendanceEvent
}
