package com.workguard.task.domain

import com.workguard.core.network.ApiResult
import com.workguard.task.data.TaskRepository
import javax.inject.Inject

class UploadTaskMediaUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): ApiResult<Unit> {
        return repository.uploadTaskMedia(taskId)
    }
}
