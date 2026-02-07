package com.workguard.profile.data

import com.workguard.core.network.ApiResult
import com.workguard.core.network.EmployeeProfile

interface ProfileRepository {
    suspend fun getProfile(): ApiResult<EmployeeProfile>
}
