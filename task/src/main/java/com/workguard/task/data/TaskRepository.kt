package com.workguard.task.data

import com.workguard.core.model.Task
import com.workguard.core.model.enums.TaskType
import com.workguard.core.network.ApiResult

interface TaskRepository {
    suspend fun startTask(title: String? = null, taskType: TaskType = TaskType.PATROL): ApiResult<Task>
    suspend fun uploadTaskMedia(taskId: String): ApiResult<Unit>
    suspend fun completeTask(taskId: String): ApiResult<Task>
}
