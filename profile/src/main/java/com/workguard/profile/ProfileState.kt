package com.workguard.profile

import com.workguard.core.network.EmployeeProfile

data class ProfileState(
    val profile: EmployeeProfile? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
