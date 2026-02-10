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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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

@Composable
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
fun HomeScreen(
    state: HomeState,
    onTaskClick: () -> Unit,
    onRefresh: () -> Unit,
    onLocationPermissionResult: (Boolean) -> Unit,
    onWorkScheduleClick: () -> Unit
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
            AllMenuBottomSheet(
                cardColor = cardColor,
                accent = accent,
                muted = muted,
                todaySchedule = state.todaySchedule,
                isTodayLoading = state.isTodayScheduleLoading,
                onClose = { showScheduleSheet = false },
                onWorkScheduleClick = {
                    showScheduleSheet = false
                    onWorkScheduleClick()
                }
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
private fun AllMenuBottomSheet(
    cardColor: Color,
    accent: Color,
    muted: Color,
    todaySchedule: WorkScheduleDay?,
    isTodayLoading: Boolean,
    onClose: () -> Unit,
    onWorkScheduleClick: () -> Unit
) {
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
                text = "All Menu",
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
            text = "Pilih menu untuk membuka halaman.",
            style = MaterialTheme.typography.bodySmall,
            color = muted
        )
        Spacer(modifier = Modifier.height(14.dp))

        MenuCard(
            title = "Jadwal Kerja",
            subtitle = buildString {
                val shift = normalizeShiftName(todaySchedule?.shiftName)
                val time = formatShiftRange(todaySchedule?.shiftStart, todaySchedule?.shiftEnd)
                when {
                    isTodayLoading -> append("Memuat jadwal hari ini...")
                    shift != null && time.isNotBlank() -> append("$shift • $time")
                    shift != null -> append(shift)
                    else -> append("Kalender shift & jam kerja")
                }
            },
            icon = Icons.Outlined.CalendarToday,
            cardColor = cardColor,
            accent = accent,
            muted = muted,
            onClick = onWorkScheduleClick
        )
    }
}

@Composable
private fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
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
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFF1F2A30),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
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
