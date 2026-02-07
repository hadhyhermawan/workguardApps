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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ChatScreen(
    state: ChatState,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(UiTokens.Bg, UiTokens.Soft)
    )
    val messages = state.messages
    val canSend = state.threadId != null && state.input.trim().isNotEmpty() && !state.isSending

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 80.dp, y = (-60).dp)
                        .background(UiTokens.Accent.copy(alpha = 0.08f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-80).dp, y = 70.dp)
                        .background(UiTokens.Blue.copy(alpha = 0.06f), CircleShape)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    reverseLayout = true
                ) {
                    items(messages.asReversed(), key = { it.id }) { message ->
                        MessageRow(message = message)
                    }
                }
            }
            if (state.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(UiTokens.Soft, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = UiTokens.Text
                    )
                }
            }
            ChatComposer(
                value = state.input,
                onValueChange = onInputChange,
                onAddClick = {},
                onImageClick = {},
                onFileClick = {},
                onVideoClick = {},
                onSendClick = onSendClick,
                enabled = canSend,
                isSending = state.isSending
            )
        }
    }
}

@Composable
private fun MessageRow(message: ChatUiMessage) {
    if (message.type == MessageType.System) {
        SystemMessageChip(text = message.text)
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!message.isMine) {
            ParticipantAvatar(initials = message.sender.take(2).uppercase())
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
        ) {
            if (!message.isMine) {
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.labelSmall,
                    color = UiTokens.AccentDark,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            MessageBubble(message = message)
            Spacer(modifier = Modifier.height(4.dp))
            MessageMeta(time = message.time, isMine = message.isMine)
        }
    }
}

@Composable
private fun SystemMessageChip(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = UiTokens.Muted,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(UiTokens.Surface)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ParticipantAvatar(initials: String) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(UiTokens.Blue.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = UiTokens.Blue
        )
    }
}

@Composable
private fun MessageBubble(message: ChatUiMessage) {
    val bubbleBrush = if (message.isMine) {
        Brush.linearGradient(colors = listOf(UiTokens.Accent, UiTokens.AccentDark))
    } else {
        Brush.linearGradient(colors = listOf(UiTokens.Surface, UiTokens.Surface))
    }
    val textColor = if (message.isMine) Color.White else UiTokens.Text
    val shape = if (message.isMine) {
        RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
    }

    Column(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .background(bubbleBrush, shape)
            .padding(12.dp)
    ) {
        when (message.type) {
            MessageType.Text -> {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
            MessageType.Image -> {
                MediaPreview(
                    label = "Foto",
                    icon = Icons.Outlined.Image,
                    accent = UiTokens.Blue
                )
                if (message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                }
            }
            MessageType.Video -> {
                MediaPreview(
                    label = "Video",
                    icon = Icons.Outlined.PlayArrow,
                    accent = UiTokens.Orange
                )
                if (message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                }
            }
            MessageType.File -> {
                FilePreview(
                    name = message.attachmentName,
                    meta = message.attachmentMeta,
                    isMine = message.isMine
                )
            }
            MessageType.System -> Unit
        }
    }
}

@Composable
private fun MediaPreview(label: String, icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(accent.copy(alpha = 0.25f), UiTokens.Soft)
                )
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .align(Alignment.Center)
                .size(32.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = UiTokens.Text,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(UiTokens.Surface.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun FilePreview(name: String, meta: String, isMine: Boolean) {
    val background = if (isMine) Color.White.copy(alpha = 0.18f) else UiTokens.Soft
    val textColor = if (isMine) Color.White else UiTokens.Text
    val secondaryColor = if (isMine) Color.White.copy(alpha = 0.8f) else UiTokens.Muted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (isMine) 0.25f else 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = if (isMine) Color.White else UiTokens.Accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (name.isBlank()) "Dokumen" else name,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (meta.isBlank()) "Lampiran file" else meta,
                style = MaterialTheme.typography.labelSmall,
                color = secondaryColor
            )
        }
        Icon(
            imageVector = Icons.Outlined.AttachFile,
            contentDescription = null,
            tint = if (isMine) Color.White else UiTokens.Muted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun MessageMeta(time: String, isMine: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = if (isMine) UiTokens.AccentDark else UiTokens.Muted
        )
        if (isMine) {
            Icon(
                imageVector = Icons.Outlined.DoneAll,
                contentDescription = "Status terkirim",
                tint = UiTokens.AccentDark,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onImageClick: () -> Unit,
    onFileClick: () -> Unit,
    onVideoClick: () -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean,
    isSending: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(UiTokens.Bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Lampiran cepat",
            style = MaterialTheme.typography.labelMedium,
            color = UiTokens.Muted
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AttachmentAction(
                label = "Gambar",
                icon = Icons.Outlined.Image,
                tint = UiTokens.Blue,
                onClick = onImageClick,
                modifier = Modifier.weight(1f)
            )
            AttachmentAction(
                label = "Video",
                icon = Icons.Outlined.Videocam,
                tint = UiTokens.Orange,
                onClick = onVideoClick,
                modifier = Modifier.weight(1f)
            )
            AttachmentAction(
                label = "File",
                icon = Icons.Outlined.AttachFile,
                tint = UiTokens.Accent,
                onClick = onFileClick,
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(UiTokens.Soft)
                        .clickable(onClick = onAddClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Tambah",
                        tint = UiTokens.Accent
                    )
                }
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text("Tulis pesan") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = UiTokens.Accent,
                        focusedTextColor = UiTokens.Text,
                        unfocusedTextColor = UiTokens.Text,
                        focusedPlaceholderColor = UiTokens.Muted,
                        unfocusedPlaceholderColor = UiTokens.Muted
                    )
                )
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(UiTokens.Accent, UiTokens.AccentDark)
                            )
                        )
                        .clickable(enabled = enabled, onClick = onSendClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSending) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = "Kirim",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentAction(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(UiTokens.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = UiTokens.Text
        )
    }
}
