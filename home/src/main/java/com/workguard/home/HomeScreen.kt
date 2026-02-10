package com.workguard.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.workguard.core.notification.AppBadgeNotifier
import com.workguard.home.data.HomeActivityItem
import com.workguard.home.data.HomeTaskItem
import java.util.Calendar

@Composable
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
fun HomeScreen(
    state: HomeState,
    onTaskClick: () -> Unit,
    onRefresh: () -> Unit,
    onLocationPermissionResult: (Boolean) -> Unit,
    onLoadScheduleMonth: (year: Int, month: Int, force: Boolean) -> Unit
) {
    val background = Color(0xFFF4F7F8)
    val cardColor = Color(0xFFFFFFFF)
    val accent = Color(0xFF16B3A8)
    val muted = Color(0xFF7A878A)
    val displayName = state.displayName.ifBlank { "Employee" }
    val headerSubtitle = listOfNotNull(
        state.role?.takeIf { it.isNotBlank() },
        state.companyName?.takeIf { it.isNotBlank() }
    ).joinToString(" | ").ifBlank { "Welcome Back!" }
    val initials = remember(displayName) {
        displayName
            .split(" ")
            .filter { it.isNotBlank() }
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
            .ifBlank { "WG" }
    }
    val todayTasks = state.todayTasks
    val recentActivities = state.recentActivities
    val taskSummary = state.todayTaskSummary
    val quickStats = state.quickStats
    val violationsToday = quickStats.violationsToday
    val highlightedTask = todayTasks.firstOrNull()
    val taskCardTitle = highlightedTask?.title?.takeIf { it.isNotBlank() } ?: "Task Patroli"
    val taskCardSchedule = highlightedTask?.let { formatTaskSchedule(it) } ?: "Jadwal belum tersedia"
    val taskCardStatus = highlightedTask?.status?.takeIf { it.isNotBlank() }
        ?: if (highlightedTask == null) "Belum ada task" else "Menunggu"
    val taskActionLabel = "Mulai Patroli"

    var showScheduleSheet by remember { mutableStateOf(false) }
    val scheduleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val openScheduleSheet = remember { { showScheduleSheet = true } }

    val statCards = listOf(
        StatCardData(
            value = formatQuickTime(quickStats.checkInAt),
            label = "Check In",
            icon = Icons.Outlined.PlayArrow,
            onClick = openScheduleSheet
        ),
        StatCardData(
            value = formatQuickTime(quickStats.checkOutAt),
            label = "Check Out",
            icon = Icons.Outlined.Stop,
            onClick = openScheduleSheet
        ),
        StatCardData(
            value = quickStats.pendingPermits.toString(),
            label = "Izin Pending",
            icon = Icons.Outlined.EventNote,
            onClick = openScheduleSheet
        ),
        StatCardData(
            value = quickStats.pendingOvertimes.toString(),
            label = "Lembur Pending",
            icon = Icons.Outlined.Timer,
            onClick = openScheduleSheet
        ),
        StatCardData(
            value = quickStats.attendanceStatus?.takeIf { it.isNotBlank() } ?: "-",
            label = "Status Hadir",
            icon = Icons.Outlined.CalendarToday,
            onClick = openScheduleSheet
        )
    )
    val taskSummaryText = "Mulai ${taskSummary.started} • Selesai ${taskSummary.completed} • Batal ${taskSummary.cancelled}"
    val context = LocalContext.current

    LaunchedEffect(violationsToday) {
        AppBadgeNotifier.updateViolationsBadge(context, violationsToday)
    }

    LaunchedEffect(showScheduleSheet) {
        if (showScheduleSheet) {
            val (year, month) = resolveActiveYearMonth(state.scheduleMonth)
            onLoadScheduleMonth(year, month, false)
        }
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isLocationEnabled by remember {
        mutableStateOf(checkLocationEnabled(context))
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            isLocationEnabled = checkLocationEnabled(context)
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = onRefresh
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLocationEnabled = checkLocationEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasLocationPermission, isLocationEnabled) {
        if (hasLocationPermission && isLocationEnabled) {
            startTrackingService(context)
            onLocationPermissionResult(true)
        }
    }

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
            if (!hasLocationPermission) {
                LocationPermissionCard(
                    onEnable = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else if (!isLocationEnabled) {
                LocationServicesCard(
                    onEnable = {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            HeaderSection(
                initials = initials,
                displayName = displayName,
                subtitle = headerSubtitle,
                photoUrl = state.photoUrl,
                notificationCount = violationsToday,
                accent = accent
            )
            Spacer(modifier = Modifier.height(18.dp))
            QuickStatsSection(
                cardColor = cardColor,
                accent = accent,
                muted = muted,
                stats = statCards
            )
            Spacer(modifier = Modifier.height(12.dp))
            AllMenuCard(
                cardColor = cardColor,
                accent = accent,
                muted = muted,
                onClick = openScheduleSheet
            )
            Spacer(modifier = Modifier.height(12.dp))
            WorkHoursCard(
                cardColor = cardColor,
                accent = accent,
                muted = muted,
                schedule = state.todaySchedule,
                isLoading = state.isTodayScheduleLoading,
                onClick = openScheduleSheet
            )
            Spacer(modifier = Modifier.height(16.dp))
            TaskHighlightCard(
                title = taskCardTitle,
                schedule = taskCardSchedule,
                status = taskCardStatus,
                actionLabel = taskActionLabel,
                accent = accent,
                muted = muted,
                cardColor = cardColor,
                onClick = onTaskClick
            )
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(
                title = "Today Task",
                actionText = "See More",
                onActionClick = onTaskClick
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = taskSummaryText,
                color = muted,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (todayTasks.isEmpty()) {
                EmptyStateCard(text = "Belum ada tugas hari ini.")
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 12.dp)
                ) {
                    items(todayTasks) { task ->
                        TaskCard(
                            data = TaskCardData(
                                title = task.title?.takeIf { it.isNotBlank() } ?: "Tugas",
                                date = formatTaskSchedule(task),
                                description = task.description?.takeIf { it.isNotBlank() }
                                    ?: task.status?.takeIf { it.isNotBlank() }
                                    ?: "-"
                            ),
                            accent = accent,
                            cardColor = cardColor,
                            onClick = onTaskClick
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(
                title = "Recent Activity",
                actionText = "View all",
                onActionClick = onTaskClick
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (recentActivities.isEmpty()) {
                EmptyStateCard(text = "Belum ada aktivitas terbaru.")
            } else {
                recentActivities.forEach { activity ->
                    ActivityRow(
                        data = ActivityData(
                            title = activity.title?.takeIf { it.isNotBlank() } ?: "Aktivitas",
                            date = activity.date?.takeIf { it.isNotBlank() } ?: "-",
                            time = activity.time?.takeIf { it.isNotBlank() } ?: "-",
                            status = activity.status?.takeIf { it.isNotBlank() } ?: "-",
                            statusColor = resolveActivityStatusColor(
                                statusColor = activity.statusColor,
                                status = activity.status,
                                accent = accent,
                                muted = muted
                            ),
                            icon = resolveActivityIcon(activity)
                        ),
                        accent = accent,
                        cardColor = cardColor,
                        muted = muted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        PullRefreshIndicator(
            refreshing = state.isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = cardColor,
            contentColor = accent
        )
    }

    if (showScheduleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showScheduleSheet = false },
            sheetState = scheduleSheetState
        ) {
            WorkScheduleBottomSheet(
                cardColor = cardColor,
                accent = accent,
                muted = muted,
                monthState = state.scheduleMonth,
                todaySchedule = state.todaySchedule,
                onClose = { showScheduleSheet = false },
                onLoadMonth = onLoadScheduleMonth
            )
        }
    }
}

@Composable
private fun HeaderSection(
    initials: String,
    displayName: String,
    subtitle: String,
    photoUrl: String?,
    notificationCount: Int,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(photoUrl = photoUrl, initials = initials)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subtitle,
                color = Color(0xFF6D7A7E),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = displayName,
                color = Color(0xFF1F2A30),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(modifier = Modifier.size(42.dp)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(Color(0xFFE9F7F6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = "Notifications",
                    tint = accent
                )
            }
            if (notificationCount > 0) {
                val badgeText = if (notificationCount > 99) "99+" else notificationCount.toString()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 2.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE11D48))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    photoUrl: String?,
    initials: String
) {
    val avatarBackground = Color(0xFF1F2A30)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(avatarBackground),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNullOrBlank()) {
            Text(
                text = initials,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                    else -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationPermissionCard(
    onEnable: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Aktifkan lokasi",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2A30)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dibutuhkan untuk validasi absensi & tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A878A)
                )
            }
            Button(
                onClick = onEnable,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16B3A8))
            ) {
                Text("Izinkan")
            }
        }
    }
}

@Composable
private fun LocationServicesCard(
    onEnable: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GPS belum aktif",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2A30)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Nyalakan GPS untuk melanjutkan validasi lokasi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A878A)
                )
            }
            Button(
                onClick = onEnable,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16B3A8))
            ) {
                Text("Aktifkan")
            }
        }
    }
}

