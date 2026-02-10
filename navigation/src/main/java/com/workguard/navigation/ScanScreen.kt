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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workguard.navigation.R
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
    val poppins = remember {
        FontFamily(
            Font(R.font.poppins_regular, FontWeight.Normal),
            Font(R.font.poppins_semibold, FontWeight.SemiBold)
        )
    }
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
    val dateRaw = dateFormatter.format(Date(nowMillis))
    val dateText = dateRaw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("id", "ID")) else it.toString() }
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
            .background(UiTokens.Bg)
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timeText,
                    color = UiTokens.Text,
                    fontFamily = poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 50.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = UiTokens.Muted,
                    fontFamily = poppins,
                    fontWeight = FontWeight.Normal
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            AttendancePhotoCircle(
                photoUrl = resolveAttendancePhoto(state),
                isLoading = state.isLoading
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Card ringkas check-in/out + shift
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Masuk", style = MaterialTheme.typography.labelSmall, color = UiTokens.Muted, fontFamily = poppins)
                            Text(checkInText, style = MaterialTheme.typography.titleMedium, color = UiTokens.Text, fontFamily = poppins, fontWeight = FontWeight.SemiBold)
                            val start = formatShiftTime(state.shiftStart)
                            if (start != null) {
                                Text(start, style = MaterialTheme.typography.labelSmall, color = UiTokens.Muted, fontSize = 11.sp, fontFamily = poppins)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(UiTokens.Divider)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Pulang", style = MaterialTheme.typography.labelSmall, color = UiTokens.Muted, fontFamily = poppins)
                            Text(checkOutText, style = MaterialTheme.typography.titleMedium, color = UiTokens.Text, fontFamily = poppins, fontWeight = FontWeight.SemiBold)
                            val end = formatShiftTime(state.shiftEnd)
                            if (end != null) {
                                Text(end, style = MaterialTheme.typography.labelSmall, color = UiTokens.Muted, fontSize = 11.sp, fontFamily = poppins)
                            }
                        }
                    }
                }
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = state.shiftName?.takeIf { it.isNotBlank() } ?: "Shift",
                            style = MaterialTheme.typography.labelSmall,
                            color = UiTokens.Muted,
                            fontFamily = poppins
                        )
                        Text(
                            text = scheduleText,
                            style = MaterialTheme.typography.titleMedium,
                            color = UiTokens.Text,
                            fontFamily = poppins,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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

            // Tombol aksi utama (ambil label sesuai izin)
            val canCheckIn = state.canCheckIn
            val canCheckOut = state.canCheckOut
            val primaryLabel = when {
                canCheckOut -> "Absen Pulang"
                canCheckIn -> "Absen Masuk"
                else -> "Tidak dapat absen"
            }
            val primaryColor = when {
                canCheckOut -> Color(0xFFE76F51) // merah lembut untuk pulang
                canCheckIn -> UiTokens.Accent     // hijau tema untuk masuk
                else -> Color(0xFFDADADA)
            }
            val primaryTextColor = if (canCheckOut || canCheckIn) Color.White else UiTokens.Text
            val primaryOnClick = {
                ensureLocationReady {
                    when {
                        canCheckOut -> onCheckOutClick()
                        canCheckIn -> onCheckInClick()
                        else -> Unit
                    }
                }
            }
            Button(
                onClick = primaryOnClick,
                enabled = !state.isLoading && (canCheckIn || canCheckOut),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 6.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = Color(0xFFE4E4E4),
                    contentColor = primaryTextColor,
                    disabledContentColor = UiTokens.Muted
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = UiTokens.Accent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = primaryLabel,
                    color = primaryTextColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = poppins
                )
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
    val ringSize = 260.dp
    val transition = rememberInfiniteTransition(label = "ring")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing)
        ),
        label = "ring-rotation"
    )
    val ringBrush = remember {
        Brush.sweepGradient(
            0f to UiTokens.Accent,
            0.5f to UiTokens.Accent.copy(alpha = 0.15f),
            1f to UiTokens.Accent
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(ringSize)
        ) {
            rotate(rotation) {
                drawArc(
                    brush = ringBrush,
                    startAngle = 0f,
                    sweepAngle = 300f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }
        Card(
            shape = RoundedCornerShape(180.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFDADADA)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.size(240.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFDADADA), RoundedCornerShape(180.dp)),
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
    val checkIn = state.checkInPhotoUrl?.takeIf { it.isNotBlank() }
    val checkOut = state.checkOutPhotoUrl?.takeIf { it.isNotBlank() }
    // Always prioritise foto absen pulang jika ada
    return checkOut ?: checkIn
}

private fun formatTime(value: String?, formatter: SimpleDateFormat): String {
    val millis = IsoTimeUtil.parseMillis(value)
    return if (millis == null) "--:--" else formatter.format(Date(millis))
}

private fun formatShiftTime(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank()) return null
    val millis = IsoTimeUtil.parseMillis(trimmed)
    if (millis != null) {
        return SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(millis))
    }
    val regex = Regex("^\\d{2}:\\d{2}(:\\d{2})?$")
    return if (regex.matches(trimmed)) trimmed.take(5) else trimmed
}

private fun buildScheduleText(state: AttendanceState): String {
    val start = formatShiftTime(state.shiftStart)
    val end = formatShiftTime(state.shiftEnd)
    return if (!start.isNullOrBlank() && !end.isNullOrBlank()) {
        "$start - $end"
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
