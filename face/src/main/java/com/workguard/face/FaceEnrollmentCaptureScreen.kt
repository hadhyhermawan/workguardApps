package com.workguard.face

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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun FaceEnrollmentCaptureScreen(
    state: FaceEnrollmentState,
    onPhotoCaptured: (File) -> Unit,
    onClearError: () -> Unit,
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val gradient = Brush.verticalGradient(
        colors = listOf(FaceUiTokens.Soft, FaceUiTokens.Bg)
    )
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

    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) {
            onCompleted()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        if (!hasPermission) {
            PermissionCard(onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) })
        } else {
            CameraCaptureContent(
                state = state,
                onPhotoCaptured = onPhotoCaptured,
                onClearError = onClearError,
                context = context,
                lifecycleOwner = lifecycleOwner
            )
        }
    }
}

@Composable
private fun CameraCaptureContent(
    state: FaceEnrollmentState,
    onPhotoCaptured: (File) -> Unit,
    onClearError: () -> Unit,
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
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
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()
            val hasFront = runCatching { provider.hasCamera(selector) }.getOrDefault(false)
            if (!hasFront) {
                cameraError = "Kamera depan tidak tersedia"
            }
            val resolvedSelector = if (hasFront) selector else CameraSelector.DEFAULT_BACK_CAMERA
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
            } else {
                cameraError = "Preview kamera tidak tersedia"
            }
        }
    }

    DisposableEffect(cameraProvider) {
        onDispose { cameraProvider?.unbindAll() }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Foto ${state.currentSlot} dari ${state.totalSlots}",
                style = MaterialTheme.typography.titleMedium,
                color = FaceUiTokens.Text
            )
            LinearProgressIndicator(
                progress = { state.progress },
                color = FaceUiTokens.Accent,
                trackColor = FaceUiTokens.Divider,
                modifier = Modifier.fillMaxWidth()
            )
            if (cameraError != null) {
                ErrorCard(message = cameraError ?: "Gagal membuka kamera") {
                    cameraError = null
                }
            }
            if (state.errorMessage != null) {
                ErrorCard(message = state.errorMessage) {
                    onClearError()
                }
            }
            Button(
                onClick = {
                    if (state.isLoading || isCapturing) return@Button
                    onClearError()
                    cameraError = null
                    isCapturing = true
                    val outputDir = File(context.cacheDir, "face-templates").apply { mkdirs() }
                    val file = File(
                        outputDir,
                        "face_slot_${state.currentSlot}_${System.currentTimeMillis()}.jpg"
                    )
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
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && !isCapturing && !state.isCompleted,
                colors = ButtonDefaults.buttonColors(containerColor = FaceUiTokens.AccentDark)
            ) {
                Text(if (state.isLoading) "Mengirim..." else "Ambil foto")
            }
        }
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = FaceUiTokens.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Izin kamera dibutuhkan",
                style = MaterialTheme.typography.titleMedium,
                color = FaceUiTokens.Text
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Aktifkan kamera untuk melanjutkan pendaftaran wajah.",
                style = MaterialTheme.typography.bodySmall,
                color = FaceUiTokens.Muted
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRequest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FaceUiTokens.Accent)
            ) {
                Text("Izinkan Kamera")
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FaceUiTokens.Soft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = FaceUiTokens.Text,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = FaceUiTokens.Accent)
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

private object FaceUiTokens {
    val Accent = androidx.compose.ui.graphics.Color(0xFF16B3A8)
    val AccentDark = androidx.compose.ui.graphics.Color(0xFF0C8D85)
    val Bg = androidx.compose.ui.graphics.Color(0xFFF4F7F8)
    val Divider = androidx.compose.ui.graphics.Color(0xFFE2E6E8)
    val Muted = androidx.compose.ui.graphics.Color(0xFF7A878A)
    val Soft = androidx.compose.ui.graphics.Color(0xFFE9F7F6)
    val Surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    val Text = androidx.compose.ui.graphics.Color(0xFF1F2A30)
}
