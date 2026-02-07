package com.workguard.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workguard.core.network.ApiResult
import com.workguard.core.network.ChatAttachment
import com.workguard.core.network.ChatMessageItem
import com.workguard.core.network.ChatMessageRequest
import com.workguard.core.util.IsoTimeUtil
import com.workguard.core.util.UrlUtil
import com.workguard.navigation.data.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state = _state.asStateFlow()

    private var currentUserId: Int? = null
    private var memberMap: Map<Int, String> = emptyMap()
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale("id", "ID"))
    private val requestedThreadId =
        savedStateHandle.get<String>(Routes.ChatThreadArg)?.takeIf { it.isNotBlank() }

    init {
        loadInitial()
    }

    fun onInputChange(value: String) {
        _state.update { it.copy(input = value) }
    }

    fun onSendClick() {
        val threadId = state.value.threadId
        if (threadId.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "Thread chat belum siap") }
            return
        }
        val text = state.value.input.trim()
        if (text.isBlank() || state.value.isSending) {
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }
            when (
                val result = repository.sendMessage(
                    threadId,
                    ChatMessageRequest(text = text, type = "text", attachments = emptyList())
                )
            ) {
                is ApiResult.Success -> {
                    val mapped = mapMessages(listOf(result.data))
                    _state.update {
                        it.copy(
                            isSending = false,
                            input = "",
                            messages = it.messages + mapped
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update {
                        it.copy(
                            isSending = false,
                            errorMessage = result.throwable.message
                                ?: "Gagal mengirim pesan"
                        )
                    }
                }
            }
        }
    }

    fun loadMore() {
        val threadId = state.value.threadId ?: return
        val cursor = state.value.nextCursor ?: return
        if (state.value.isLoadingMore) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            when (val result = repository.getMessages(threadId, limit = 30, cursor = cursor)) {
                is ApiResult.Success -> {
                    val mapped = mapMessages(result.data.items)
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            messages = mapped + it.messages,
                            nextCursor = result.data.nextCursor
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            errorMessage = result.throwable.message
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        loadInitial()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            currentUserId = when (val idResult = repository.getCurrentUserId()) {
                is ApiResult.Success -> idResult.data
                is ApiResult.Error -> null
            }

            var threadId = requestedThreadId
            var threadTitle = "Chat"
            if (threadId != null) {
                when (val detailResult = repository.getThreadDetail(threadId)) {
                    is ApiResult.Success -> {
                        val members = detailResult.data.members
                        memberMap = members.mapNotNull { member ->
                            val id = member.id ?: return@mapNotNull null
                            val name = member.name?.takeIf { it.isNotBlank() } ?: "User"
                            id to name
                        }.toMap()
                        threadTitle = detailResult.data.title?.takeIf { it.isNotBlank() } ?: "Chat"
                    }
                    is ApiResult.Error -> {
                        _state.update { it.copy(errorMessage = detailResult.throwable.message) }
                    }
                }
            }

            if (threadId == null) {
                val threadsResult = repository.getThreads(type = null, limit = 20, cursor = null)
                if (threadsResult is ApiResult.Error) {
                    _state.update {
                        it.copy(isLoading = false, errorMessage = threadsResult.throwable.message)
                    }
                    return@launch
                }
                val threads = (threadsResult as ApiResult.Success).data.items
                val thread = threads.firstOrNull() ?: run {
                    _state.update {
                        it.copy(isLoading = false, errorMessage = "Thread chat tidak tersedia")
                    }
                    return@launch
                }
                threadId = thread.id ?: run {
                    _state.update {
                        it.copy(isLoading = false, errorMessage = "Thread chat tidak valid")
                    }
                    return@launch
                }
                threadTitle = thread.title?.takeIf { it.isNotBlank() } ?: "Chat"
                val detailResult = repository.getThreadDetail(threadId)
                if (detailResult is ApiResult.Success) {
                    val members = detailResult.data.members
                    memberMap = members.mapNotNull { member ->
                        val id = member.id ?: return@mapNotNull null
                        val name = member.name?.takeIf { it.isNotBlank() } ?: "User"
                        id to name
                    }.toMap()
                }
            }

            val resolvedThreadId = threadId ?: run {
                _state.update {
                    it.copy(isLoading = false, errorMessage = "Thread chat tidak valid")
                }
                return@launch
            }

            val messagesResult = repository.getMessages(resolvedThreadId, limit = 30, cursor = null)
            when (messagesResult) {
                is ApiResult.Success -> {
                    val mapped = mapMessages(messagesResult.data.items)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            threadId = resolvedThreadId,
                            threadTitle = threadTitle,
                            messages = mapped,
                            nextCursor = messagesResult.data.nextCursor
                        )
                    }
                    val lastMessageId = mapped.lastOrNull()?.id
                    repository.markRead(resolvedThreadId, lastMessageId)
                }
                is ApiResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = messagesResult.throwable.message
                                ?: "Gagal memuat pesan"
                        )
                    }
                }
            }
        }
    }

    private fun mapMessages(items: List<ChatMessageItem>): List<ChatUiMessage> {
        if (items.isEmpty()) return emptyList()
        return items.sortedBy { IsoTimeUtil.parseMillis(it.createdAt) ?: Long.MAX_VALUE }
            .mapNotNull { item ->
                val id = item.id ?: return@mapNotNull null
                val senderId = item.senderId
                val isMine = senderId != null && senderId == currentUserId
                val senderName = if (isMine) {
                    "Saya"
                } else {
                    senderId?.let { memberMap[it] } ?: "User"
                }
                val time = formatTime(item.createdAt)
                val type = mapType(item.type)
                val attachment = item.attachments.firstOrNull()
                ChatUiMessage(
                    id = id,
                    sender = senderName,
                    isMine = isMine,
                    time = time,
                    type = type,
                    text = item.text.orEmpty(),
                    attachmentName = resolveAttachmentName(type, attachment),
                    attachmentMeta = resolveAttachmentMeta(type, attachment)
                )
            }
    }

    private fun mapType(raw: String?): MessageType {
        return when (raw?.trim()?.lowercase()) {
            "image" -> MessageType.Image
            "video" -> MessageType.Video
            "file" -> MessageType.File
            else -> MessageType.Text
        }
    }

    private fun resolveAttachmentName(type: MessageType, attachment: ChatAttachment?): String {
        if (type != MessageType.File) return ""
        val url = attachment?.url?.let { UrlUtil.resolveAssetUrl(it) } ?: return "Dokumen"
        return url.substringAfterLast('/').ifBlank { "Dokumen" }
    }

    private fun resolveAttachmentMeta(type: MessageType, attachment: ChatAttachment?): String {
        if (type != MessageType.File) return ""
        val size = attachment?.size ?: return ""
        return formatSize(size)
    }

    private fun formatSize(size: Long): String {
        val kb = size / 1024f
        val mb = kb / 1024f
        return if (mb >= 1f) {
            String.format(Locale("id", "ID"), "%.1f MB", mb)
        } else {
            String.format(Locale("id", "ID"), "%.0f KB", kb)
        }
    }

    private fun formatTime(value: String?): String {
        val millis = IsoTimeUtil.parseMillis(value) ?: return "--:--"
        return timeFormatter.format(Date(millis))
    }
}
