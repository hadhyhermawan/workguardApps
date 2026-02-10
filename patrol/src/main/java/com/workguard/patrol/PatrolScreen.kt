package com.workguard.patrol

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workguard.patrol.R
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.workguard.patrol.model.PatrolPoint
import java.io.File
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun PatrolScreen(
    state: PatrolState,
    onStartPatrol: () -> Unit,
    onPhotoCaptured: (File) -> Unit,
    onCancelCapture: () -> Unit,
    onClearError: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val bg = Color(0xFFF4F7F8)
    val surface = Color(0xFFFFFFFF)
    val accent = Color(0xFF16B3A8)
    val muted = Color(0xFF7A878A)
    val poppins = remember {
        FontFamily(
            Font(com.workguard.patrol.R.font.poppins_regular, FontWeight.Normal),
            Font(com.workguard.patrol.R.font.poppins_semibold, FontWeight.SemiBold)
        )
    }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale("id", "ID")) }
    val dateFormatter = remember { SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")) }
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
    val sessionActive = state.patrolSessionId != null
    val sessionLimitReached = state.completedSessions >= state.maxSessionsPerShift
    val remainingPoints = state.remainingPoints ?: state.points.count { !it.isScanned }
    val totalPoints = state.points.size
    val nextPoint = state.points.firstOrNull { !it.isScanned }
    val sessionIndex = if (sessionActive) {
        state.completedSessions + 1
    } else {
        state.completedSessions
    }
    val statusText = when {
        sessionActive -> "Sesi aktif"
        sessionLimitReached -> "Batas sesi tercapai"
        state.completedSessions > 0 -> "Sesi selesai"
        else -> "Belum dimulai"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        if (nextPoint != null) {
            PatrolCaptureScreen(
                point = nextPoint,
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                onPhotoCaptured = onPhotoCaptured,
                onCancel = onCancelCapture,
                onClearError = onClearError
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color(0xFF1F2A30),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Patroli",
                        color = Color(0xFF1F2A30),
                        fontFamily = poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                }
            }
            Text(
                text = timeText,
                color = Color(0xFF1F2A30),
                fontFamily = poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 36.sp,
                letterSpacing = 0.5.sp
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
                fontFamily = poppins,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(12.dp))
            PatrolAvatarRing()
            Spacer(modifier = Modifier.height(14.dp))

            if (state.errorMessage != null) {
                InfoCard(
                    message = state.errorMessage,
                    onDismiss = onClearError,
                    containerColor = Color(0xFFFFF1F1),
                    textColor = Color(0xFFB42318)
                )
            }

            if (state.statusMessage != null) {
                InfoCard(
                    message = state.statusMessage,
                    onDismiss = onClearError,
                    containerColor = Color(0xFFE9F7F6),
                    textColor = Color(0xFF0E8C84)
                )
            }

            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Status Patroli",
                        style = MaterialTheme.typography.labelSmall,
                        color = muted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1F2A30)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sesi $sessionIndex/${state.maxSessionsPerShift}",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                    if (sessionActive && totalPoints > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sisa $remainingPoints dari $totalPoints titik",
                            style = MaterialTheme.typography.bodySmall,
                            color = muted
                        )
                    }
                    if (sessionLimitReached) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Batas patroli per shift sudah tercapai.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB42318)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    PatrolScheduleChips(
                        slots = remember { buildPatrolSlots() },
                        accent = accent,
                        surface = surface,
                        poppins = poppins
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val canStart = !state.isLoading && !sessionActive && !sessionLimitReached
                    val btnColor = if (canStart) accent else Color(0xFFE4E4E4)
                    val btnTextColor = if (canStart) Color.White else muted
                    Button(
                        onClick = onStartPatrol,
                        enabled = canStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = btnColor)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "Mulai/Absen Patroli",
                            color = btnTextColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Daftar titik disembunyikan; kamera akan otomatis berjalan berurutan
        }
    }
}
}

