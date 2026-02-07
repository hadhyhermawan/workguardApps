package com.workguard.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workguard.auth.domain.LoginUseCase
import com.workguard.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {
    companion object {
        private const val TAG = "AuthViewModel"
    }
    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events = _events.asSharedFlow()

    fun onEmployeeCodeChange(value: String) {
        _state.update { it.copy(employeeCode = value) }
    }

    fun onCompanyCodeChange(value: String) {
        _state.update { it.copy(companyCode = value) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value) }
    }

    fun onLoginClicked() {
        val companyCode = state.value.companyCode.trim()
        val code = state.value.employeeCode.trim()
        val password = state.value.password
        if (companyCode.isBlank() || code.isBlank() || password.isBlank()) {
            Log.w(TAG, "Login blocked: missing credentials")
            _state.update {
                it.copy(errorMessage = "Lengkapi kode perusahaan, NIK, dan password")
            }
            return
        }

        viewModelScope.launch {
            val maskedCode = if (code.length <= 3) "***" else "***${code.takeLast(3)}"
            val maskedCompany = if (companyCode.length <= 2) "**" else "${companyCode.take(2)}***"
            Log.d(TAG, "Login requested for companyCode=$maskedCompany employeeCode=$maskedCode")
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = loginUseCase(companyCode, code, password)) {
                is ApiResult.Success -> {
                    Log.i(TAG, "Login success")
                    _state.update { it.copy(isLoading = false) }
                    _events.emit(AuthEvent.LoggedIn)
                }
                is ApiResult.Error -> {
                    Log.w(TAG, "Login failed: ${result.throwable.message}")
                    _state.update {
                        it.copy(isLoading = false, errorMessage = result.throwable.message)
                    }
                    _events.emit(AuthEvent.ShowError("Login failed"))
                }
            }
        }
    }
}

sealed interface AuthEvent {
    object LoggedIn : AuthEvent
    data class ShowError(val message: String) : AuthEvent
}
