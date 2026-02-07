package com.workguard.core.datastore

import javax.inject.Inject
import javax.inject.Singleton

interface AuthDataStore {
    fun accessToken(): String?
    fun refreshToken(): String?
    fun tokenType(): String?
    fun expiresAtMillis(): Long?
    fun saveTokens(
        accessToken: String,
        refreshToken: String,
        tokenType: String? = null,
        expiresAtMillis: Long? = null
    )
    fun companyCode(): String?
    fun saveCompanyCode(companyCode: String)
    fun clear()
}

@Singleton
class InMemoryAuthDataStore @Inject constructor() : AuthDataStore {
    private var access: String? = null
    private var refresh: String? = null
    private var type: String? = null
    private var expiresAt: Long? = null
    private var company: String? = null

    override fun accessToken(): String? = access

    override fun refreshToken(): String? = refresh

    override fun tokenType(): String? = type

    override fun expiresAtMillis(): Long? = expiresAt

    override fun saveTokens(
        accessToken: String,
        refreshToken: String,
        tokenType: String?,
        expiresAtMillis: Long?
    ) {
        access = accessToken
        refresh = refreshToken
        type = tokenType
        expiresAt = expiresAtMillis
    }

    override fun companyCode(): String? = company

    override fun saveCompanyCode(companyCode: String) {
        company = companyCode
    }

    override fun clear() {
        access = null
        refresh = null
        type = null
        expiresAt = null
        company = null
    }
}

fun AuthDataStore.isAccessTokenValid(nowMillis: Long): Boolean {
    val token = accessToken()
    if (token.isNullOrBlank()) {
        return false
    }
    val expiresAt = expiresAtMillis()
    if (expiresAt != null) {
        return expiresAt > nowMillis
    }
    return !tokenType().isNullOrBlank()
}
