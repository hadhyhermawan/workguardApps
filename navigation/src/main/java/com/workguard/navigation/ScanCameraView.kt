package com.workguard.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
internal fun ScanCameraView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
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

    if (!hasPermission) {
        ScanPermissionCard(
            modifier = modifier,
            onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
        )
        return
    }

    ScanCameraPreview(modifier = modifier)
}

@Composable
private fun ScanCameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val lastAnalyzedAt = remember { AtomicLong(0L) }
    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .enableTracking()
                .build()
        )
    }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    var faceDetected by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
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
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        val now = System.currentTimeMillis()
                        if (now - lastAnalyzedAt.get() < 220L) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        lastAnalyzedAt.set(now)
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val rotation = imageProxy.imageInfo.rotationDegrees
                        val image = InputImage.fromMediaImage(mediaImage, rotation)
                        detector.process(image)
                            .addOnSuccessListener { faces ->
                                mainExecutor.execute {
                                    faceDetected = faces.isNotEmpty()
                                }
                            }
                            .addOnFailureListener {
                                mainExecutor.execute {
                                    faceDetected = false
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    }
                }
            imageAnalysis.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
            val selector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()
            val hasFront = runCatching { provider.hasCamera(selector) }.getOrDefault(false)
            if (!hasFront) {
                cameraError = "Kamera depan tidak tersedia"
            }
            val resolvedSelector = if (hasFront) selector else CameraSelector.DEFAULT_BACK_CAMERA
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, resolvedSelector, preview, imageAnalysis)
            cameraProvider = provider
        } catch (e: Exception) {
            cameraError = "Gagal membuka kamera"
        }
    }

    LaunchedEffect(cameraProvider, useCompatibleMode, streamState) {
        if (cameraProvider == null) return@LaunchedEffect
        if (streamState == PreviewView.StreamState.STREAMING) return@LaunchedEffect
        kotlinx.coroutines.delay(1200)
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

    DisposableEffect(Unit) {
        onDispose {
            detector.close()
            analysisExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        ScanFaceOverlay(faceDetected = faceDetected)
        if (cameraError != null) {
            ScanCameraError(
                message = cameraError ?: "Gagal membuka kamera",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
            ) {
                cameraError = null
            }
        }
    }
}

@Composable
private fun ScanFaceOverlay(faceDetected: Boolean) {
    val ringColor = if (faceDetected) UiTokens.Accent else UiTokens.AccentDark
    Box(modifier = Modifier.fillMaxSize()) {
        CornerFrame(
            modifier = Modifier
                .align(Alignment.Center)
                .size(170.dp),
            color = UiTokens.Accent.copy(alpha = 0.55f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            CornerFrame(
                modifier = Modifier.fillMaxSize(),
                color = ringColor
            )
            Icon(
                imageVector = Icons.Outlined.Face,
                contentDescription = null,
                tint = ringColor,
                modifier = Modifier.size(32.dp)
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.VerifiedUser,
                contentDescription = null,
                tint = UiTokens.Accent
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (faceDetected) "Wajah terdeteksi" else "Arahkan wajah",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CornerFrame(
    modifier: Modifier,
    color: Color,
    strokeWidth: Dp = 3.dp,
    cornerLengthFraction: Float = 0.22f
) {
    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val corner = size.minDimension * cornerLengthFraction
        val maxX = size.width
        val maxY = size.height
        val cap = StrokeCap.Square

        drawLine(color, Offset(0f, 0f), Offset(corner, 0f), strokePx, cap)
        drawLine(color, Offset(0f, 0f), Offset(0f, corner), strokePx, cap)

        drawLine(color, Offset(maxX, 0f), Offset(maxX - corner, 0f), strokePx, cap)
        drawLine(color, Offset(maxX, 0f), Offset(maxX, corner), strokePx, cap)

        drawLine(color, Offset(0f, maxY), Offset(corner, maxY), strokePx, cap)
        drawLine(color, Offset(0f, maxY), Offset(0f, maxY - corner), strokePx, cap)

        drawLine(color, Offset(maxX, maxY), Offset(maxX - corner, maxY), strokePx, cap)
        drawLine(color, Offset(maxX, maxY), Offset(maxX, maxY - corner), strokePx, cap)
    }
}

@Composable
private fun ScanPermissionCard(modifier: Modifier, onRequest: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Izin kamera dibutuhkan",
                style = MaterialTheme.typography.titleMedium,
                color = UiTokens.Text
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Aktifkan kamera untuk mulai scan wajah.",
                style = MaterialTheme.typography.bodySmall,
                color = UiTokens.Muted
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRequest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = UiTokens.Accent)
            ) {
                Text("Izinkan Kamera")
            }
        }
    }
}

@Composable
private fun ScanCameraError(
    message: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = UiTokens.Text,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = UiTokens.Accent)
            ) {
                Text("Tutup")
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
