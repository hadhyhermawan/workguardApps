package com.workguard.payroll

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class PayrollViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(PayrollState())
    val state = _state.asStateFlow()
}
