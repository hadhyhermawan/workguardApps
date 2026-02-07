package com.workguard.face.domain

import com.workguard.core.network.ApiResult
import com.workguard.core.network.FaceTemplateStatus
import com.workguard.face.data.FaceRepository
import javax.inject.Inject

class GetFaceTemplateStatusUseCase @Inject constructor(
    private val repository: FaceRepository
) {
    suspend operator fun invoke(): ApiResult<FaceTemplateStatus> {
        return repository.getFaceTemplateStatus()
    }
}
