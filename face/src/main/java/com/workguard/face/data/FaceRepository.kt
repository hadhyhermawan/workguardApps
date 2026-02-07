package com.workguard.face.data

import com.workguard.core.model.FaceSession
import com.workguard.core.model.enums.FaceContext
import com.workguard.core.network.ApiResult
import com.workguard.core.network.FaceTemplate
import com.workguard.core.network.FaceTemplateStatus
import java.io.File

interface FaceRepository {
    suspend fun createFaceSession(
        context: FaceContext,
        photo: java.io.File,
        issuedAtMillis: Long,
        expiresAtMillis: Long
    ): ApiResult<FaceSession>

    suspend fun registerFaceTemplate(
        photo: File,
        slot: Int,
        notes: String? = null
    ): ApiResult<FaceTemplate>

    suspend fun getFaceTemplateStatus(): ApiResult<FaceTemplateStatus>
}
