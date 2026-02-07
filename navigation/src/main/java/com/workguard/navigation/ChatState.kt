package com.workguard.navigation

data class ChatState(
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val threadId: String? = null,
    val threadTitle: String = "Chat",
    val input: String = "",
    val messages: List<ChatUiMessage> = emptyList(),
    val nextCursor: String? = null,
    val isLoadingMore: Boolean = false
)

data class ChatUiMessage(
    val id: String,
    val sender: String,
    val isMine: Boolean,
    val time: String,
    val type: MessageType,
    val text: String = "",
    val attachmentName: String = "",
    val attachmentMeta: String = ""
)

enum class MessageType {
    Text,
    Image,
    Video,
    File,
    System
}
