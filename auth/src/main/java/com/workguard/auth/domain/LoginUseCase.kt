package com.workguard.auth.domain

import com.workguard.auth.data.AuthRepository
import com.workguard.core.model.User
import com.workguard.core.network.ApiResult
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        companyCode: String,
        employeeCode: String,
        password: String
    ): ApiResult<User> {
        return authRepository.login(companyCode, employeeCode, password)
    }
}
