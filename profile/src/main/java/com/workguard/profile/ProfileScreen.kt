package com.workguard.profile

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.workguard.core.network.EmployeeProfile
import com.workguard.core.util.UrlUtil

@Composable
fun ProfileScreen(
    state: ProfileState,
    onRetry: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF4F7F8), Color(0xFFF4F7F8))
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
                Text(
                    text = "Profil Karyawan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2A30)
                )
            }

            if (state.isLoading && state.profile == null) {
                item {
                    Text(
                        text = "Memuat profil...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A878A)
                    )
                }
            }

            if (state.errorMessage != null) {
                item {
                    ErrorCard(message = state.errorMessage, onRetry = onRetry)
                }
            }

            val profile = state.profile
            if (profile != null) {
                item { ProfileHeader(profile = profile) }
                item {
                    SectionCard(title = "Informasi Pribadi") {
                        InfoRow("Nama lengkap", profile.fullName)
                        InfoRow("Kode karyawan", profile.employeeCode)
                        InfoRow("Jenis kelamin", profile.gender)
                        InfoRow("Tempat lahir", profile.birthPlace)
                        InfoRow("Tanggal lahir", profile.birthDate)
                        InfoRow("Nomor telepon", profile.phone)
                        InfoRow("Alamat", profile.address, multiline = true)
                    }
                }
                item {
                    SectionCard(title = "Pekerjaan") {
                        InfoRow("Peran", profile.role)
                        InfoRow("Tipe tugas", profile.taskType)
                        InfoRow("Status kerja", profile.employmentStatus)
                        InfoRow("Tanggal bergabung", profile.joinDate)
                        InfoRow("Departemen", profile.department?.name)
                        InfoRow("Posisi", profile.position?.name)
                    }
                }
                item {
                    SectionCard(title = "Cabang") {
                        InfoRow("Nama cabang", profile.branch?.name)
                        InfoRow("Kode cabang", profile.branch?.code)
                        InfoRow("Alamat cabang", profile.branch?.address, multiline = true)
                        InfoRow("Kota", profile.branch?.city)
                        InfoRow("Telepon", profile.branch?.phone)
                    }
                }
                item {
                    SectionCard(title = "Pengaturan") {
                        InfoRow("Kunci lokasi", profile.lockLocation?.toYesNo())
                        InfoRow("Kunci jam kerja", profile.lockWorkHours?.toYesNo())
                        InfoRow("Kunci login device", profile.lockDeviceLogin?.toYesNo())
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: EmployeeProfile) {
    val name = profile.fullName ?: "Karyawan"
    val photoUrl = UrlUtil.resolveAssetUrl(
        profile.employeePhotoUrl?.takeIf { it.isNotBlank() }
            ?: profile.userPhotoUrl?.takeIf { it.isNotBlank() }
    )
    val initials = name.split(" ")
        .filter { it.isNotBlank() }
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifBlank { "WG" }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF16B3A8).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl.isNullOrBlank()) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0C8D85)
                    )
                } else {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Foto karyawan",
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
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0C8D85)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2A30)
                )
                Text(
                    text = profile.role ?: "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A878A)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = profile.employeeCode ?: "-",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF16B3A8)
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2A30)
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?, multiline: Boolean = false) {
    if (multiline) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7A878A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value?.takeIf { it.isNotBlank() } ?: "-",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF1F2A30),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF7A878A),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "-",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF1F2A30),
            textAlign = TextAlign.End
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE9F7F6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF1F2A30),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16B3A8))
            ) {
                Text("Coba lagi")
            }
        }
    }
}

private fun Boolean?.toYesNo(): String {
    return if (this == true) "Ya" else "Tidak"
}
