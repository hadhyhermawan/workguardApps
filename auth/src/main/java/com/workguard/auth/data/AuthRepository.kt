package com.workguard.auth.data

import com.workguard.core.model.Employee
import com.workguard.core.model.User
import com.workguard.core.network.ApiResult

interface AuthRepository {
    suspend fun login(companyCode: String, employeeCode: String, password: String): ApiResult<User>
    suspend fun getProfile(): ApiResult<Employee>
    suspend fun logout()
}
