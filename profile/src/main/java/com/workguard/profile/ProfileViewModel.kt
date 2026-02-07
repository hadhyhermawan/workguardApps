package com.workguard.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workguard.core.network.ApiResult
import com.workguard.profile.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getProfile()) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            profile = result.data,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update {
                        it.copy(isLoading = false, errorMessage = result.throwable.message)
                    }
                }
            }
        }
    }
}
