package com.workguard.face.domain

import com.workguard.core.datastore.FaceSessionStore
import com.workguard.core.model.FaceSession
import com.workguard.core.model.enums.FaceContext
import com.workguard.core.network.ApiResult
import com.workguard.core.security.FaceSessionConfig
import com.workguard.core.util.Clock
import com.workguard.face.data.FaceRepository
import java.io.File
import javax.inject.Inject

class CreateFaceSessionUseCase @Inject constructor(
    private val repository: FaceRepository,
    private val faceSessionStore: FaceSessionStore,
    private val clock: Clock,
    private val config: FaceSessionConfig
) {
    suspend operator fun invoke(context: FaceContext, photo: File): ApiResult<FaceSession> {
        val nowMillis = clock.nowMillis()
        val expiresAt = nowMillis + config.ttlFor(context)
        val result = repository.createFaceSession(context, photo, nowMillis, expiresAt)
        if (result is ApiResult.Success) {
            faceSessionStore.saveSession(result.data)
        }
        return result
    }
}
