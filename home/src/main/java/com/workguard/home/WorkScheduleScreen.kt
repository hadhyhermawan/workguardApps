package com.workguard.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun WorkScheduleScreen(
    state: WorkScheduleScreenState,
    onRefresh: () -> Unit,
    onLoadMonth: (year: Int, month: Int, force: Boolean) -> Unit
) {
    val background = Color(0xFFF4F7F8)
    val cardColor = Color(0xFFFFFFFF)
    val accent = Color(0xFF16B3A8)
    val muted = Color(0xFF7A878A)
    val monthState = state.month
    val (year, month) = resolveActiveYearMonth(monthState)
    val monthPrefix = remember(year, month) { buildMonthPrefix(year, month) }

    val defaultSelectedDate = state.todayDate.takeIf { it.startsWith(monthPrefix) } ?: "${monthPrefix}01"
    var selectedDate by remember(year, month, state.todayDate) { mutableStateOf(defaultSelectedDate) }

    val daysByDate = remember(monthState.days) { monthState.days.associateBy { it.date } }
    val selectedSchedule = daysByDate[selectedDate]
    val pullRefreshState = rememberPullRefreshState(
        refreshing = monthState.isLoading,
        onRefresh = onRefresh
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            MonthNavigationRow(
                year = year,
                month = month,
                title = formatMonthYear(year, month),
                isLoading = monthState.isLoading,
                muted = muted,
                onPrevClick = {
                    val (prevYear, prevMonth) = previousYearMonth(year, month)
                    onLoadMonth(prevYear, prevMonth, false)
                },
                onNextClick = {
                    val (nextYear, nextMonth) = nextYearMonth(year, month)
                    onLoadMonth(nextYear, nextMonth, false)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            val hasMonthData = monthState.days.isNotEmpty()
            if (monthState.isLoading && !hasMonthData) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Memuat jadwal kerja...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                if (!monthState.errorMessage.isNullOrBlank()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sebagian jadwal gagal dimuat",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF9F1239)
                                )
                                Text(
                                    text = monthState.errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9F1239)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = { onLoadMonth(year, month, true) },
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Text("Muat ulang")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (!hasMonthData) {
                    EmptyStateCard(text = "Belum ada jadwal.")
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    WorkScheduleCalendar(
                        year = year,
                        month = month,
                        daysByDate = daysByDate,
                        selectedDate = selectedDate,
                        todayDate = state.todayDate,
                        accent = accent,
                        muted = muted,
                        onSelectDate = { selectedDate = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    WorkScheduleDayDetailCard(
                        date = selectedDate,
                        schedule = selectedSchedule,
                        cardColor = cardColor,
                        accent = accent,
                        muted = muted
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    AttendanceHistoryList(
                        items = state.history,
                        isLoading = state.isHistoryLoading,
                        error = state.historyError,
                        todayDate = state.todayDate,
                        accent = accent,
                        muted = muted,
                        cardColor = cardColor
                    )
                }
            }
        }

        PullRefreshIndicator(
            refreshing = monthState.isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = cardColor,
            contentColor = accent
        )
    }
}

@Composable
private fun MonthNavigationRow(
    year: Int,
    month: Int,
    title: String,
    isLoading: Boolean,
    muted: Color,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val enabled = !isLoading
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevClick, enabled = enabled) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Bulan sebelumnya",
                    tint = if (enabled) muted else muted.copy(alpha = 0.4f)
                )
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Jadwal kerja",
                    style = MaterialTheme.typography.labelSmall,
                    color = muted
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2A30)
                )
            }
            IconButton(onClick = onNextClick, enabled = enabled) {
                Icon(
                    imageVector = Icons.Outlined.ArrowForward,
                    contentDescription = "Bulan berikutnya",
                    tint = if (enabled) muted else muted.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun WorkScheduleCalendar(
    year: Int,
    month: Int,
    daysByDate: Map<String, WorkScheduleDay>,
    selectedDate: String,
    todayDate: String?,
    accent: Color,
    muted: Color,
    onSelectDate: (String) -> Unit
) {
    val weekdayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    Row(modifier = Modifier.fillMaxWidth()) {
        weekdayLabels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    val (offset, daysInMonth) = remember(year, month) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun ... 7=Sat
        val mondayBasedOffset = (dayOfWeek + 5) % 7 // 0=Mon ... 6=Sun
        mondayBasedOffset to cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val rows = remember(offset, daysInMonth) { ((offset + daysInMonth + 6) / 7).coerceAtLeast(5) }
    val monthPrefix = remember(year, month) { buildMonthPrefix(year, month) }
    val shape = RoundedCornerShape(14.dp)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (rowIndex in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (colIndex in 0 until 7) {
                    val cellIndex = rowIndex * 7 + colIndex
                    val day = cellIndex - offset + 1
                    if (day < 1 || day > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = "${monthPrefix}${day.toString().padStart(2, '0')}"
                        val schedule = daysByDate[date]
                        val shift = normalizeShiftName(schedule?.shiftName)
                        val shiftColor = resolveShiftColor(shift, accent, muted)
                        val shiftBadge = shiftBadgeLabel(shift)
                        val isSelected = date == selectedDate
                        val isToday = date == todayDate
                        val borderColor = when {
                            isSelected -> accent
                            else -> Color(0xFFE5EAEC)
                        }
                        val backgroundColor = when {
                            isSelected -> accent.copy(alpha = 0.12f)
                            isToday -> accent.copy(alpha = 0.07f)
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(shape)
                                .background(backgroundColor)
                                .border(width = 1.dp, color = borderColor, shape = shape)
                                .clickable { onSelectDate(date) }
                                .padding(8.dp)
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isToday) accent else Color(0xFF1F2A30),
                                modifier = Modifier.align(Alignment.Center)
                            )
                            if (shiftBadge != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(shiftColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shiftBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkScheduleDayDetailCard(
    date: String,
    schedule: WorkScheduleDay?,
    cardColor: Color,
    accent: Color,
    muted: Color
) {
    val shiftName = normalizeShiftName(schedule?.shiftName)
    val timeRange = formatShiftRange(schedule?.shiftStart, schedule?.shiftEnd)
    val reason = schedule?.reason?.trim().orEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = accent
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = formatHumanDate(date),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2A30)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            when {
                schedule == null -> {
                    Text(
                        text = "Jadwal belum dimuat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted
                    )
                }
                shiftName != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(resolveShiftColor(shiftName, accent, muted).copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = shiftName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = resolveShiftColor(shiftName, accent, muted)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = timeRange.ifBlank { "-" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted
                        )
                    }
                }
                reason.isNotBlank() -> {
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted
                    )
                }
                else -> {
                    Text(
                        text = "Tidak ada jadwal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceHistoryList(
    items: List<WorkScheduleDay>,
    isLoading: Boolean,
    error: String?,
    todayDate: String,
    accent: Color,
    muted: Color,
    cardColor: Color
) {
    val sorted = remember(items) {
        items
            .filter { isPastOrToday(it.date, todayDate) }
            .sortedByDescending { it.date }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Riwayat kehadiran",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2A30)
        )
        when {
            isLoading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = accent,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Memuat riwayat...",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                }
            }
            !error.isNullOrBlank() -> {
                EmptyStateCard(text = error)
            }
            sorted.isEmpty() -> {
                EmptyStateCard(text = "Belum ada riwayat kehadiran.")
            }
            else -> {
                sorted.forEach { day ->
                    AttendanceHistoryCard(
                        day = day,
                        accent = accent,
                        muted = muted,
                        cardColor = cardColor
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceHistoryCard(
    day: WorkScheduleDay,
    accent: Color,
    muted: Color,
    cardColor: Color
) {
    val dateInfo = remember(day.date) { parseDateInfo(day.date) }
    val shift = normalizeShiftName(day.shiftName)
    val timeRange = formatShiftRange(day.shiftStart, day.shiftEnd)
    val reason = day.reason?.trim().orEmpty()
    val checkIn = formatTimeShort(day.checkInAt)
    val checkOut = formatTimeShort(day.checkOutAt)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dateInfo.dayOfMonth,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                    Text(
                        text = dateInfo.dayOfWeek,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateInfo.formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
                Spacer(modifier = Modifier.height(4.dp))
                when {
                    shift != null -> {
                        Text(
                            text = shift,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2A30)
                        )
                    }
                    else -> {
                        Text(
                            text = "Shift tidak tersedia",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2A30)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text(
                            text = "Check-in",
                            style = MaterialTheme.typography.labelSmall,
                            color = muted
                        )
                        Text(
                            text = checkIn.ifBlank { "--:--" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2A30)
                        )
                    }
                    Column {
                        Text(
                            text = "Check-out",
                            style = MaterialTheme.typography.labelSmall,
                            color = muted
                        )
                        Text(
                            text = checkOut.ifBlank { "--:--" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2A30)
                        )
                    }
                    Column {
                        Text(
                            text = "Shift",
                            style = MaterialTheme.typography.labelSmall,
                            color = muted
                        )
                        Text(
                            text = timeRange.ifBlank { "-" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2A30)
                        )
                    }
                }
                if (reason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                }
            }
        }
    }
}

private data class DateInfo(
    val dayOfMonth: String,
    val dayOfWeek: String,
    val formattedDate: String
)

private fun parseDateInfo(date: String): DateInfo {
    // Expects yyyy-MM-dd
    val cal = Calendar.getInstance()
    return try {
        val parts = date.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 1970
        val month = parts.getOrNull(1)?.toIntOrNull()?.minus(1) ?: 0
        val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
        cal.set(year, month, day)
        val dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale("id", "ID"))
            ?.replaceFirstChar { it.uppercase() }
            ?: "Day"
        val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale("id", "ID"))
            ?.replaceFirstChar { it.uppercase() }
            ?: "Mon"
        DateInfo(
            dayOfMonth = day.toString().padStart(2, '0'),
            dayOfWeek = dayName,
            formattedDate = "$day $monthName $year"
        )
    } catch (_: Exception) {
        DateInfo(
            dayOfMonth = "--",
            dayOfWeek = "Day",
            formattedDate = date
        )
    }
}

private fun isPastOrToday(date: String, today: String): Boolean {
    if (date.length < 10 || today.length < 10) return true
    return date <= today
}

private fun formatTimeShort(value: String?): String {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank()) return ""
    val millis = com.workguard.core.util.IsoTimeUtil.parseMillis(trimmed)
    if (millis != null) {
        val formatter = java.text.SimpleDateFormat("HH:mm", Locale("id", "ID"))
        return formatter.format(java.util.Date(millis))
    }
    val regex = Regex("^\\d{2}:\\d{2}")
    return if (regex.containsMatchIn(trimmed)) trimmed.take(5) else trimmed
}

@Composable
private fun EmptyStateCard(text: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF7A878A),
            modifier = Modifier.padding(16.dp)
        )
    }
}

private fun resolveActiveYearMonth(monthState: WorkScheduleMonthState): Pair<Int, Int> {
    val year = monthState.year
    val month = monthState.month
    if (year > 0 && month in 1..12) return year to month
    val cal = Calendar.getInstance()
    return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
}

private fun previousYearMonth(year: Int, month: Int): Pair<Int, Int> {
    return if (month == 1) (year - 1) to 12 else year to (month - 1)
}

private fun nextYearMonth(year: Int, month: Int): Pair<Int, Int> {
    return if (month == 12) (year + 1) to 1 else year to (month + 1)
}

private fun buildMonthPrefix(year: Int, month: Int): String {
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-"
}

private fun formatMonthYear(year: Int, month: Int): String {
    val months = listOf(
        "Januari",
        "Februari",
        "Maret",
        "April",
        "Mei",
        "Juni",
        "Juli",
        "Agustus",
        "September",
        "Oktober",
        "November",
        "Desember"
    )
    val name = months.getOrNull(month - 1) ?: month.toString()
    return "$name $year"
}

private fun formatHumanDate(date: String): String {
    // Expected: yyyy-MM-dd
    if (date.length < 10) return date
    val year = date.substring(0, 4)
    val month = date.substring(5, 7).toIntOrNull() ?: return date
    val day = date.substring(8, 10)
    val months = listOf(
        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "Mei",
        "Jun",
        "Jul",
        "Agu",
        "Sep",
        "Okt",
        "Nov",
        "Des"
    )
    val name = months.getOrNull(month - 1) ?: month.toString()
    return "${day.toIntOrNull() ?: day} $name $year"
}

private fun normalizeShiftName(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return null
    val withoutPrefix = value.replace(Regex("^shift\\s+", RegexOption.IGNORE_CASE), "")
    return withoutPrefix.trim().ifBlank { value }
}

private fun resolveShiftColor(shiftName: String?, accent: Color, muted: Color): Color {
    val normalized = shiftName?.trim()?.lowercase().orEmpty()
    return when {
        normalized.contains("pagi") -> accent
        normalized.contains("sore") -> Color(0xFFF59E0B)
        normalized.contains("malam") -> Color(0xFF6366F1)
        normalized.isBlank() -> muted
        else -> accent
    }
}

private fun shiftBadgeLabel(shiftName: String?): String? {
    val normalized = shiftName?.trim()?.lowercase().orEmpty()
    return when {
        normalized.contains("pagi") -> "P"
        normalized.contains("sore") -> "S"
        normalized.contains("malam") -> "M"
        normalized.isBlank() -> null
        else -> shiftName?.trim()?.take(1)?.uppercase(Locale.getDefault())
    }
}

private fun formatShiftRange(shiftStart: String?, shiftEnd: String?): String {
    val start = formatShiftTime(shiftStart)
    val end = formatShiftTime(shiftEnd)
    return when {
        start.isNotBlank() && end.isNotBlank() -> "$start - $end"
        start.isNotBlank() -> start
        end.isNotBlank() -> end
        else -> ""
    }
}

private fun formatShiftTime(value: String?): String {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank()) return ""
    val millis = com.workguard.core.util.IsoTimeUtil.parseMillis(trimmed)
    if (millis != null) {
        val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale("id", "ID"))
        return formatter.format(java.util.Date(millis))
    }
    // Common server formats: HH:mm or HH:mm:ss
    val timeRegex = Regex("^\\d{2}:\\d{2}(:\\d{2})?$")
    if (timeRegex.matches(trimmed)) {
        return trimmed.take(5)
    }
    return trimmed
}
