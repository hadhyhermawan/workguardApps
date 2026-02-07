package com.workguard.face.domain

import com.workguard.core.network.ApiResult
import com.workguard.core.network.FaceTemplate
import com.workguard.face.data.FaceRepository
import java.io.File
import javax.inject.Inject

class RegisterFaceTemplateUseCase @Inject constructor(
    private val repository: FaceRepository
) {
    suspend operator fun invoke(
        photo: File,
        slot: Int,
        notes: String? = null
    ): ApiResult<FaceTemplate> {
        return repository.registerFaceTemplate(photo, slot, notes)
    }
}
