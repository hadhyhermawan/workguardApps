package com.workguard.task.domain

import com.workguard.core.model.Task
import com.workguard.core.model.enums.TaskType
import com.workguard.core.network.ApiResult
import com.workguard.task.data.TaskRepository
import javax.inject.Inject

class StartTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(
        title: String? = null,
        taskType: TaskType = TaskType.PATROL
    ): ApiResult<Task> {
        return repository.startTask(title, taskType)
    }
}
