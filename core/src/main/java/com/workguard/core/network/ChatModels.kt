package com.workguard.core.network

import com.squareup.moshi.Json

data class ChatThreadListResponse(
    val items: List<ChatThreadItem> = emptyList(),
    @Json(name = "next_cursor")
    val nextCursor: String? = null
)

data class ChatThreadItem(
    val id: String? = null,
    val type: String? = null,
    val title: String? = null,
    @Json(name = "avatar_url")
    val avatarUrl: String? = null,
    @Json(name = "last_message")
    val lastMessage: ChatLastMessage? = null,
    @Json(name = "unread_count")
    val unreadCount: Int? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null
)

data class ChatLastMessage(
    val id: String? = null,
    val text: String? = null,
    @Json(name = "sender_id")
    val senderId: Int? = null,
    @Json(name = "created_at")
    val createdAt: String? = null
)

data class ChatThreadDetail(
    val id: String? = null,
    val type: String? = null,
    val title: String? = null,
    @Json(name = "avatar_url")
    val avatarUrl: String? = null,
    val members: List<ChatMember> = emptyList()
)

data class ChatMember(
    val id: Int? = null,
    val name: String? = null,
    @Json(name = "photo_url")
    val photoUrl: String? = null
)

data class ChatMessageListResponse(
    val items: List<ChatMessageItem> = emptyList(),
    @Json(name = "next_cursor")
    val nextCursor: String? = null
)

data class ChatMessageItem(
    val id: String? = null,
    @Json(name = "thread_id")
    val threadId: String? = null,
    @Json(name = "sender_id")
    val senderId: Int? = null,
    val text: String? = null,
    val type: String? = null,
    val attachments: List<ChatAttachment> = emptyList(),
    @Json(name = "created_at")
    val createdAt: String? = null,
    val status: String? = null
)

data class ChatAttachment(
    val url: String? = null,
    val mime: String? = null,
    val size: Long? = null
)

data class ChatMessageRequest(
    val text: String? = null,
    val type: String? = null,
    val attachments: List<ChatAttachmentRequest> = emptyList()
)

data class ChatAttachmentRequest(
    val url: String? = null,
    val mime: String? = null,
    val size: Long? = null
)

data class ChatAttachmentUploadResponse(
    val id: Int? = null,
    @Json(name = "file_url")
    val fileUrl: String? = null,
    val mime: String? = null,
    val size: Long? = null
)

data class ChatReadRequest(
    @Json(name = "last_read_message_id")
    val lastReadMessageId: String? = null
)

data class ChatTypingRequest(
    @Json(name = "is_typing")
    val isTyping: Boolean
)

data class ChatCallStartRequest(
    val type: String,
    val meta: Map<String, String>? = null
)

data class ChatCallResponse(
    val id: String? = null,
    val type: String? = null,
    val meta: Map<String, String>? = null
)
