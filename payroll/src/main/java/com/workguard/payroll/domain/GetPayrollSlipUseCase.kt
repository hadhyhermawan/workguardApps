package com.workguard.payroll.domain

import com.workguard.core.model.PayrollSlip
import com.workguard.core.network.ApiResult
import java.util.UUID
import javax.inject.Inject

class GetPayrollSlipUseCase @Inject constructor() {
    suspend operator fun invoke(period: String): ApiResult<PayrollSlip> {
        val slip = PayrollSlip(
            id = UUID.randomUUID().toString(),
            period = period,
            issuedAtMillis = System.currentTimeMillis(),
            netAmount = 0L
        )
        return ApiResult.Success(slip)
    }
}
