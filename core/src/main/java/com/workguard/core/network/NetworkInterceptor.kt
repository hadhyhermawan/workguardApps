package com.workguard.core.network

import com.workguard.core.datastore.AuthDataStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class NetworkInterceptor @Inject constructor(
    private val authDataStore: AuthDataStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val rawToken = authDataStore.accessToken()?.trim()
        val request = chain.request().newBuilder()

        if (!rawToken.isNullOrBlank()) {
            val tokenType = authDataStore.tokenType()?.trim()
            val headerValue = when {
                !tokenType.isNullOrBlank() -> "$tokenType $rawToken"
                rawToken.startsWith("Bearer ", ignoreCase = true) -> rawToken
                rawToken.startsWith("Token ", ignoreCase = true) -> rawToken
                else -> "Bearer $rawToken"
            }
            request.header("Authorization", headerValue)
        }

        return chain.proceed(request.build())
    }
}
