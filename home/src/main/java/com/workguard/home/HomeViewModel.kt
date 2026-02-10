package com.workguard.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workguard.core.location.LocationProvider
import com.workguard.core.network.ApiResult
import com.workguard.core.network.ApiService
import com.workguard.core.network.AttendanceTodayResponse
import com.workguard.home.data.HomeRepository
import com.workguard.core.util.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException
import java.util.Calendar

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val locationProvider: LocationProvider,
    private val apiService: ApiService,
    private val clock: Clock
) : ViewModel() {
    companion object {
        private const val TAG = "HomeViewModel"
        private const val SCHEDULE_FETCH_CONCURRENCY = 6
    }

    private val _state = MutableStateFlow(HomeState(displayName = "", isLoading = true))
    val state = _state.asStateFlow()
    private var trackingPingSent = false
    private var trackingPingInFlight = false

    init {
        initScheduleMonth()
        loadHome()
        loadTodaySchedule()
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

    fun refresh() {
        loadHome()
        loadTodaySchedule(force = true)
        val month = state.value.scheduleMonth
        if (month.year > 0 && month.month in 1..12 && month.days.isNotEmpty()) {
            loadScheduleMonth(month.year, month.month, force = true)
        }
    }

    fun loadTodaySchedule(force: Boolean = false) {
        val nowMillis = clock.nowMillis()
        val date = formatDate(nowMillis)
        val existing = state.value.todaySchedule
        if (!force && existing?.date == date) {
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isTodayScheduleLoading = true) }
            when (val result = fetchAttendanceToday(date)) {
                is ApiResult.Success -> {
                    val data = result.data
                    _state.update {
                        it.copy(
                            todaySchedule = WorkScheduleDay(
                                date = date,
                                shiftName = data.shiftName,
                                shiftStart = data.shiftStart,
                                shiftEnd = data.shiftEnd,
                                checkInAt = data.checkInAt,
                                checkOutAt = data.checkOutAt,
                                reason = data.reason
                            ),
                            isTodayScheduleLoading = false
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update {
                        it.copy(
                            todaySchedule = WorkScheduleDay(
                                date = date,
                                reason = result.throwable.message ?: "Gagal memuat jadwal"
                            ),
                            isTodayScheduleLoading = false
                        )
                    }
                }
            }
        }
    }

    fun loadScheduleMonth(year: Int, month: Int, force: Boolean = false) {
        if (year <= 0 || month !in 1..12) return
        val current = state.value.scheduleMonth
        if (!force && current.year == year && current.month == month && current.days.isNotEmpty()) {
            return
        }
        if (current.isLoading) {
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    scheduleMonth = it.scheduleMonth.copy(
                        year = year,
                        month = month,
                        days = emptyList(),
                        isLoading = true,
                        errorMessage = null
                    )
                )
            }
            val dates = buildMonthDates(year, month)
            val semaphore = Semaphore(SCHEDULE_FETCH_CONCURRENCY)
            val results = coroutineScope {
                dates.map { date ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            date to fetchAttendanceToday(date)
                        }
                    }
                }.awaitAll()
            }
            val sorted = results.sortedBy { it.first }
            val days = mutableListOf<WorkScheduleDay>()
            var errorMessage: String? = null
            sorted.forEach { (date, result) ->
                when (result) {
                    is ApiResult.Success -> {
                        val data = result.data
                        days += WorkScheduleDay(
                            date = date,
                            shiftName = data.shiftName,
                            shiftStart = data.shiftStart,
                            shiftEnd = data.shiftEnd,
                            checkInAt = data.checkInAt,
                            checkOutAt = data.checkOutAt,
                            reason = data.reason
                        )
                    }
                    is ApiResult.Error -> {
                        val message = result.throwable.message ?: "Gagal memuat jadwal"
                        if (errorMessage == null) {
                            errorMessage = message
                        }
                        days += WorkScheduleDay(
                            date = date,
                            reason = message
                        )
                    }
                }
            }
            _state.update {
                it.copy(
                    scheduleMonth = it.scheduleMonth.copy(
                        year = year,
                        month = month,
                        days = days,
                        isLoading = false,
                        errorMessage = errorMessage
                    )
                )
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

    private fun initScheduleMonth() {
        val (year, month) = resolveYearMonth(clock.nowMillis())
        _state.update { it.copy(scheduleMonth = it.scheduleMonth.copy(year = year, month = month)) }
    }

    private fun resolveYearMonth(nowMillis: Long): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }

    private fun formatDate(nowMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    private fun buildMonthDates(year: Int, month: Int): List<String> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (1..maxDay).map { day ->
            "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        }
    }

    private suspend fun fetchAttendanceToday(date: String): ApiResult<AttendanceTodayResponse> {
        return try {
            val response = apiService.getAttendanceToday(date)
            if (response.success == false) {
                ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal mengambil jadwal")
                )
            } else {
                val data = response.data
                    ?: return ApiResult.Error(IllegalStateException("Data jadwal kosong"))
                ApiResult.Success(data)
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal mengambil jadwal (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }
}
