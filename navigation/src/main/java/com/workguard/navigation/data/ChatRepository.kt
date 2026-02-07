package com.workguard.navigation.data

import com.workguard.core.network.ApiResult
import com.workguard.core.network.ChatAttachmentUploadResponse
import com.workguard.core.network.ChatMessageItem
import com.workguard.core.network.ChatMessageListResponse
import com.workguard.core.network.ChatMessageRequest
import com.workguard.core.network.ChatThreadDetail
import com.workguard.core.network.ChatThreadListResponse
import java.io.File

interface ChatRepository {
    suspend fun getThreads(
        type: String? = null,
        limit: Int? = null,
        cursor: String? = null
    ): ApiResult<ChatThreadListResponse>

    suspend fun getThreadDetail(threadId: String): ApiResult<ChatThreadDetail>

    suspend fun getMessages(
        threadId: String,
        limit: Int? = null,
        cursor: String? = null
    ): ApiResult<ChatMessageListResponse>

    suspend fun sendMessage(
        threadId: String,
        request: ChatMessageRequest
    ): ApiResult<ChatMessageItem>

    suspend fun uploadAttachment(file: File): ApiResult<ChatAttachmentUploadResponse>

    suspend fun markRead(threadId: String, lastMessageId: String?): ApiResult<Unit>

    suspend fun getCurrentUserId(): ApiResult<Int>
}
