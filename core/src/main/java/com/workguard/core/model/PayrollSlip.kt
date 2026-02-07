package com.workguard.core.model

data class PayrollSlip(
    val id: String,
    val period: String,
    val issuedAtMillis: Long,
    val netAmount: Long
)