@Composable
private fun PatrolPointCard(
    point: PatrolPoint,
    accent: Color,
    muted: Color,
    surface: Color,
    isSessionActive: Boolean,
    isNext: Boolean,
    onScan: () -> Unit
) {
    val canScan = isSessionActive && isNext && !point.isScanned
    val actionLabel = when {
        point.isScanned -> "Sudah diambil"
        !isSessionActive -> "Mulai sesi dulu"
        !isNext -> "Menunggu giliran"
        else -> "Scan Titik"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = accent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = point.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF1F2A30)
                )
                if (point.isScanned) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (!point.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = point.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onScan,
                enabled = canScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    disabledContainerColor = Color(0xFFB0B9BC)
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun PatrolCaptureScreen(
    point: PatrolPoint,
    isLoading: Boolean,
    errorMessage: String?,
    onPhotoCaptured: (File) -> Unit,
    onCancel: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasPermission) {
            InfoCard(
                message = "Izin kamera dibutuhkan untuk foto titik patroli.",
                onDismiss = {},
                containerColor = Color(0xFFFFFFFF),
                textColor = Color(0xFF1F2A30),
                actionLabel = "Izinkan",
                onAction = { permissionLauncher.launch(Manifest.permission.CAMERA) }
            )
        } else {
            PatrolCameraPreview(
                pointName = point.name,
                context = context,
                lifecycleOwner = lifecycleOwner,
                onPhotoCaptured = onPhotoCaptured,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onClearError = onClearError,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun PatrolAvatarRing() {
    val ringSize = 220.dp
    val transition = rememberInfiniteTransition(label = "patrol-ring")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing)
        ),
        label = "patrol-rot"
    )
    val brush = remember {
        Brush.sweepGradient(
            0f to Color(0xFF16B3A8),
            0.5f to Color(0x3316B3A8),
            1f to Color(0xFF16B3A8)
        )
    }
    Box(contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(ringSize)
        ) {
            rotate(rotation) {
                drawArc(
                    brush = brush,
                    startAngle = 0f,
                    sweepAngle = 300f,
                    useCenter = false,
                    style = Stroke(width = 5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }
        Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(160.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFDADADA)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.size(180.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFDADADA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFF6D7A7E),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
private fun PatrolScheduleChips(
    slots: List<String>,
    accent: Color,
    surface: Color,
    poppins: FontFamily
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(slots.size) { idx ->
            val slot = slots[idx]
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = 0.4f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                    )
                    .background(
                        color = surface,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = slot,
                    color = Color(0xFF1F2A30),
                    fontFamily = poppins,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun buildPatrolSlots(): List<String> {
    val slots = mutableListOf<String>()
    var hour = 0
    while (hour < 24) {
        slots += String.format(Locale("id", "ID"), "%02d:00", hour)
        hour += 2
    }
    return slots
}

@Composable
private fun PatrolCameraPreview(
    pointName: String,
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onPhotoCaptured: (File) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onCancel: () -> Unit
) {
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var useCompatibleMode by remember { mutableStateOf(true) }
    var triedFallback by remember { mutableStateOf(false) }
    var streamState by remember { mutableStateOf(PreviewView.StreamState.IDLE) }

    DisposableEffect(previewView, lifecycleOwner) {
        val observer = Observer<PreviewView.StreamState> { state ->
            if (state != null) {
                streamState = state
            }
        }
        previewView.previewStreamState.observe(lifecycleOwner, observer)
        onDispose { previewView.previewStreamState.removeObserver(observer) }
    }

    LaunchedEffect(lifecycleOwner, previewView, useCompatibleMode) {
        try {
            val provider = context.awaitCameraProvider()
            previewView.implementationMode = if (useCompatibleMode) {
                PreviewView.ImplementationMode.COMPATIBLE
            } else {
                PreviewView.ImplementationMode.PERFORMANCE
            }
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val selector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
            val hasBack = runCatching { provider.hasCamera(selector) }.getOrDefault(false)
            if (!hasBack) {
                cameraError = "Kamera belakang tidak tersedia"
            }
            val resolvedSelector = if (hasBack) selector else CameraSelector.DEFAULT_BACK_CAMERA
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, resolvedSelector, preview, imageCapture)
            cameraProvider = provider
        } catch (e: Exception) {
            cameraError = "Gagal membuka kamera"
        }
    }

    LaunchedEffect(cameraProvider, useCompatibleMode, streamState) {
        if (cameraProvider == null) return@LaunchedEffect
        if (streamState == PreviewView.StreamState.STREAMING) return@LaunchedEffect
        delay(1200)
        if (streamState != PreviewView.StreamState.STREAMING) {
            if (!triedFallback) {
                triedFallback = true
                useCompatibleMode = !useCompatibleMode
            } else if (cameraError == null) {
                cameraError = "Preview kamera tidak tersedia"
            }
        }
    }

    DisposableEffect(cameraProvider) {
        onDispose { cameraProvider?.unbindAll() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0x66000000), shape = androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Foto titik $pointName",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }

        if (cameraError != null) {
            InfoCard(
                message = cameraError ?: "Gagal membuka kamera",
                onDismiss = { cameraError = null },
                containerColor = Color(0xFFFFFFFF),
                textColor = Color(0xFF1F2A30),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 96.dp)
            )
        } else if (errorMessage != null) {
            InfoCard(
                message = errorMessage,
                onDismiss = onClearError,
                containerColor = Color(0xFFFFF1F1),
                textColor = Color(0xFFB42318),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 96.dp)
            )
        }

        Button(
            onClick = {
                if (isLoading || isCapturing) return@Button
                cameraError = null
                isCapturing = true
                val outputDir = File(context.cacheDir, "patrol-media").apply { mkdirs() }
                val file = File(outputDir, "patrol_${System.currentTimeMillis()}.jpg")
                val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
                imageCapture.targetRotation = rotation
                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            isCapturing = false
                            onPhotoCaptured(file)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            isCapturing = false
                            cameraError = "Gagal mengambil foto"
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .height(52.dp),
            enabled = !isLoading && !isCapturing,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16B3A8))
        ) {
            Text(if (isLoading) "Mengirim..." else "Ambil Foto")
        }

        if (!isCapturing && !isLoading && cameraError == null && errorMessage == null && state.statusMessage?.contains("Berhasil", ignoreCase = true) == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(96.dp)
                    .background(Color(0x8000C853), shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Berhasil",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    message: String,
    onDismiss: () -> Unit,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16B3A8))
                ) {
                    Text(actionLabel)
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16B3A8))
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

private suspend fun Context.awaitCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                try {
                    cont.resume(future.get())
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }
