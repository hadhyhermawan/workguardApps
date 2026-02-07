package com.workguard.face

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.workguard.core.model.enums.CameraFacing
import java.io.File

@Composable
fun FaceScanScreen(
    state: FaceScanState,
    onConfirmScan: (CameraFacing, File) -> Unit
) {
    val accent = Color(0xFF16B3A8)
    val accentDark = Color(0xFF0E8C84)
    val muted = Color(0xFF6D7A7E)
    val textColor = Color(0xFF1F2A30)
    var faceDetected by remember { mutableStateOf(false) }
    var faceCount by remember { mutableStateOf(0) }
    var captureSignal by remember { mutableStateOf(0L) }
    var isCapturing by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        FaceScanCameraView(
            modifier = Modifier.fillMaxSize(),
            accent = accent,
            accentDark = accentDark,
            surface = Color.Black,
            textColor = textColor,
            muted = muted,
            captureSignal = captureSignal,
            onFaceDetected = { detected -> faceDetected = detected },
            onFaceCount = { count -> faceCount = count },
            onPhotoCaptured = { file, error ->
                if (error != null) {
                    captureError = error
                    isCapturing = false
                    return@FaceScanCameraView
                }
                if (file == null) {
                    captureError = "Gagal mengambil foto"
                    isCapturing = false
                    return@FaceScanCameraView
                }
                captureError = null
                isCapturing = false
                onConfirmScan(CameraFacing.FRONT, file)
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = if (faceDetected) accent else muted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            faceCount == 1 -> "Wajah terdeteksi"
                            faceCount > 1 -> "Terdeteksi $faceCount wajah, hanya 1 orang."
                            else -> "Arahkan wajah ke tengah bingkai."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (faceDetected) accent else muted
                    )
                }

                Button(
                    onClick = {
                        if (state.isLoading || isCapturing) return@Button
                        if (faceCount != 1) {
                            captureError = "Pastikan hanya 1 wajah di dalam frame"
                            return@Button
                        }
                        captureError = null
                        isCapturing = true
                        captureSignal += 1L
                    },
                    enabled = faceCount == 1 && !state.isLoading && !isCapturing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    if (state.isLoading || isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (state.isLoading) "Memproses" else "Mengambil foto")
                    } else {
                        Text("Konfirmasi scan")
                    }
                }

                val errorMessage = captureError ?: state.errorMessage
                if (errorMessage != null) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F2)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB42318),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
