package com.workguard.patrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workguard.attendance.data.AttendanceRepository
import com.workguard.core.datastore.FaceSessionStore
import com.workguard.core.location.LocationProvider
import com.workguard.core.model.enums.CameraFacing
import com.workguard.core.model.enums.FaceContext
import com.workguard.core.network.ApiResult
import com.workguard.core.security.GpsReading
import com.workguard.core.security.GpsValidator
import com.workguard.core.security.ValidationResult
import com.workguard.core.util.Clock
import com.workguard.patrol.data.PatrolRepository
import com.workguard.patrol.model.PatrolPoint
import com.workguard.task.domain.StartTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PatrolViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val startTaskUseCase: StartTaskUseCase,
    private val patrolRepository: PatrolRepository,
    private val patrolSessionStore: com.workguard.patrol.data.PatrolSessionStore,
    private val faceSessionStore: FaceSessionStore,
    private val locationProvider: LocationProvider,
    private val gpsValidator: GpsValidator,
    private val clock: Clock
) : ViewModel() {
    private val _state = MutableStateFlow(PatrolState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<PatrolEvent>()
    val events = _events.asSharedFlow()

    private var pendingAction: PatrolAction? = null
    private var activeShiftKey: String? = null

    init {
        restoreSession()
    }

    fun onStartPatrolRequested() {
        val currentState = state.value
        if (currentState.isLoading) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    statusMessage = null,
                    sessionComplete = false
                )
            }
            val attendanceResult = attendanceRepository.getTodayStatus()
            if (attendanceResult is ApiResult.Error) {
                _state.update {
                    it.copy(isLoading = false, errorMessage = attendanceResult.throwable.message)
                }
                return@launch
            }
            val attendanceData = (attendanceResult as ApiResult.Success).data
            val shiftInfo = buildShiftInfo(attendanceData)
            val shiftKey = buildShiftKey(attendanceData)
            val previousShiftKey = activeShiftKey ?: patrolSessionStore.get()?.shiftKey
            if (shiftKey != null && previousShiftKey != null && shiftKey != previousShiftKey) {
                patrolSessionStore.clear()
                _state.update { PatrolState(isLoading = true, shiftInfo = shiftInfo) }
            } else if (shiftInfo != null) {
                _state.update { it.copy(shiftInfo = shiftInfo) }
            }
            activeShiftKey = shiftKey
            val refreshedState = state.value
            if (refreshedState.patrolSessionId != null && !refreshedState.sessionComplete) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Sesi patroli sebelumnya belum selesai. Silakan lanjutkan sesi lama."
                    )
                }
                return@launch
            }
            val attendanceError = validateAttendance(attendanceData, shiftInfo)
            if (attendanceError != null) {
                _state.update { it.copy(isLoading = false, errorMessage = attendanceError) }
                return@launch
            }
            if (refreshedState.completedSessions >= refreshedState.maxSessionsPerShift) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Batas patroli per shift sudah tercapai."
                    )
                }
                return@launch
            }
            if (!hasValidFaceSession()) {
                pendingAction = PatrolAction.START
                _state.update { it.copy(isLoading = false) }
                requestFaceScan()
                return@launch
            }
            val location = locationProvider.getLastKnownLocation()
            val gpsResult = gpsValidator.validate(location?.toReading())
            if (gpsResult is ValidationResult.Invalid) {
                _state.update { it.copy(isLoading = false, errorMessage = gpsResult.reason) }
                return@launch
            }

            val title = attendanceData.shiftName?.takeIf { it.isNotBlank() }?.let {
                "Patroli $it"
            }
            val existingTaskId = state.value.taskId
            val taskId = if (existingTaskId.isNullOrBlank()) {
                when (val taskResult = startTaskUseCase(title = title)) {
                    is ApiResult.Success -> {
                        val newTaskId = taskResult.data.id
                        _state.update { it.copy(taskId = newTaskId) }
                        saveSession(
                            taskId = newTaskId,
                            patrolSessionId = state.value.patrolSessionId,
                            points = state.value.points,
                            remainingPoints = state.value.remainingPoints,
                            sessionComplete = state.value.sessionComplete
                        )
                        newTaskId
                    }
                    is ApiResult.Error -> {
                        _state.update {
                            it.copy(isLoading = false, errorMessage = taskResult.throwable.message)
                        }
                        return@launch
                    }
                }
            } else {
                existingTaskId
            }

            val faceScore = faceSessionStore.getSession()?.matchScore
            when (val sessionResult = patrolRepository.startSession(faceScore)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            taskId = taskId,
                            patrolSessionId = sessionResult.data,
                            points = emptyList(),
                            remainingPoints = null,
                            sessionComplete = false
                        )
                    }
                    saveSession(
                        taskId = taskId,
                        patrolSessionId = sessionResult.data,
                        points = emptyList(),
                        remainingPoints = null,
                        sessionComplete = false
                    )
                    loadPatrolPoints(resetScans = true, autoSelectFirst = true)
                }
                is ApiResult.Error -> {
                    val message = sessionResult.throwable.message
                    val isLimitReached = message?.contains("batas patroli per shift", ignoreCase = true) == true
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = message,
                            completedSessions = if (isLimitReached) it.maxSessionsPerShift else it.completedSessions
                        )
                    }
                    if (isLimitReached) {
                        saveSession(
                            taskId = taskId,
                            patrolSessionId = null,
                            points = state.value.points,
                            remainingPoints = state.value.remainingPoints,
                            sessionComplete = state.value.sessionComplete
                        )
                    }
                }
            }
        }
    }

    fun onPhotoCaptured(photo: File) {
        val selected = state.value.points.firstOrNull { !it.isScanned } ?: return
        val taskId = state.value.taskId ?: return
        val patrolSessionId = state.value.patrolSessionId
        if (patrolSessionId == null) {
            _state.update { it.copy(errorMessage = "Sesi patroli belum aktif") }
            return
        }
        if (selected.isScanned) {
            _state.update { it.copy(errorMessage = "Titik patroli ini sudah diambil.") }
            return
        }
        val nextPoint = state.value.points.firstOrNull { !it.isScanned }
        if (nextPoint != null && nextPoint.id != selected.id) {
            _state.update { it.copy(errorMessage = "Silakan lakukan patroli sesuai urutan.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val gpsResult = gpsValidator.validate(locationProvider.getLastKnownLocation()?.toReading())
            if (gpsResult is ValidationResult.Invalid) {
                _state.update { it.copy(isLoading = false, errorMessage = gpsResult.reason) }
                return@launch
            }
            val uploadResult = patrolRepository.uploadPatrolMedia(
                taskId = taskId,
                photo = photo,
                cameraFacing = CameraFacing.BACK
            )
            if (uploadResult is ApiResult.Error) {
                _state.update { it.copy(isLoading = false, errorMessage = uploadResult.throwable.message) }
                return@launch
            }
            val photoUrl = (uploadResult as ApiResult.Success).data
            when (val scanResult = patrolRepository.scanPatrolPoint(
                patrolPointId = selected.id,
                patrolSessionId = patrolSessionId,
                photoUrl = photoUrl
            )) {
                is ApiResult.Success -> {
                    photo.delete()
                    val updatedPoints = state.value.points.map { point ->
                        if (point.id == selected.id) point.copy(isScanned = true) else point
                    }
                    val remaining = scanResult.data.remainingPoints
                        ?: updatedPoints.count { !it.isScanned }
                    val sessionComplete = scanResult.data.sessionComplete || remaining == 0
                    val finalRemaining = if (sessionComplete) 0 else remaining
                    val nextPoint = updatedPoints.firstOrNull { !it.isScanned }
                    val newCompletedSessions = if (sessionComplete) {
                        (state.value.completedSessions + 1).coerceAtMost(state.value.maxSessionsPerShift)
                    } else {
                        state.value.completedSessions
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = if (sessionComplete) {
                                "Sesi selesai, kembali ke beranda"
                            } else {
                                "Berhasil scan ${selected.name}"
                            },
                            points = updatedPoints,
                            remainingPoints = finalRemaining,
                            sessionComplete = sessionComplete,
                            patrolSessionId = if (sessionComplete) null else it.patrolSessionId,
                            completedSessions = newCompletedSessions
                        )
                    }
                    val sessionId = if (sessionComplete) null else patrolSessionId
                    saveSession(
                        taskId = taskId,
                        patrolSessionId = sessionId,
                        points = updatedPoints,
                        remainingPoints = finalRemaining,
                        sessionComplete = sessionComplete
                    )
                    if (sessionComplete) {
                        _events.emit(PatrolEvent.Finished)
                    }
                }
                is ApiResult.Error -> {
                    val message = scanResult.throwable.message
                    if (message?.contains("sudah diambil", ignoreCase = true) == true) {
                        photo.delete()
                        val updatedPoints = state.value.points.map { point ->
                            if (point.id == selected.id) point.copy(isScanned = true) else point
                        }
                        val remaining = updatedPoints.count { !it.isScanned }
                        val sessionComplete = remaining == 0
                        val newCompletedSessions = if (sessionComplete) {
                            (state.value.completedSessions + 1).coerceAtMost(state.value.maxSessionsPerShift)
                        } else {
                            state.value.completedSessions
                        }
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = message,
                                points = updatedPoints,
                                remainingPoints = remaining,
                                sessionComplete = sessionComplete,
                                patrolSessionId = if (sessionComplete) null else it.patrolSessionId,
                                completedSessions = newCompletedSessions
                            )
                        }
                        val sessionId = if (sessionComplete) null else patrolSessionId
                        saveSession(
                            taskId = taskId,
                            patrolSessionId = sessionId,
                            points = updatedPoints,
                            remainingPoints = remaining,
                            sessionComplete = sessionComplete
                        )
                    } else {
                        _state.update { it.copy(isLoading = false, errorMessage = message) }
                    }
                }
            }
        }
    }

    fun onCancelCapture() {
        viewModelScope.launch {
            _events.emit(PatrolEvent.Finished)
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null, statusMessage = null) }
    }

    fun onFaceScanCompleted() {
        when (pendingAction) {
            PatrolAction.START -> {
                pendingAction = null
                onStartPatrolRequested()
            }
            else -> Unit
        }
    }

    private fun loadPatrolPoints(resetScans: Boolean = false, autoSelectFirst: Boolean = false) {
        viewModelScope.launch {
            when (val result = patrolRepository.getPatrolPoints()) {
                is ApiResult.Success -> {
                    val scannedLookup = if (resetScans) {
                        emptyMap()
                    } else {
                        state.value.points.associate { it.id to it.isScanned }
                    }
                    val mapped = result.data.map { point ->
                        val scanned = scannedLookup[point.id] == true
                        point.copy(isScanned = scanned)
                    }
                    val remaining = mapped.count { !it.isScanned }
                    if (mapped.isEmpty()) {
                        _state.update {
                            it.copy(
                                points = emptyList(),
                                remainingPoints = 0,
                                errorMessage = "Titik patroli belum tersedia. Silakan hubungi admin untuk set titik."
                            )
                        }
                    } else {
                        _state.update { current ->
                            val nextPoint = if (autoSelectFirst) mapped.firstOrNull { !it.isScanned } else null
                            current.copy(
                                points = mapped,
                                remainingPoints = remaining,
                                statusMessage = current.statusMessage // keep, no selectedPoint field
                            )
                        }
                    }
                    saveSession(
                        taskId = state.value.taskId,
                        patrolSessionId = state.value.patrolSessionId,
                        points = mapped,
                        remainingPoints = remaining,
                        sessionComplete = state.value.sessionComplete
                    )
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(errorMessage = result.throwable.message) }
                }
            }
        }
    }

    private fun restoreSession() {
        val snapshot = patrolSessionStore.get() ?: return
        activeShiftKey = snapshot.shiftKey
        val remaining = snapshot.remainingPoints ?: snapshot.points.count { !it.isScanned }
        _state.update {
            it.copy(
                taskId = snapshot.taskId,
                patrolSessionId = snapshot.patrolSessionId,
                points = snapshot.points,
                completedSessions = snapshot.completedSessions,
                remainingPoints = remaining,
                sessionComplete = snapshot.sessionComplete
            )
        }
        if (snapshot.points.isEmpty() && snapshot.patrolSessionId != null) {
            loadPatrolPoints()
        }
    }

    private fun saveSession(
        taskId: String?,
        patrolSessionId: Long?,
        points: List<PatrolPoint>,
        remainingPoints: Int?,
        sessionComplete: Boolean
    ) {
        val completedSessions = state.value.completedSessions
        if (taskId.isNullOrBlank() &&
            patrolSessionId == null &&
            points.isEmpty() &&
            completedSessions == 0
        ) {
            patrolSessionStore.clear()
            return
        }
        patrolSessionStore.save(
            com.workguard.patrol.data.PatrolSessionSnapshot(
                taskId = taskId,
                patrolSessionId = patrolSessionId,
                points = points,
                completedSessions = completedSessions,
                shiftKey = activeShiftKey,
                remainingPoints = remainingPoints,
                sessionComplete = sessionComplete
            )
        )
    }

    private fun requestFaceScan() {
        viewModelScope.launch {
            _events.emit(PatrolEvent.RequireFaceScan(FaceContext.TASK))
        }
    }

    private fun hasValidFaceSession(): Boolean {
        val session = faceSessionStore.getSession() ?: return false
        return session.isValidFor(FaceContext.TASK, clock.nowMillis())
    }

    private fun validateAttendance(
        response: com.workguard.core.network.AttendanceTodayResponse,
        shiftInfo: String?
    ): String? {
        val reason = response.reason?.takeIf { it.isNotBlank() }
        val formattedReason = reason?.let { formatShiftAwareMessage(it, shiftInfo) }
        val scheduleMissing = response.shiftStart.isNullOrBlank() &&
            response.shiftEnd.isNullOrBlank() &&
            response.shiftName.isNullOrBlank()
        if (scheduleMissing && formattedReason != null) {
            return formattedReason
        }
        if (response.checkInAt.isNullOrBlank()) {
            return formattedReason ?: "Belum check-in hari ini"
        }
        if (response.canCheckIn == false && formattedReason != null) {
            return formattedReason
        }
        return null
    }

    private fun buildShiftInfo(response: com.workguard.core.network.AttendanceTodayResponse): String? {
        val name = response.shiftName?.trim()?.takeIf { it.isNotBlank() }
        val start = response.shiftStart?.trim()?.takeIf { it.isNotBlank() }
        val end = response.shiftEnd?.trim()?.takeIf { it.isNotBlank() }
        val range = listOfNotNull(start, end).joinToString(" - ")
        return when {
            name != null && range.isNotBlank() -> "Shift $name ($range)"
            name != null -> "Shift $name"
            range.isNotBlank() -> "Shift $range"
            else -> null
        }
    }

    private fun buildShiftKey(response: com.workguard.core.network.AttendanceTodayResponse): String? {
        val checkInAt = response.checkInAt?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val shiftName = response.shiftName?.trim().orEmpty()
        val shiftStart = response.shiftStart?.trim().orEmpty()
        val shiftEnd = response.shiftEnd?.trim().orEmpty()
        return listOf(checkInAt, shiftName, shiftStart, shiftEnd).joinToString("|")
    }

    private fun formatShiftAwareMessage(reason: String, shiftInfo: String?): String {
        if (shiftInfo.isNullOrBlank()) return reason
        val normalized = reason.lowercase()
        val shouldAppend = normalized.contains("jam kerja") ||
            normalized.contains("shift") ||
            normalized.contains("jadwal")
        return if (shouldAppend) {
            "$reason\n$shiftInfo"
        } else {
            reason
        }
    }

    private fun com.workguard.core.location.LocationSnapshot?.toReading(): GpsReading? {
        val accuracy = this?.accuracyMeters ?: return null
        return GpsReading(accuracyMeters = accuracy, isMocked = this.isMocked)
    }
}

sealed interface PatrolEvent {
    data class RequireFaceScan(val context: FaceContext) : PatrolEvent
    data object Finished : PatrolEvent
}
