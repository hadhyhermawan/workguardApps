package com.workguard.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workguard.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AuthSessionViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    companion object {
        private const val TAG = "AuthSessionViewModel"
    }

    fun logout() {
        viewModelScope.launch {
            Log.d(TAG, "Logout started")
            authRepository.logout()
        }
    }
}
