package com.notaskflow.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth")

class TokenManager(
    private val context: Context
) {
    val tokenFlow: Flow<String?> = context.authDataStore.data
        .map { preferences -> preferences[TOKEN_VALUE] }

    suspend fun saveToken(tokenValue: String, expireTime: Long) {
        context.authDataStore.edit { preferences ->
            preferences[TOKEN_VALUE] = tokenValue
            preferences[EXPIRE_TIME] = expireTime
        }
    }

    suspend fun currentToken(): String? {
        return tokenFlow.first()
    }

    suspend fun expireTime(): Long? {
        return context.authDataStore.data
            .map { preferences -> preferences[EXPIRE_TIME] }
            .first()
    }

    suspend fun clear() {
        context.authDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private companion object {
        val TOKEN_VALUE = stringPreferencesKey("token_value")
        val EXPIRE_TIME = longPreferencesKey("expire_time")
    }
}
