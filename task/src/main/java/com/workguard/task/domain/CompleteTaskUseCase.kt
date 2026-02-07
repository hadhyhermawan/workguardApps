package com.workguard.task.domain

import com.workguard.core.model.Task
import com.workguard.core.network.ApiResult
import com.workguard.task.data.TaskRepository
import javax.inject.Inject

class CompleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): ApiResult<Task> {
        return repository.completeTask(taskId)
    }
}
