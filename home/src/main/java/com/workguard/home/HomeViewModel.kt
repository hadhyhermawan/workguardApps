package com.workguard.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workguard.core.location.LocationProvider
import com.workguard.core.network.ApiResult
import com.workguard.home.data.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {
    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _state = MutableStateFlow(HomeState(displayName = "", isLoading = true))
    val state = _state.asStateFlow()
    private var trackingPingSent = false
    private var trackingPingInFlight = false

    init {
        loadHome()
        requestTrackingPing()
    }

    fun loadHome(companyCode: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = homeRepository.loadHome(companyCode)) {
                is ApiResult.Success -> {
                    val data = result.data
                    _state.update {
                        it.copy(
                            displayName = data.displayName,
                            role = data.role,
                            photoUrl = data.photoUrl,
                            companyName = data.companyName,
                            companyLogoUrl = data.companyLogoUrl,
                            employeeId = data.employeeId,
                            companyId = data.companyId,
                            todayTaskSummary = data.todayTaskSummary,
                            todayTasks = data.todayTasks,
                            recentActivities = data.recentActivities,
                            quickStats = data.quickStats,
                            isLoading = false,
                            errorMessage = data.errorMessage
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.throwable.message
                        )
                    }
                }
            }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            requestTrackingPing()
        }
    }

    private fun requestTrackingPing() {
        if (trackingPingSent || trackingPingInFlight) {
            return
        }
        trackingPingInFlight = true
        viewModelScope.launch {
            val snapshot = locationProvider.getLastKnownLocation()
            if (snapshot == null) {
                Log.w(TAG, "Tracking ping skipped: location unavailable")
                trackingPingInFlight = false
                return@launch
            }
            trackingPingSent = true
            trackingPingInFlight = false
            when (val result = homeRepository.sendTrackingPing(snapshot)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Tracking ping success")
                }
                is ApiResult.Error -> {
                    Log.w(TAG, "Tracking ping failed: ${result.throwable.message}")
                }
            }
        }
    }
}