@Composable
private fun QuickStatsSection(
    cardColor: Color,
    accent: Color,
    muted: Color,
    stats: List<StatCardData>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stats.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { data ->
                    StatCard(
                        data = data,
                        cardColor = cardColor,
                        accent = accent,
                        muted = muted
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AllMenuCard(
    cardColor: Color,
    accent: Color,
    muted: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "All menu",
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "All Menu",
                    color = Color(0xFF1F2A30),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Lihat jadwal kerja",
                    color = muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                imageVector = Icons.Outlined.ArrowForward,
                contentDescription = "Buka",
                tint = muted
            )
        }
    }
}

@Composable
private fun WorkHoursCard(
    cardColor: Color,
    accent: Color,
    muted: Color,
    schedule: WorkScheduleDay?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val shiftName = normalizeShiftName(schedule?.shiftName)
    val scheduleDate = schedule?.date
    val timeRange = formatShiftRange(schedule?.shiftStart, schedule?.shiftEnd)
    val reason = schedule?.reason?.trim().orEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Jam kerja",
                        tint = accent
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Jam Kerja",
                        color = Color(0xFF1F2A30),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = scheduleDate?.let { formatHumanDate(it) } ?: "Hari ini",
                        color = muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> {
                    Text(
                        text = "Memuat jadwal kerja...",
                        color = muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                shiftName != null -> {
                    Text(
                        text = shiftName,
                        color = Color(0xFF1F2A30),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeRange.ifBlank { "-" },
                        color = muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                reason.isNotBlank() -> {
                    Text(
                        text = "Tidak ada jadwal",
                        color = Color(0xFF1F2A30),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reason,
                        color = muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {
                    Text(
                        text = "Jadwal belum tersedia",
                        color = muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatCard(
    data: StatCardData,
    cardColor: Color,
    accent: Color,
    muted: Color
) {
    val clickableModifier = if (data.onClick != null) {
        Modifier
            .weight(1f)
            .height(96.dp)
            .clickable(onClick = data.onClick)
    } else {
        Modifier
            .weight(1f)
            .height(96.dp)
    }
    Card(
        modifier = clickableModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column {
                Text(
                    text = data.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2A30)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = data.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = data.label,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2A30),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = actionText,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6D7A7E),
            modifier = Modifier.clickable(onClick = onActionClick)
        )
    }
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

@Composable
private fun TaskCard(
    data: TaskCardData,
    accent: Color,
    cardColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2A30)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.EventNote,
                    contentDescription = "Schedule",
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = data.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = accent
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = data.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D7A7E)
            )
        }
    }
}

@Composable
private fun TaskHighlightCard(
    title: String,
    schedule: String,
    status: String,
    actionLabel: String,
    accent: Color,
    muted: Color,
    cardColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Task Hari Ini",
                        style = MaterialTheme.typography.labelSmall,
                        color = muted
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2A30)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = schedule,
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun ActivityRow(
    data: ActivityData,
    accent: Color,
    cardColor: Color,
    muted: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = data.title,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2A30)
                )
                Text(
                    text = data.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = data.time,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2A30),
                    textAlign = TextAlign.End
                )
                Text(
                    text = data.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = data.statusColor
                )
            }
        }
    }
}

