package com.workguard.auth.domain

import com.workguard.auth.data.AuthRepository
import com.workguard.core.model.Employee
import com.workguard.core.network.ApiResult
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): ApiResult<Employee> {
        return authRepository.getProfile()
    }
}
