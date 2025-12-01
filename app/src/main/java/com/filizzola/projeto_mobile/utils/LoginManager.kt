package com.filizzola.projeto_mobile.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class LoginManager(private val context: Context) {

    private val USER_ID_KEY = stringPreferencesKey("user_id")
    // Chave para salvar o token de sessão do Supabase
    private val USER_SESSION_KEY = stringPreferencesKey("user_session")
    // NOVA CHAVE: Consentimento de sincronização
    private val SYNC_CONSENT_KEY = booleanPreferencesKey("sync_consent")

    suspend fun saveLoggedUser(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
    }

    suspend fun getLoggedUser(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[USER_ID_KEY]
    }

    suspend fun saveSession(sessionJson: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_SESSION_KEY] = sessionJson
        }
    }

    // Recupera o JSON da sessão para restaurar o login
    suspend fun getSession(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[USER_SESSION_KEY]
    }

    suspend fun clearLoggedUser() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_ID_KEY)
            preferences.remove(USER_SESSION_KEY)

        }
    }


    suspend fun setSyncConsent(isAllowed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SYNC_CONSENT_KEY] = isAllowed
        }
    }

    suspend fun hasSyncConsent(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[SYNC_CONSENT_KEY] ?: false
    }
}