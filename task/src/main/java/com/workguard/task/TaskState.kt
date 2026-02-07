package com.workguard.task

data class TaskState(
    val currentStep: TaskStep = TaskStep.START,
    val taskId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

enum class TaskStep {
    START,
    CAMERA,
    COMPLETE,
    DONE
}
