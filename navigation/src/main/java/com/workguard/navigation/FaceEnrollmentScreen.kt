package com.workguard.navigation

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.workguard.face.FaceEnrollmentState
import kotlin.math.roundToInt

@Composable
fun FaceEnrollmentScreen(
    state: FaceEnrollmentState,
    onStartEnrollment: () -> Unit = {}
) {
    val headerFont = FontFamily.Serif
    val gradient = Brush.verticalGradient(
        colors = listOf(UiTokens.Soft, UiTokens.Bg)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Pendaftaran Wajah",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = headerFont,
                        fontWeight = FontWeight.SemiBold,
                        color = UiTokens.Text
                    )
                    Text(
                        text = "Aktifkan Face ID dengan pendaftaran wajah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = UiTokens.Muted
                    )
                }
            }

            item {
                FaceEnrollPanel(
                    state = state,
                    onStartEnrollment = onStartEnrollment
                )
            }

            item {
                FaceEnrollChecklist()
            }
        }
    }
}

@Composable
private fun FaceEnrollPanel(
    state: FaceEnrollmentState,
    onStartEnrollment: () -> Unit
) {
    val avatarSize = 56.dp
    val iconSize = 24.dp
    val percent = (state.progress * 100f).roundToInt().coerceIn(0, 100)
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(UiTokens.Accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Face,
                        contentDescription = null,
                        tint = UiTokens.Accent,
                        modifier = Modifier.size(iconSize)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pendaftaran wajah",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = UiTokens.Text
                    )
                    Text(
                        text = "3 langkah singkat, estimasi 1 menit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = UiTokens.Muted
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                color = UiTokens.Accent,
                trackColor = UiTokens.Divider,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Progress pendaftaran ${percent}%",
                style = MaterialTheme.typography.labelSmall,
                color = UiTokens.Muted
            )
            if (state.isCompleted) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pendaftaran selesai",
                    style = MaterialTheme.typography.bodySmall,
                    color = UiTokens.AccentDark
                )
            }
            if (!state.isCompleted) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onStartEnrollment,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = UiTokens.AccentDark)
                ) {
                    Text("Mulai pendaftaran")
                }
            }
        }
    }
}

@Composable
private fun FaceEnrollChecklist() {
    Column {
        Text(
            text = "Persiapan pendaftaran",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = UiTokens.Text
        )
        Spacer(modifier = Modifier.height(10.dp))
        FaceChecklistRow("Wajah terlihat jelas tanpa penutup.")
        Spacer(modifier = Modifier.height(8.dp))
        FaceChecklistRow("Pencahayaan merata dari depan.")
        Spacer(modifier = Modifier.height(8.dp))
        FaceChecklistRow("Ikuti arahan posisi kepala di layar.")
    }
}

@Composable
private fun FaceChecklistRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = UiTokens.AccentDark,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = UiTokens.Muted
        )
    }
}
