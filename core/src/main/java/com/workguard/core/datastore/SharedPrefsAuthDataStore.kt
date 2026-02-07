package com.workguard.core.datastore

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsAuthDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthDataStore {
    companion object {
        private const val PREFS_NAME = "workguard_auth"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_COMPANY = "company_code"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun accessToken(): String? = prefs.getString(KEY_ACCESS, null)

    override fun refreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    override fun tokenType(): String? = prefs.getString(KEY_TOKEN_TYPE, null)

    override fun expiresAtMillis(): Long? {
        return if (prefs.contains(KEY_EXPIRES_AT)) {
            prefs.getLong(KEY_EXPIRES_AT, 0L)
        } else {
            null
        }
    }

    override fun saveTokens(
        accessToken: String,
        refreshToken: String,
        tokenType: String?,
        expiresAtMillis: Long?
    ) {
        val editor = prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
        if (tokenType.isNullOrBlank()) {
            editor.remove(KEY_TOKEN_TYPE)
        } else {
            editor.putString(KEY_TOKEN_TYPE, tokenType)
        }
        if (expiresAtMillis == null) {
            editor.remove(KEY_EXPIRES_AT)
        } else {
            editor.putLong(KEY_EXPIRES_AT, expiresAtMillis)
        }
        editor.apply()
    }

    override fun companyCode(): String? = prefs.getString(KEY_COMPANY, null)

    override fun saveCompanyCode(companyCode: String) {
        prefs.edit()
            .putString(KEY_COMPANY, companyCode)
            .apply()
    }

    override fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_TOKEN_TYPE)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_COMPANY)
            .apply()
    }
}
