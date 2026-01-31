package com.explysm.openclaw.utils

import android.content.Context
import android.os.Environment
import com.explysm.openclaw.utils.Logger
import java.io.File

object StorageManager {
    
    private const val APP_DIR_NAME = "OpenClaw-Termux"
    private const val SETTINGS_FILE_NAME = "settings.json"
    private const val LOGS_DIR_NAME = "logs"
    private const val DATA_DIR_NAME = "data"
    
    private val baseDirectory: File by lazy {
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APP_DIR_NAME)
    }
    
    private val settingsFileObj: File by lazy {
        File(baseDirectory, SETTINGS_FILE_NAME)
    }
    
    private val logsDirectory: File by lazy {
        File(baseDirectory, LOGS_DIR_NAME)
    }
    
    private val dataDirectory: File by lazy {
        File(baseDirectory, DATA_DIR_NAME)
    }
    
    fun initialize(context: Context): Boolean {
        return try {
            // Create main app directory
            if (!baseDirectory.exists()) {
                val created = baseDirectory.mkdirs()
                Logger.i("StorageManager", "Created base directory: $baseDirectory, success: $created")
            }
            
            // Create logs directory
            if (!logsDirectory.exists()) {
                val created = logsDirectory.mkdirs()
                Logger.i("StorageManager", "Created logs directory: $logsDirectory, success: $created")
            }
            
            // Create data directory
            if (!dataDirectory.exists()) {
                val created = dataDirectory.mkdirs()
                Logger.i("StorageManager", "Created data directory: $dataDirectory, success: $created")
            }
            
            // Verify external storage is writable
            val writable = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
            Logger.i("StorageManager", "External storage writable: $writable")
            
            writable && baseDirectory.exists() && logsDirectory.exists() && dataDirectory.exists()
        } catch (e: Exception) {
            Logger.e("StorageManager", "Failed to initialize storage directories", e)
            false
        }
    }
    
    fun getSettingsFile(): File {
        return settingsFileObj
    }
    
    fun getLogsDir(): File {
        return logsDirectory
    }

    fun getDataDir(): File {
        return dataDirectory
    }

    fun getBaseDir(): File {
        return baseDirectory
    }
    
    fun isStorageAvailable(): Boolean {
        return try {
            Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && baseDirectory.exists()
        } catch (e: Exception) {
            Logger.e("StorageManager", "Error checking storage availability", e)
            false
        }
    }
    
    fun getFileContent(file: File): String? {
        return try {
            if (file.exists() && file.canRead()) {
                file.readText()
            } else {
                Logger.w("StorageManager", "File not readable: ${file.absolutePath}")
                null
            }
        } catch (e: Exception) {
            Logger.e("StorageManager", "Error reading file: ${file.absolutePath}", e)
            null
        }
    }
    
    fun setFileContent(file: File, content: String): Boolean {
        return try {
            // Ensure parent directory exists
            file.parentFile?.mkdirs()
            
            file.writeText(content)
            Logger.d("StorageManager", "Successfully wrote file: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Logger.e("StorageManager", "Error writing file: ${file.absolutePath}", e)
            false
        }
    }
    
    fun fileExists(file: File): Boolean {
        return try {
            file.exists()
        } catch (e: Exception) {
            Logger.e("StorageManager", "Error checking file existence: ${file.absolutePath}", e)
            false
        }
    }
    
    fun deleteFile(file: File): Boolean {
        return try {
            if (file.exists()) {
                val deleted = file.delete()
                Logger.d("StorageManager", "Deleted file: ${file.absolutePath}, success: $deleted")
                deleted
            } else {
                true // File doesn't exist, consider it "deleted"
            }
        } catch (e: Exception) {
            Logger.e("StorageManager", "Error deleting file: ${file.absolutePath}", e)
            false
        }
    }
    
    fun getStorageInfo(): String {
        return try {
            """
            Storage Manager Info:
            Base Directory: ${baseDirectory.absolutePath}
            Settings File: ${settingsFile.absolutePath}
            Logs Directory: ${logsDirectory.absolutePath}
            Data Directory: ${dataDirectory.absolutePath}
            External Storage State: ${Environment.getExternalStorageState()}
            Storage Available: ${isStorageAvailable()}
            Base Dir Exists: ${baseDirectory.exists()}
            Settings File Exists: ${settingsFileObj.exists()}
            """.trimIndent()
        } catch (e: Exception) {
            "Error getting storage info: ${e.message}"
        }
    }
}