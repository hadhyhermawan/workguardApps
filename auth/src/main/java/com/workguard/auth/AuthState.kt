package com.workguard.auth

data class AuthState(
    val companyCode: String = "",
    val employeeCode: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
