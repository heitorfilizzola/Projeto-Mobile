package com.filizzola.projeto_mobile.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class LoginManager(private val context: Context) {

    private val USER_ID_KEY = stringPreferencesKey("user_id")

    suspend fun saveLoggedUser(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
    }

    suspend fun getLoggedUser(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[USER_ID_KEY]
    }

    suspend fun clearLoggedUser() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_ID_KEY)
        }
    }
}