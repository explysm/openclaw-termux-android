package com.explysm.openclaw.data

import android.content.Context
import com.explysm.openclaw.utils.Logger
import com.explysm.openclaw.utils.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class SettingsRepository(private val context: Context) {
    
    companion object {
        const val DEFAULT_API_URL = "http://127.0.0.1:5039"
        const val DEFAULT_POLL_INTERVAL = 10
    }
    
    // New JSON-based settings
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val _settings = MutableStateFlow(SettingsData())
    val settings: StateFlow<SettingsData> = _settings.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        try {
            // Initialize storage
            val storageInitialized = StorageManager.initialize(context)
            Logger.i("SettingsRepository", "Storage initialization: $storageInitialized")
            
            // Try to load from JSON file
            val settingsFile = StorageManager.getSettingsFile()
            val jsonContent = StorageManager.getFileContent(settingsFile)
            
            if (jsonContent != null && jsonContent.isNotBlank()) {
                try {
                    val loadedSettings = json.decodeFromString<SettingsData>(jsonContent)
                    _settings.value = loadedSettings
                    Logger.i("SettingsRepository", "Successfully loaded settings from JSON file: ${settingsFile.absolutePath}")
                } catch (e: Exception) {
                    Logger.e("SettingsRepository", "Failed to parse JSON settings, using defaults", e)
                    _settings.value = SettingsData()
                }
            } else {
                Logger.i("SettingsRepository", "No JSON settings found, using defaults")
                _settings.value = SettingsData()
                // Save default settings to file
                saveSettingsToFile(SettingsData())
            }
            
            Logger.i("SettingsRepository", "Final settings loaded: ${_settings.value}")
            
        } catch (e: Exception) {
            Logger.e("SettingsRepository", "Error loading settings, using defaults", e)
            _settings.value = SettingsData()
        }
    }
    
    private suspend fun saveSettingsToFile(settingsData: SettingsData): Boolean {
        return try {
            val jsonContent = json.encodeToString(settingsData)
            val success = StorageManager.setFileContent(StorageManager.getSettingsFile(), jsonContent)
            if (success) {
                Logger.d("SettingsRepository", "Settings saved to file successfully")
            } else {
                Logger.w("SettingsRepository", "Failed to save settings to file")
            }
            success
        } catch (e: Exception) {
            Logger.e("SettingsRepository", "Error saving settings to file", e)
            false
        }
    }
    
    // Public Flow interface for compatibility with existing code
    val apiUrl: Flow<String> = settings.map { it.apiUrl }
    val pollInterval: Flow<Int> = settings.map { it.pollInterval }
    val isDarkTheme: Flow<Boolean> = settings.map { it.darkTheme }
    val autoStart: Flow<Boolean> = settings.map { it.autoStart }
    val enableNotifications: Flow<Boolean> = settings.map { it.enableNotifications }
    val onboardingCompleted: Flow<Boolean> = settings.map { it.onboardingCompleted }
    
    // Public suspend functions for updating settings
    suspend fun setApiUrl(url: String) {
        try {
            val newSettings = _settings.value.copy(apiUrl = url)
            _settings.value = newSettings
            saveSettingsToFile(newSettings)
            Logger.d("SettingsRepository", "API URL updated: $url")
        } catch (e: Exception) {
            Logger.e("SettingsRepository", "Error setting API URL", e)
        }
    }
    
    suspend fun setPollInterval(interval: Int) {
        try {
            val newSettings = _settings.value.copy(pollInterval = interval)
            _settings.value = newSettings
            saveSettingsToFile(newSettings)
            Logger.d("SettingsRepository", "Poll interval updated: $interval")
        } catch (e: Exception) {
            Logger.e("SettingsRepository", "Error setting poll interval", e)
        }
    }
    
    suspend fun setDarkTheme(enabled: Boolean) {
        try {
            val newSettings = _settings.value.copy(darkTheme = enabled)
            _settings.value = newSettings
            saveSettingsToFile(newSettings)
            Logger.d("SettingsRepository", "Dark theme updated: $enabled")
        } catch (e: Exception) {
            Logger.e("SettingsRepository", "Error setting dark theme", e)
        }
    }
    
    suspend fun setAutoStart(enabled: Boolean) {
        try {
            val newSettings = _settings.value.copy(autoStart = enabled)
            _settings.value = newSettings
            saveSettingsToFile(newSettings)
            Logger.d("SettingsRepository", "Auto start updated: $enabled")
        } catch (e: Exception) {
            Logger.e("SettingsRepository", "Error setting auto start", e)
        }
    }
    
    suspend fun setEnableNotifications(enabled: Boolean) {
        try {
            val newSettings = _settings.value.copy(enableNotifications = enabled)
            _settings.value = newSettings
            saveSettingsToFile(newSettings)
            Logger.d("SettingsRepository", "Notifications updated: $enabled")
        } catch (e: Exception) {
            Logger.e("SettingsRepository", "Error setting notifications", e)
        }
    }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        try {
            val newSettings = _settings.value.copy(onboardingCompleted = completed)
            _settings.value = newSettings
            saveSettingsToFile(newSettings)
            Logger.d("SettingsRepository", "Onboarding completed updated: $completed")
        } catch (e: Exception) {
            Logger.e("SettingsRepository", "Error setting onboarding completed", e)
        }
    }
    
    // Additional utility functions
    suspend fun resetToDefaults() {
        try {
            val defaultSettings = SettingsData()
            _settings.value = defaultSettings
            saveSettingsToFile(defaultSettings)
            Logger.i("SettingsRepository", "Settings reset to defaults")
        } catch (e: Exception) {
            Logger.e("SettingsRepository", "Error resetting settings to defaults", e)
        }
    }
    
    suspend fun exportSettings(): String? {
        return try {
            json.encodeToString(_settings.value)
        } catch (e: Exception) {
            Logger.e("SettingsRepository", "Error exporting settings", e)
            null
        }
    }
    
    suspend fun importSettings(jsonString: String): Boolean {
        return try {
            val importedSettings = json.decodeFromString<SettingsData>(jsonString)
            _settings.value = importedSettings
            saveSettingsToFile(importedSettings)
            Logger.i("SettingsRepository", "Settings imported successfully")
            true
        } catch (e: Exception) {
            Logger.e("SettingsRepository", "Error importing settings", e)
            false
        }
    }
    
    fun getStorageInfo(): String {
        return """
        Settings Repository Info:
        Current Settings: ${_settings.value}
        Storage Available: ${StorageManager.isStorageAvailable()}
        Settings File Exists: ${StorageManager.fileExists(StorageManager.getSettingsFile())}
        Settings File Path: ${StorageManager.getSettingsFile().absolutePath}
        Storage Manager Info:
        ${StorageManager.getStorageInfo()}
        """.trimIndent()
    }
}