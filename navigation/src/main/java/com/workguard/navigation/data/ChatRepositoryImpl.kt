package com.workguard.navigation.data

import com.workguard.core.network.ApiResult
import com.workguard.core.network.ApiService
import com.workguard.core.network.ChatAttachmentUploadResponse
import com.workguard.core.network.ChatMessageItem
import com.workguard.core.network.ChatMessageListResponse
import com.workguard.core.network.ChatMessageRequest
import com.workguard.core.network.ChatReadRequest
import com.workguard.core.network.ChatThreadDetail
import com.workguard.core.network.ChatThreadListResponse
import com.workguard.core.util.FileUtil
import java.io.File
import java.io.IOException
import javax.inject.Inject
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException

class ChatRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ChatRepository {
    override suspend fun getThreads(
        type: String?,
        limit: Int?,
        cursor: String?
    ): ApiResult<ChatThreadListResponse> {
        return try {
            val response = apiService.getChatThreads(type, limit, cursor)
            if (response.success == false) {
                ApiResult.Error(IllegalStateException(response.message ?: "Gagal memuat chat"))
            } else {
                ApiResult.Success(response.data ?: ChatThreadListResponse())
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal memuat chat (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun getThreadDetail(threadId: String): ApiResult<ChatThreadDetail> {
        return try {
            val response = apiService.getChatThreadDetail(threadId)
            if (response.success == false) {
                ApiResult.Error(IllegalStateException(response.message ?: "Gagal memuat thread"))
            } else {
                val data = response.data
                    ?: return ApiResult.Error(IllegalStateException("Thread kosong"))
                ApiResult.Success(data)
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal memuat thread (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun getMessages(
        threadId: String,
        limit: Int?,
        cursor: String?
    ): ApiResult<ChatMessageListResponse> {
        return try {
            val response = apiService.getChatMessages(threadId, limit, cursor)
            if (response.success == false) {
                ApiResult.Error(IllegalStateException(response.message ?: "Gagal memuat pesan"))
            } else {
                ApiResult.Success(response.data ?: ChatMessageListResponse())
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal memuat pesan (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun sendMessage(
        threadId: String,
        request: ChatMessageRequest
    ): ApiResult<ChatMessageItem> {
        return try {
            val response = apiService.sendChatMessage(threadId, request)
            if (response.success == false) {
                ApiResult.Error(IllegalStateException(response.message ?: "Gagal mengirim pesan"))
            } else {
                val data = response.data
                    ?: return ApiResult.Error(IllegalStateException("Respons pesan kosong"))
                ApiResult.Success(data)
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal mengirim pesan (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun uploadAttachment(file: File): ApiResult<ChatAttachmentUploadResponse> {
        if (!file.exists() || file.length() <= 0L) {
            return ApiResult.Error(IllegalStateException("File tidak tersedia"))
        }
        val fileName = FileUtil.sanitizeFileName(file.name.ifBlank { "chat_file" })
        val mediaType = resolveMimeType(file)
        val part = MultipartBody.Part.createFormData(
            "file",
            fileName,
            file.asRequestBody(mediaType)
        )
        return try {
            val response = apiService.uploadChatAttachment(part)
            if (response.success == false) {
                ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal upload lampiran")
                )
            } else {
                val data = response.data
                    ?: return ApiResult.Error(IllegalStateException("Respons upload kosong"))
                ApiResult.Success(data)
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal upload lampiran (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun markRead(threadId: String, lastMessageId: String?): ApiResult<Unit> {
        return try {
            val response = apiService.markChatRead(
                threadId,
                ChatReadRequest(lastReadMessageId = lastMessageId)
            )
            if (response.success == false) {
                ApiResult.Error(IllegalStateException(response.message ?: "Gagal update read"))
            } else {
                ApiResult.Success(Unit)
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal update read (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun getCurrentUserId(): ApiResult<Int> {
        return try {
            val response = apiService.getMe()
            if (response.success == false) {
                ApiResult.Error(IllegalStateException(response.message ?: "Gagal memuat user"))
            } else {
                val id = response.data?.employeeId
                    ?: return ApiResult.Error(IllegalStateException("ID user kosong"))
                ApiResult.Success(id)
            }
        } catch (e: HttpException) {
            ApiResult.Error(IllegalStateException("Gagal memuat user (${e.code()})"))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    private fun resolveMimeType(file: File): MediaType {
        val ext = file.extension.lowercase()
        return when (ext) {
            "png" -> "image/png".toMediaType()
            "webp" -> "image/webp".toMediaType()
            "jpg", "jpeg" -> "image/jpeg".toMediaType()
            "mp4" -> "video/mp4".toMediaType()
            "pdf" -> "application/pdf".toMediaType()
            else -> "application/octet-stream".toMediaType()
        }
    }
}
