package com.workguard.patrol.data

import com.workguard.core.model.enums.CameraFacing
import com.workguard.core.network.ApiResult
import com.workguard.patrol.model.PatrolPoint
import com.workguard.patrol.model.PatrolScanResult
import java.io.File

interface PatrolRepository {
    suspend fun startSession(faceScore: Double?): ApiResult<Long>
    suspend fun getPatrolPoints(): ApiResult<List<PatrolPoint>>
    suspend fun uploadPatrolMedia(
        taskId: String,
        photo: File,
        cameraFacing: CameraFacing
    ): ApiResult<String>
    suspend fun scanPatrolPoint(
        patrolPointId: Int,
        patrolSessionId: Long,
        photoUrl: String
    ): ApiResult<PatrolScanResult>
}
