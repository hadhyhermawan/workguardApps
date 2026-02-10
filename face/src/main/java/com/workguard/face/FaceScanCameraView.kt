package com.workguard.face

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.FileOutputStream
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
internal fun FaceScanCameraView(
    modifier: Modifier = Modifier,
    accent: Color,
    accentDark: Color,
    surface: Color,
    textColor: Color,
    muted: Color,
    captureSignal: Long,
    onBack: (() -> Unit)? = null,
    onFaceDetected: (Boolean) -> Unit,
    onFaceCount: (Int) -> Unit,
    onPhotoCaptured: (File?, String?) -> Unit
) {
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
        FaceScanPermissionCard(
            modifier = modifier,
            accent = accent,
            surface = surface,
            textColor = textColor,
            muted = muted,
            onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
        )
        return
    }

    FaceScanCameraPreview(
        modifier = modifier,
        accent = accent,
        accentDark = accentDark,
        surface = surface,
        textColor = textColor,
        captureSignal = captureSignal,
        onBack = onBack,
        onFaceDetected = onFaceDetected,
        onFaceCount = onFaceCount,
        onPhotoCaptured = onPhotoCaptured
    )
}

@Composable
private fun FaceScanCameraPreview(
    modifier: Modifier = Modifier,
    accent: Color,
    accentDark: Color,
    surface: Color,
    textColor: Color,
    captureSignal: Long,
    onBack: (() -> Unit)?,
    onFaceDetected: (Boolean) -> Unit,
    onFaceCount: (Int) -> Unit,
    onPhotoCaptured: (File?, String?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(95)
            .build()
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
    val captureDetector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
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
    var isCapturing by remember { mutableStateOf(false) }
    var faceCount by remember { mutableStateOf(0) }

    LaunchedEffect(faceDetected) {
        onFaceDetected(faceDetected)
    }

    LaunchedEffect(faceCount) {
        onFaceCount(faceCount)
    }

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
                                val count = faces.size
                                mainExecutor.execute {
                                    faceDetected = count > 0
                                    faceCount = count
                                }
                            }
                            .addOnFailureListener {
                                mainExecutor.execute {
                                    faceDetected = false
                                    faceCount = 0
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
            provider.bindToLifecycle(lifecycleOwner, resolvedSelector, preview, imageAnalysis, imageCapture)
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

    DisposableEffect(Unit) {
        onDispose {
            detector.close()
            captureDetector.close()
            analysisExecutor.shutdown()
        }
    }

    LaunchedEffect(captureSignal, cameraProvider) {
        if (captureSignal <= 0L) return@LaunchedEffect
        if (isCapturing) return@LaunchedEffect
        if (cameraProvider == null) {
            onPhotoCaptured(null, "Kamera belum siap")
            return@LaunchedEffect
        }
        if (cameraError != null) {
            onPhotoCaptured(null, cameraError)
            return@LaunchedEffect
        }
        if (faceCount != 1) {
            onPhotoCaptured(null, "Pastikan hanya 1 wajah di dalam frame")
            return@LaunchedEffect
        }
        isCapturing = true
        val outputDir = File(context.cacheDir, "face-scan").apply { mkdirs() }
        val file = File(outputDir, "scan_${System.currentTimeMillis()}.jpg")
        val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
        imageCapture.targetRotation = rotation
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            outputOptions,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    validateAndCropFace(context, captureDetector, file) { resultFile, message ->
                        isCapturing = false
                        if (resultFile != null) {
                            onPhotoCaptured(resultFile, null)
                        } else {
                            file.delete()
                            onPhotoCaptured(null, message ?: "Pastikan hanya 1 wajah di dalam frame")
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    onPhotoCaptured(null, "Gagal mengambil foto")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .size(44.dp)
                    .background(
                        color = accent.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        FaceScanOverlay(
            accent = accent,
            accentDark = accentDark,
            faceDetected = faceDetected,
            onBack = onBack
        )
        if (cameraError != null) {
            FaceScanCameraError(
                message = cameraError ?: "Gagal membuka kamera",
                surface = surface,
                textColor = textColor,
                accent = accent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
            ) {
                cameraError = null
            }
        }
    }
}

private fun validateAndCropFace(
    context: Context,
    detector: FaceDetector,
    file: File,
    onResult: (File?, String?) -> Unit
) {
    val image = runCatching {
        InputImage.fromFilePath(context, Uri.fromFile(file))
    }.getOrNull()
    if (image == null) {
        onResult(null, "Gagal memproses foto")
        return
    }
    detector.process(image)
        .addOnSuccessListener { faces ->
            when {
                faces.size == 1 -> {
                    val bounds = faces.first().boundingBox
                    val cropped = runCatching { cropFaceToFile(file, bounds) }.getOrDefault(false)
                    if (cropped) {
                        onResult(file, null)
                    } else {
                        onResult(null, "Gagal memproses foto")
                    }
                }
                faces.isEmpty() -> onResult(null, "Wajah tidak terdeteksi")
                else -> onResult(null, "Pastikan hanya 1 wajah di dalam frame")
            }
        }
        .addOnFailureListener {
            onResult(null, "Gagal memproses foto")
        }
}

private fun cropFaceToFile(file: File, bounds: android.graphics.Rect): Boolean {
    val original = BitmapFactory.decodeFile(file.absolutePath) ?: return false
    val rotated = applyExifRotation(original, file.absolutePath)

    val paddingX = (bounds.width() * 0.25f).toInt()
    val paddingY = (bounds.height() * 0.35f).toInt()
    val left = (bounds.left - paddingX).coerceAtLeast(0)
    val top = (bounds.top - paddingY).coerceAtLeast(0)
    val right = (bounds.right + paddingX).coerceAtMost(rotated.width)
    val bottom = (bounds.bottom + paddingY).coerceAtMost(rotated.height)
    if (right <= left || bottom <= top) {
        if (rotated != original) rotated.recycle()
        if (original != rotated) original.recycle()
        return false
    }

    val cropped = Bitmap.createBitmap(rotated, left, top, right - left, bottom - top)
    if (rotated != original) rotated.recycle()
    if (original != rotated) original.recycle()

    FileOutputStream(file).use { stream ->
        cropped.compress(Bitmap.CompressFormat.JPEG, 95, stream)
    }
    cropped.recycle()

    val exif = ExifInterface(file.absolutePath)
    exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
    exif.saveAttributes()
    return true
}

private fun applyExifRotation(bitmap: Bitmap, path: String): Bitmap {
    val exif = ExifInterface(path)
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
    }
    return if (matrix.isIdentity) {
        bitmap
    } else {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}

@Composable
private fun FaceScanOverlay(
    accent: Color,
    accentDark: Color,
    faceDetected: Boolean,
    onBack: (() -> Unit)? = null
) {
    val ringColor = if (faceDetected) accent else accentDark
    Box(modifier = Modifier.fillMaxSize()) {
        InwardRoundedFrame(
            modifier = Modifier
                .align(Alignment.Center)
                .size(176.dp),
            color = ringColor,
            strokeWidth = 4.dp,
            cornerRadius = 28.dp
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = accent.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(6.dp)
                        .clickable { onBack() }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (faceDetected) "Wajah terdeteksi" else "Arahkan wajah",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun InwardRoundedFrame(
    modifier: Modifier,
    color: Color,
    strokeWidth: Dp = 3.dp,
    cornerRadius: Dp = 24.dp
) {
    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val inset = strokePx / 2f
        val left = inset
        val top = inset
        val right = size.width - inset
        val bottom = size.height - inset
        val maxRadius = ((size.minDimension - strokePx) / 2f).coerceAtLeast(0f)
        val r = cornerRadius.toPx().coerceAtMost(maxRadius)
        val availableHorizontal = (right - left) - (2f * r)
        val availableVertical = (bottom - top) - (2f * r)
        val maxCornerLen = minOf(availableHorizontal, availableVertical).coerceAtLeast(0f) / 2f
        val cornerLen = (size.minDimension * 0.22f).coerceIn(0f, maxCornerLen)

        val stroke = Stroke(
            width = strokePx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        fun cornerPath(builder: Path.() -> Unit): Path = Path().apply(builder)

        // Top-left corner
        drawPath(
            path = cornerPath {
                moveTo(left, top + r + cornerLen)
                lineTo(left, top + r)
                arcTo(Rect(left, top, left + (2f * r), top + (2f * r)), 180f, 90f, false)
                lineTo(left + r + cornerLen, top)
            },
            color = color,
            style = stroke
        )

        // Top-right corner
        drawPath(
            path = cornerPath {
                moveTo(right - r - cornerLen, top)
                lineTo(right - r, top)
                arcTo(Rect(right - (2f * r), top, right, top + (2f * r)), 270f, 90f, false)
                lineTo(right, top + r + cornerLen)
            },
            color = color,
            style = stroke
        )

        // Bottom-right corner
        drawPath(
            path = cornerPath {
                moveTo(right, bottom - r - cornerLen)
                lineTo(right, bottom - r)
                arcTo(
                    Rect(right - (2f * r), bottom - (2f * r), right, bottom),
                    0f,
                    90f,
                    false
                )
                lineTo(right - r - cornerLen, bottom)
            },
            color = color,
            style = stroke
        )

        // Bottom-left corner
        drawPath(
            path = cornerPath {
                moveTo(left + r + cornerLen, bottom)
                lineTo(left + r, bottom)
                arcTo(Rect(left, bottom - (2f * r), left + (2f * r), bottom), 90f, 90f, false)
                lineTo(left, bottom - r - cornerLen)
            },
            color = color,
            style = stroke
        )
    }
}

@Composable
private fun FaceScanPermissionCard(
    modifier: Modifier,
    accent: Color,
    surface: Color,
    textColor: Color,
    muted: Color,
    onRequest: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Izin kamera dibutuhkan",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Aktifkan kamera untuk mulai scan wajah.",
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRequest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Izinkan Kamera")
            }
        }
    }
}

@Composable
private fun FaceScanCameraError(
    message: String,
    surface: Color,
    textColor: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
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
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
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
