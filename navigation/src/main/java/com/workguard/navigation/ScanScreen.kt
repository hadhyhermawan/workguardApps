package com.workguard.navigation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workguard.attendance.AttendanceState
import com.workguard.core.util.IsoTimeUtil
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun ScanScreen(
    state: AttendanceState,
    onCheckInClick: () -> Unit,
    onCheckOutClick: () -> Unit,
    onReload: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val timeFont = FontFamily.Monospace
    val gradient = Brush.verticalGradient(
        colors = listOf(UiTokens.Soft, UiTokens.Bg)
    )
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale("id", "ID")) }
    val dateFormatter = remember { SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")) }
    val shortTimeFormatter = remember { SimpleDateFormat("HH:mm", Locale("id", "ID")) }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    val timeText = timeFormatter.format(Date(nowMillis))
    val dateText = dateFormatter.format(Date(nowMillis)).lowercase(Locale("id", "ID"))
    val checkInText = formatTime(state.checkInAt, shortTimeFormatter)
    val checkOutText = formatTime(state.checkOutAt, shortTimeFormatter)
    val scheduleText = buildScheduleText(state)
    var locationWarning by remember { mutableStateOf<String?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(hasLocationPermission(context))
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
            locationWarning = if (isLocationEnabled) null else "Aktifkan GPS untuk absen."
        } else {
            locationWarning = "Izin lokasi dibutuhkan untuk absen."
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLocationEnabled = checkLocationEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun ensureLocationReady(onReady: () -> Unit) {
        if (!hasLocationPermission) {
            locationWarning = "Izin lokasi dibutuhkan untuk absen."
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }
        if (!isLocationEnabled) {
            locationWarning = "Aktifkan GPS untuk absen."
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }
        locationWarning = null
        onReady()
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoadingStatus,
        onRefresh = onReload
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(horizontal = 20.dp, vertical = 18.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.displayMedium,
                    fontFamily = timeFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = UiTokens.Text
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = UiTokens.Muted
                )
                Text(
                    text = "Absensi harian",
                    style = MaterialTheme.typography.bodySmall,
                    color = UiTokens.Muted
                )
            }

            AttendancePhotoCircle(
                photoUrl = resolveAttendancePhoto(state),
                isLoading = state.isLoading
            )

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Jadwal kerja",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = UiTokens.Text
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = scheduleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = UiTokens.Muted
                    )
                }
            }

            val errorMessage = state.errorMessage
            if (errorMessage != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = UiTokens.Soft),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = UiTokens.Text,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            if (locationWarning != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = UiTokens.Soft),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = locationWarning ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = UiTokens.Text,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusItem(label = "Masuk", value = checkInText)
                        StatusItem(label = "Pulang", value = checkOutText)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionButton(
                            label = "Absen Masuk",
                            enabled = !state.isLoading && state.canCheckIn,
                            isLoading = state.isLoading && state.activeAction == com.workguard.attendance.AttendanceAction.CHECK_IN,
                            onClick = { ensureLocationReady(onCheckInClick) },
                            modifier = Modifier.weight(1f)
                        )
                        ActionButton(
                            label = "Absen Pulang",
                            enabled = !state.isLoading && state.canCheckOut,
                            isLoading = state.isLoading && state.activeAction == com.workguard.attendance.AttendanceAction.CHECK_OUT,
                            onClick = { ensureLocationReady(onCheckOutClick) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
        PullRefreshIndicator(
            refreshing = state.isLoadingStatus,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = UiTokens.Surface,
            contentColor = UiTokens.Accent
        )
    }
}

@Composable
private fun StatusItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = UiTokens.Muted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = UiTokens.Text
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = UiTokens.Accent)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.width(16.dp).height(16.dp),
                strokeWidth = 2.dp,
                color = UiTokens.Surface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Memproses")
        } else {
            Text(label)
        }
    }
}

@Composable
private fun AttendancePhotoCircle(
    photoUrl: String?,
    isLoading: Boolean
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(180.dp),
            colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.size(220.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(UiTokens.Bg, RoundedCornerShape(180.dp)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    photoUrl.isNullOrBlank() -> {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = UiTokens.Accent
                            )
                        } else {
                            Image(
                                painter = painterResource(id = com.workguard.attendance.R.drawable.user),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                    else -> {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto absensi",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            when (painter.state) {
                                is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                                is AsyncImagePainter.State.Loading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp,
                                        color = UiTokens.Accent
                                    )
                                }
                                else -> {
                                    Image(
                                        painter = painterResource(id = com.workguard.attendance.R.drawable.user),
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp)
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

private fun resolveAttendancePhoto(state: AttendanceState): String? {
    val status = state.todayStatus?.trim()?.uppercase().orEmpty()
    val checkIn = state.checkInPhotoUrl?.takeIf { it.isNotBlank() }
    val checkOut = state.checkOutPhotoUrl?.takeIf { it.isNotBlank() }
    return when {
        status.contains("CHECKED_OUT") -> checkOut ?: checkIn
        status.contains("CHECKED_IN") -> checkIn ?: checkOut
        else -> checkOut ?: checkIn
    }
}

private fun formatTime(value: String?, formatter: SimpleDateFormat): String {
    val millis = IsoTimeUtil.parseMillis(value)
    return if (millis == null) "--:--" else formatter.format(Date(millis))
}

private fun buildScheduleText(state: AttendanceState): String {
    val start = state.shiftStart
    val end = state.shiftEnd
    val name = state.shiftName?.takeIf { it.isNotBlank() } ?: "Shift"
    return if (!start.isNullOrBlank() && !end.isNullOrBlank()) {
        "$name - $start - $end"
    } else {
        "Jadwal belum tersedia"
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

private fun checkLocationEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    return LocationManagerCompat.isLocationEnabled(manager)
}
