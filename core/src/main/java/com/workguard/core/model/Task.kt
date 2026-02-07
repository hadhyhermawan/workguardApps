package com.workguard.core.model

import com.workguard.core.model.enums.TaskStatus
import com.workguard.core.model.enums.TaskType

data class Task(
    val id: String,
    val type: TaskType,
    val status: TaskStatus
)
