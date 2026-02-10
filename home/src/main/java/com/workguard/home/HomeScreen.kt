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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    val todayLabel = remember {
        val formatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        formatter.format(Calendar.getInstance().time)
    }
    val locationLabel = state.companyName?.takeIf { it.isNotBlank() } ?: "Lokasi belum tersedia"

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
    var showScheduleSheet by remember { mutableStateOf(false) }
    val scheduleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val openScheduleSheet = remember { { showScheduleSheet = true } }
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(displayName, initials, state.photoUrl, size = 56.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headerSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = muted
                    )
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF1F2A30)
                    )
                }
                NotificationBell(count = violationsToday, accent = accent, onClick = onTaskClick)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(todayLabel, style = MaterialTheme.typography.labelMedium, color = muted)
                Text(locationLabel, style = MaterialTheme.typography.labelMedium, color = muted, textAlign = TextAlign.End)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoPill(
                    title = "Masuk",
                    value = formatQuickTime(quickStats.checkInAt),
                    accent = accent,
                    cardColor = cardColor,
                    muted = muted,
                    modifier = Modifier.weight(1f)
                )
                InfoPill(
                    title = "Pulang",
                    value = formatQuickTime(quickStats.checkOutAt),
                    accent = accent,
                    cardColor = cardColor,
                    muted = muted,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBadge(
                    value = "${quickStats.pendingPermits}",
                    label = "Tidak Hadir",
                    icon = Icons.Outlined.EventNote,
                    accent = accent,
                    muted = muted,
                    cardColor = cardColor,
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    value = "${quickStats.pendingOvertimes}",
                    label = "Kehadiran",
                    icon = Icons.Outlined.CalendarToday,
                    accent = accent,
                    muted = muted,
                    cardColor = cardColor,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            AllMenuCard(
                cardColor = cardColor,
                accent = accent,
                muted = muted,
                onClick = openScheduleSheet
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Patroli Hari Ini",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF1F2A30)
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (todayTasks.isEmpty()) {
                EmptyStateCard(text = "Belum ada jadwal patroli.")
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(todayTasks) { task ->
                        PatrolTaskChip(task = task, accent = accent, cardColor = cardColor, muted = muted)
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Aktivitas Terkini",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF1F2A30)
            )
            Spacer(modifier = Modifier.height(12.dp))
            ActivityCompactList(
                activities = recentActivities,
                accent = accent,
                cardColor = cardColor,
                muted = muted
            )
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
            sheetState = scheduleSheetState,
            dragHandle = null,
            containerColor = cardColor,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier.fillMaxHeight(0.7f)
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
            Spacer(modifier = Modifier.height(4.dp))
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
    initials: String,
    size: Dp = 48.dp
) {
    val avatarBackground = Color(0xFF1F2A30)
    Box(
        modifier = Modifier
            .size(size)
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
private fun DateLocationRow(
    dateLabel: String,
    locationLabel: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel,
            color = Color(0xFF6D7A7E),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(accent.copy(alpha = 0.14f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = "Lokasi",
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = locationLabel,
                color = Color(0xFF1F2A30),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
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
private fun QuickStatsGrid(
    cardColor: Color,
    accent: Color,
    muted: Color,
    stats: List<StatCardData>,
    statusCard: StatCardData?
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
            }
        }
        if (statusCard != null) {
            StatCardFullWidth(
                data = statusCard,
                cardColor = cardColor,
                accent = accent,
                muted = muted
            )
        }
    }
}

@Composable
private fun Avatar(name: String, initials: String, photoUrl: String?, size: Dp) {
    ProfileAvatar(photoUrl = photoUrl, initials = initials, size = size)
}

@Composable
private fun NotificationBell(count: Int, accent: Color, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = "Notifikasi",
                tint = Color(0xFF1F2A30)
            )
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE74C3C)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.coerceAtMost(99).toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun InfoPill(
    title: String,
    value: String,
    accent: Color,
    cardColor: Color,
    muted: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = value.ifBlank { "--:--" },
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF1F2A30)
                )
                Text(text = title, style = MaterialTheme.typography.bodySmall, color = muted)
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F3F4))
            )
        }
    }
}

@Composable
private fun PatrolTaskChip(task: HomeTaskItem, accent: Color, cardColor: Color, muted: Color) {
    val time = formatTaskSchedule(task)
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = task.title?.takeIf { it.isNotBlank() } ?: "Patroli",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF1F2A30)
            )
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        }
    }
}

@Composable
private fun StatBadge(
    value: String,
    label: String,
    icon: ImageVector,
    accent: Color,
    muted: Color,
    cardColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = accent)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF1F2A30)
                )
                Text(text = label, style = MaterialTheme.typography.bodySmall, color = muted)
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
private fun StatCardFullWidth(
    data: StatCardData,
    cardColor: Color,
    accent: Color,
    muted: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
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
private fun ActivityCompactList(
    activities: List<HomeActivityItem>,
    accent: Color,
    cardColor: Color,
    muted: Color
) {
    if (activities.isEmpty()) {
        EmptyStateCard(text = "Belum ada aktivitas terbaru.")
        return
    }
    val first = activities.first()
    val date = first.date?.takeIf { it.isNotBlank() } ?: "--"
    val checkIn = first.time?.takeIf { it.isNotBlank() } ?: "--"
    val checkOut = first.status?.takeIf { it.isNotBlank() } ?: "--"
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE7F0F4)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = date, color = Color(0xFF1F2A30), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Masuk $checkIn", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1F2A30))
                    Text("Pulang $checkOut", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1F2A30))
                }
                Text("Total Jam ${first.statusColor ?: "--"}", style = MaterialTheme.typography.bodySmall, color = muted)
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
