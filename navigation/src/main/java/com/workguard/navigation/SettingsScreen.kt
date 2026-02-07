package com.workguard.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onFaceEnrollmentClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var biometricEnabled by remember { mutableStateOf(true) }
    var autoSyncEnabled by remember { mutableStateOf(false) }
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
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = headerFont,
                        fontWeight = FontWeight.SemiBold,
                        color = UiTokens.Text
                    )
                    Text(
                        text = "Kelola profil, keamanan, dan preferensi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = UiTokens.Muted
                    )
                }
            }

            item { ProfileHeader(onClick = onProfileClick) }

            item {
                SectionHeader(title = "Akun")
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.AccountCircle,
                    title = "Profil",
                    subtitle = "Data karyawan dan jabatan",
                    onClick = onProfileClick
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = UiTokens.Muted
                    )
                }
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.VerifiedUser,
                    title = "Status kerja",
                    subtitle = "Aktif - Shift pagi"
                ) {
                    Text(
                        text = "Aktif",
                        style = MaterialTheme.typography.labelMedium,
                        color = UiTokens.Accent
                    )
                }
            }

            item {
                SectionHeader(title = "Keamanan")
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Lock,
                    title = "Ubah PIN",
                    subtitle = "Perbarui PIN keamanan"
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = UiTokens.Muted
                    )
                }
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Face,
                    title = "Face ID",
                    subtitle = "Gunakan wajah untuk masuk"
                ) {
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it }
                    )
                }
            }
            item {
                SectionHeader(title = "Pendaftaran Wajah")
            }
            item {
                FaceEnrollEntryCard(onClick = onFaceEnrollmentClick)
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Security,
                    title = "Privasi data",
                    subtitle = "Kelola izin dan akses"
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = UiTokens.Muted
                    )
                }
            }

            item {
                SectionHeader(title = "Notifikasi")
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifikasi utama",
                    subtitle = "Shift, laporan, dan tugas"
                ) {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Tune,
                    title = "Sinkron otomatis",
                    subtitle = "Jadwalkan sync harian"
                ) {
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = { autoSyncEnabled = it }
                    )
                }
            }

            item {
                SectionHeader(title = "Preferensi")
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Language,
                    title = "Bahasa",
                    subtitle = "Indonesia"
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = UiTokens.Muted
                    )
                }
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Kebijakan",
                    subtitle = "Syarat dan ketentuan"
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = UiTokens.Muted
                    )
                }
            }
            item {
                SettingsRow(
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    title = "Bantuan",
                    subtitle = "Pusat bantuan dan kontak"
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = UiTokens.Muted
                    )
                }
            }

            item {
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = UiTokens.Red)
                ) {
                    Text("Logout")
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
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
                    .background(UiTokens.Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "WG",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = UiTokens.AccentDark
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WorkGuard Officer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = UiTokens.Text
                )
                Text(
                    text = "officer@workguard.id",
                    style = MaterialTheme.typography.bodySmall,
                    color = UiTokens.Muted
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(UiTokens.Soft)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Verified",
                    style = MaterialTheme.typography.labelSmall,
                    color = UiTokens.AccentDark
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = UiTokens.Text
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(clickModifier)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(UiTokens.Accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = UiTokens.Accent
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = UiTokens.Text
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = UiTokens.Muted
                )
            }
            trailing()
        }
    }
}

@Composable
private fun FaceEnrollEntryCard(onClick: () -> Unit) {
    val avatarSize = 56.dp
    val iconSize = 24.dp
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pendaftaran wajah",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = UiTokens.Text
                )
                Text(
                    text = "Aktifkan Face ID.",
                    style = MaterialTheme.typography.bodySmall,
                    color = UiTokens.Muted
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = UiTokens.Muted
            )
        }
    }
}
