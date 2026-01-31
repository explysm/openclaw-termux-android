package com.explysm.openclaw.utils

import android.content.Context
import android.os.Environment
import com.explysm.openclaw.utils.Logger
import java.io.File

object StorageManager {
    
    private const val APP_DIR_NAME = "OpenClaw"
    private const val SETTINGS_FILE_NAME = "settings.json"
    private const val LOGS_DIR_NAME = "logs"
    private const val DATA_DIR_NAME = "data"
    
    private var baseDirectory: File? = null
    private var internalDirectory: File? = null
    private var settingsFileObj: File? = null
    private var logsDirectory: File? = null
    private var dataDirectory: File? = null
    
    fun initialize(context: Context): Boolean {
        return try {
            // Internal storage is the most reliable for settings
            internalDirectory = context.filesDir
            
            // External app-specific storage is good for logs and larger data
            // It's accessible via USB at Android/data/com.explysm.openclaw/files
            // and requires NO permissions.
            baseDirectory = context.getExternalFilesDir(null) ?: context.filesDir
            
            val baseDir = baseDirectory!!
            
            // Settings should be internal for maximum reliability
            settingsFileObj = File(context.filesDir, SETTINGS_FILE_NAME)
            
            // Logs and data can be in the external app-specific dir
            logsDirectory = File(baseDir, LOGS_DIR_NAME)
            dataDirectory = File(baseDir, DATA_DIR_NAME)
            
            // Create directories
            if (!logsDirectory!!.exists()) {
                val created = logsDirectory!!.mkdirs()
                android.util.Log.i("StorageManager", "Created logs directory: $logsDirectory, success: $created")
            }
            if (!dataDirectory!!.exists()) {
                val created = dataDirectory!!.mkdirs()
                android.util.Log.i("StorageManager", "Created data directory: $dataDirectory, success: $created")
            }
            
            android.util.Log.i("StorageManager", "Storage initialized. Internal: ${context.filesDir}, External: $baseDir")
            true
        } catch (e: Exception) {
            android.util.Log.e("StorageManager", "Failed to initialize storage", e)
            false
        }
    }
    
    fun getSettingsFile(): File {
        return settingsFileObj ?: File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), SETTINGS_FILE_NAME)
    }
    
    fun getLogsDir(): File {
        return logsDirectory ?: File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), LOGS_DIR_NAME)
    }

    fun getDataDir(): File {
        return dataDirectory ?: File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DATA_DIR_NAME)
    }

    fun getBaseDir(): File {
        return baseDirectory ?: File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APP_DIR_NAME)
    }
    
    fun isStorageAvailable(): Boolean {
        return settingsFileObj != null
    }
    
    fun getFileContent(file: File): String? {
        return try {
            if (file.exists() && file.canRead()) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Logger.e("StorageManager", "Error reading file: ${file.absolutePath}", e)
            null
        }
    }
    
    fun setFileContent(file: File, content: String): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.writeText(content)
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
            Base Directory: ${baseDirectory?.absolutePath ?: "Not Initialized"}
            Settings File: ${settingsFileObj?.absolutePath ?: "Not Initialized"}
            Logs Directory: ${logsDirectory?.absolutePath ?: "Not Initialized"}
            Data Directory: ${dataDirectory?.absolutePath ?: "Not Initialized"}
            External Storage State: ${Environment.getExternalStorageState()}
            Storage Available: ${isStorageAvailable()}
            Base Dir Exists: ${baseDirectory?.exists() ?: false}
            Settings File Exists: ${settingsFileObj?.exists() ?: false}
            """.trimIndent()
        } catch (e: Exception) {
            "Error getting storage info: ${e.message}"
        }
    }
}