package com.explysm.openclaw.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    
    companion object {
        val API_URL = stringPreferencesKey("api_url")
        val POLL_INTERVAL = intPreferencesKey("poll_interval")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val AUTO_START = booleanPreferencesKey("auto_start")
        val ENABLE_NOTIFICATIONS = booleanPreferencesKey("enable_notifications")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        
        const val DEFAULT_API_URL = "http://127.0.0.1:5039"
        const val DEFAULT_POLL_INTERVAL = 10
    }
    
    val apiUrl: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[API_URL] ?: DEFAULT_API_URL
        }
        .catch { exception ->
            // Handle DataStore errors gracefully
            com.explysm.openclaw.utils.Logger.e("SettingsRepository", "Error reading API URL from DataStore", exception)
            emit(DEFAULT_API_URL)
        }
        .flowOn(Dispatchers.IO)

    val pollInterval: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[POLL_INTERVAL] ?: DEFAULT_POLL_INTERVAL
        }
        .catch { exception ->
            com.explysm.openclaw.utils.Logger.e("SettingsRepository", "Error reading poll interval from DataStore", exception)
            emit(DEFAULT_POLL_INTERVAL)
        }
        .flowOn(Dispatchers.IO)

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_THEME] ?: false
        }
        .catch { exception ->
            com.explysm.openclaw.utils.Logger.e("SettingsRepository", "Error reading dark theme from DataStore", exception)
            emit(false)
        }
        .flowOn(Dispatchers.IO)

    val autoStart: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_START] ?: false
        }
        .catch { exception ->
            com.explysm.openclaw.utils.Logger.e("SettingsRepository", "Error reading auto start from DataStore", exception)
            emit(false)
        }
        .flowOn(Dispatchers.IO)

    val enableNotifications: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ENABLE_NOTIFICATIONS] ?: true
        }
        .catch { exception ->
            com.explysm.openclaw.utils.Logger.e("SettingsRepository", "Error reading notifications setting from DataStore", exception)
            emit(true)
        }
        .flowOn(Dispatchers.IO)
    
    suspend fun setApiUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[API_URL] = url
        }
    }
    
    suspend fun setPollInterval(interval: Int) {
        context.dataStore.edit { preferences ->
            preferences[POLL_INTERVAL] = interval
        }
    }
    
    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_THEME] = enabled
        }
    }
    
    suspend fun setAutoStart(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_START] = enabled
        }
    }
    
    suspend fun setEnableNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_NOTIFICATIONS] = enabled
        }
    }
    
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED] ?: false
        }
        .catch { exception ->
            com.explysm.openclaw.utils.Logger.e("SettingsRepository", "Error reading onboarding completion from DataStore", exception)
            emit(false)
        }
        .flowOn(Dispatchers.IO)
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }
}
