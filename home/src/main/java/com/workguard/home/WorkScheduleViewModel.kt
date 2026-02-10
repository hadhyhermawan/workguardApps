package com.workguard.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workguard.core.network.ApiResult
import com.workguard.core.network.ApiService
import com.workguard.core.network.AttendanceTodayResponse
import com.workguard.core.util.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.util.Calendar
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

data class WorkScheduleScreenState(
    val todayDate: String = "",
    val month: WorkScheduleMonthState = WorkScheduleMonthState(),
    val history: List<WorkScheduleDay> = emptyList(),
    val isHistoryLoading: Boolean = false,
    val historyError: String? = null
)

@HiltViewModel
class WorkScheduleViewModel @Inject constructor(
    private val apiService: ApiService,
    private val clock: Clock
) : ViewModel() {
    companion object {
        private const val TAG = "WorkScheduleViewModel"
        private const val SCHEDULE_FETCH_CONCURRENCY = 6
    }

    private val _state = MutableStateFlow(WorkScheduleScreenState())
    val state = _state.asStateFlow()

    init {
        val nowMillis = clock.nowMillis()
        val today = formatDate(nowMillis)
        val (year, month) = resolveYearMonth(nowMillis)
        _state.update {
            it.copy(
                todayDate = today,
                month = it.month.copy(year = year, month = month)
            )
        }
        loadMonth(year, month)
        loadHistory(year, month)
    }

    fun refresh() {
        val current = state.value.month
        val (fallbackYear, fallbackMonth) = resolveYearMonth(clock.nowMillis())
        val year = current.year.takeIf { it > 0 } ?: fallbackYear
        val month = current.month.takeIf { it in 1..12 } ?: fallbackMonth
        loadMonth(year, month, force = true)
        loadHistory(year, month, force = true)
    }

    fun loadMonth(year: Int, month: Int, force: Boolean = false) {
        if (year <= 0 || month !in 1..12) return
        val current = state.value.month
        if (!force && current.year == year && current.month == month && current.days.isNotEmpty()) {
            return
        }
        if (current.isLoading) {
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    month = it.month.copy(
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
                    month = it.month.copy(
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

    fun loadHistory(year: Int, month: Int, force: Boolean = false) {
        if (year <= 0 || month !in 1..12) return
        if (!force && state.value.history.isNotEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isHistoryLoading = true, historyError = null) }
            val monthStr = "%04d-%02d".format(year, month)
            try {
                val response = apiService.getAttendanceHistory(month = monthStr)
                if (response.success == false) {
                    throw IllegalStateException(response.message ?: "Gagal memuat history")
                }
                val items = response.data.orEmpty()
                val mapped = items.map {
                    WorkScheduleDay(
                        date = it.date,
                        shiftName = it.shiftName,
                        shiftStart = it.shiftStart,
                        shiftEnd = it.shiftEnd,
                        checkInAt = it.checkInAt,
                        checkOutAt = it.checkOutAt,
                        reason = it.reason
                    )
                }
                _state.update { st ->
                    st.copy(
                        history = mapped,
                        isHistoryLoading = false,
                        historyError = null
                    )
                }
            } catch (e: Exception) {
                _state.update { st ->
                    st.copy(
                        isHistoryLoading = false,
                        historyError = e.message ?: "Gagal memuat history"
                    )
                }
            }
        }
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
            Log.w(TAG, "getAttendanceToday failed (${e.code()}): ${e.message()}")
            ApiResult.Error(IllegalStateException("Gagal mengambil jadwal (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }
}