@Composable
private fun WorkScheduleBottomSheet(
    cardColor: Color,
    accent: Color,
    muted: Color,
    monthState: WorkScheduleMonthState,
    todaySchedule: WorkScheduleDay?,
    onClose: () -> Unit,
    onLoadMonth: (year: Int, month: Int, force: Boolean) -> Unit
) {
    val (year, month) = resolveActiveYearMonth(monthState)
    val monthPrefix = buildMonthPrefix(year, month)
    val todayDate = todaySchedule?.date
    val defaultSelectedDate = todayDate?.takeIf { it.startsWith(monthPrefix) } ?: "${monthPrefix}01"
    var selectedDate by remember(year, month, todayDate) { mutableStateOf(defaultSelectedDate) }

    val daysByDate = remember(monthState.days) { monthState.days.associateBy { it.date } }
    val selectedSchedule = daysByDate[selectedDate]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Jadwal Kerja",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2A30),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Tutup",
                    tint = muted
                )
            }
        }
        Text(
            text = "Tap tanggal untuk lihat detail shift.",
            style = MaterialTheme.typography.bodySmall,
            color = muted
        )
        Spacer(modifier = Modifier.height(12.dp))

        val navigationEnabled = !monthState.isLoading
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val (prevYear, prevMonth) = previousYearMonth(year, month)
                    onLoadMonth(prevYear, prevMonth, false)
                },
                enabled = navigationEnabled
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Bulan sebelumnya",
                    tint = if (navigationEnabled) muted else muted.copy(alpha = 0.4f)
                )
            }
            Text(
                text = formatMonthYear(year, month),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2A30),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = {
                    val (nextYear, nextMonth) = nextYearMonth(year, month)
                    onLoadMonth(nextYear, nextMonth, false)
                },
                enabled = navigationEnabled
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowForward,
                    contentDescription = "Bulan berikutnya",
                    tint = if (navigationEnabled) muted else muted.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val hasMonthData = monthState.days.isNotEmpty()
        if (monthState.isLoading && !hasMonthData) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 26.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = accent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(28.dp)
                )
            }
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
                EmptyStateCard(text = "Jadwal bulan ini belum tersedia.")
            } else {
                WorkScheduleCalendar(
                    year = year,
                    month = month,
                    daysByDate = daysByDate,
                    selectedDate = selectedDate,
                    todayDate = todayDate,
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
                                color = if (isToday) accent else Color(0xFF1F2A30)
                            )
                            if (shift != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(shiftColor.copy(alpha = 0.14f))
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = shift,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = shiftColor
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
            Text(
                text = formatHumanDate(date),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2A30)
            )
            Spacer(modifier = Modifier.height(8.dp))
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

private data class StatCardData(
    val value: String,
    val label: String,
    val icon: ImageVector,
    val onClick: (() -> Unit)? = null
)

private data class TaskCardData(
    val title: String,
    val date: String,
    val description: String
)

private data class ActivityData(
    val title: String,
    val date: String,
    val time: String,
    val status: String,
    val statusColor: Color,
    val icon: ImageVector
)

private fun formatTaskSchedule(task: HomeTaskItem): String {
    val date = task.date?.takeIf { it.isNotBlank() }
    val time = task.time?.takeIf { it.isNotBlank() }
    return when {
        date != null && time != null -> "$date - $time"
        date != null -> date
        time != null -> time
        else -> "-"
    }
}

private fun resolveActivityIcon(activity: HomeActivityItem): ImageVector {
    val source = listOfNotNull(activity.type, activity.title)
        .joinToString(" ")
        .lowercase()
    return when {
        source.contains("start") || source.contains("check in") || source.contains("masuk") ->
            Icons.Outlined.PlayArrow
        source.contains("end") || source.contains("check out") || source.contains("pulang") ->
            Icons.Outlined.Stop
        source.contains("leave") || source.contains("izin") || source.contains("cuti") ->
            Icons.Outlined.Timer
        else -> Icons.Outlined.PlayArrow
    }
}

private fun resolveActivityStatusColor(
    statusColor: String?,
    status: String?,
    accent: Color,
    muted: Color
): Color {
    val parsed = parseColorHex(statusColor)
    if (parsed != null) {
        return parsed
    }
    val normalized = status?.trim()?.lowercase().orEmpty()
    return when {
        normalized.contains("late") || normalized.contains("telat") -> Color(0xFFE76F51)
        normalized.contains("pending") || normalized.contains("menunggu") -> muted
        normalized.contains("on time") || normalized.contains("ontime") || normalized.contains("tepat") ->
            accent
        normalized.contains("selesai") || normalized.contains("done") -> accent
        else -> accent
    }
}

private fun parseColorHex(raw: String?): Color? {
    val value = raw?.trim()?.removePrefix("#").orEmpty()
    if (value.isBlank()) {
        return null
    }
    return try {
        when (value.length) {
            6 -> {
                val rgb = value.toInt(16)
                Color(0xFF000000.toInt() or rgb)
            }
            8 -> {
                val argb = value.toLong(16).toInt()
                Color(argb)
            }
            else -> null
        }
    } catch (_: NumberFormatException) {
        null
    }
}

private fun formatQuickTime(value: String?): String {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank()) return "--"
    val millis = com.workguard.core.util.IsoTimeUtil.parseMillis(trimmed) ?: return trimmed
    val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale("id", "ID"))
    return formatter.format(java.util.Date(millis))
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

private fun checkLocationEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(manager)
}

private fun startTrackingService(context: Context) {
    val intent = Intent().setClassName(context, "com.workguard.tracking.TrackingService")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}
