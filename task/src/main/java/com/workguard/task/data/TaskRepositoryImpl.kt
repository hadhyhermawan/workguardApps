package com.workguard.task.data

import android.util.Log
import com.workguard.core.datastore.FaceSessionStore
import com.workguard.core.location.LocationProvider
import com.workguard.core.model.Task
import com.workguard.core.model.enums.FaceContext
import com.workguard.core.model.enums.TaskStatus
import com.workguard.core.model.enums.TaskType
import com.workguard.core.network.ApiResult
import com.workguard.core.network.ApiService
import com.workguard.core.network.TaskCompleteRequest
import com.workguard.core.network.TaskCreateRequest
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException

class TaskRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val faceSessionStore: FaceSessionStore,
    private val locationProvider: LocationProvider
) : TaskRepository {
    companion object {
        private const val TAG = "TaskRepository"
    }

    override suspend fun startTask(title: String?, taskType: TaskType): ApiResult<Task> {
        val faceSession = faceSessionStore.getSession()
            ?: return ApiResult.Error(IllegalStateException("Face session belum tersedia"))
        if (faceSession.context != FaceContext.TASK) {
            return ApiResult.Error(IllegalStateException("Face session tidak sesuai konteks"))
        }
        val location = locationProvider.getLastKnownLocation()
            ?: return ApiResult.Error(IllegalStateException("Lokasi belum tersedia"))
        val request = TaskCreateRequest(
            taskType = taskType.name,
            title = title,
            faceSessionId = faceSession.sessionId,
            latitude = location.latitude,
            longitude = location.longitude
        )
        return try {
            val response = apiService.createTask(request)
            if (response.success == false) {
                ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal memulai task")
                )
            } else {
                val data = response.data
                    ?: return ApiResult.Error(IllegalStateException("Respons task kosong"))
                if (data.id == null) {
                    return ApiResult.Error(IllegalStateException("ID task tidak valid"))
                }
                ApiResult.Success(mapTask(data, taskType))
            }
        } catch (e: HttpException) {
            val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val serverMessage = extractServerMessage(errorBody)
            Log.w(TAG, "Start task http error: code=${e.code()} body=$errorBody request=$request")
            val message = serverMessage ?: "Gagal memulai task (${e.code()})"
            ApiResult.Error(IllegalStateException(message))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun uploadTaskMedia(taskId: String): ApiResult<Unit> {
        return ApiResult.Error(IllegalStateException("Foto belum tersedia"))
    }

    override suspend fun completeTask(taskId: String): ApiResult<Task> {
        val location = locationProvider.getLastKnownLocation()
            ?: return ApiResult.Error(IllegalStateException("Lokasi belum tersedia"))
        val request = TaskCompleteRequest(
            latitude = location.latitude,
            longitude = location.longitude
        )
        return try {
            val response = apiService.completeTask(taskId, request)
            if (response.success == false) {
                ApiResult.Error(
                    IllegalStateException(response.message ?: "Gagal menyelesaikan task")
                )
            } else {
                val data = response.data
                    ?: return ApiResult.Error(IllegalStateException("Respons task kosong"))
                if (data.id == null) {
                    return ApiResult.Error(IllegalStateException("ID task tidak valid"))
                }
                ApiResult.Success(mapTask(data, TaskType.PATROL))
            }
        } catch (e: HttpException) {
            val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val serverMessage = extractServerMessage(errorBody)
            Log.w(
                TAG,
                "Complete task http error: code=${e.code()} body=$errorBody taskId=$taskId"
            )
            val message = serverMessage ?: "Gagal menyelesaikan task (${e.code()})"
            ApiResult.Error(IllegalStateException(message))
        } catch (e: IOException) {
            ApiResult.Error(IllegalStateException("Koneksi bermasalah"))
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    private fun mapTask(
        response: com.workguard.core.network.TaskResponse,
        fallbackType: TaskType
    ): Task {
        val status = mapStatus(response.status)
        val taskType = mapType(response.taskType) ?: fallbackType
        return Task(
            id = response.id?.toString().orEmpty(),
            type = taskType,
            status = status
        )
    }

    private fun mapStatus(status: String?): TaskStatus {
        return when (status?.trim()?.uppercase()) {
            "IN_PROGRESS", "STARTED", "ACTIVE" -> TaskStatus.IN_PROGRESS
            "COMPLETED", "DONE", "FINISHED" -> TaskStatus.COMPLETED
            "FAILED", "CANCELLED", "CANCELED" -> TaskStatus.FAILED
            else -> TaskStatus.PENDING
        }
    }

    private fun mapType(raw: String?): TaskType? {
        return when (raw?.trim()?.uppercase()) {
            "PATROL" -> TaskType.PATROL
            "CLEANING" -> TaskType.CLEANING
            "DRIVER" -> TaskType.DRIVER
            else -> null
        }
    }

    private fun extractServerMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val match = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body) ?: return null
        return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
    }
}
