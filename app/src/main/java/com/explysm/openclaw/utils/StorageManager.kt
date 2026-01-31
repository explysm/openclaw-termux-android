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
    
    private val baseDir: File by lazy {
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APP_DIR_NAME)
    }
    
    private val settingsFile: File by lazy {
        File(baseDir, SETTINGS_FILE_NAME)
    }
    
    private val logsDir: File by lazy {
        File(baseDir, LOGS_DIR_NAME)
    }
    
    private val dataDir: File by lazy {
        File(baseDir, DATA_DIR_NAME)
    }
    
    fun initialize(context: Context): Boolean {
        return try {
            // Create main app directory
            if (!baseDir.exists()) {
                val created = baseDir.mkdirs()
                Logger.i("StorageManager", "Created base directory: $baseDir, success: $created")
            }
            
            // Create logs directory
            if (!logsDir.exists()) {
                val created = logsDir.mkdirs()
                Logger.i("StorageManager", "Created logs directory: $logsDir, success: $created")
            }
            
            // Create data directory
            if (!dataDir.exists()) {
                val created = dataDir.mkdirs()
                Logger.i("StorageManager", "Created data directory: $dataDir, success: $created")
            }
            
            // Verify external storage is writable
            val writable = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
            Logger.i("StorageManager", "External storage writable: $writable")
            
            writable && baseDir.exists() && logsDir.exists() && dataDir.exists()
        } catch (e: Exception) {
            Logger.e("StorageManager", "Failed to initialize storage directories", e)
            false
        }
    }
    
    fun getSettingsFile(): File {
        return settingsFile
    }
    
    fun getLogsDir(): File {
        return logsDir
    }
    
    fun getDataDir(): File {
        return dataDir
    }
    
    fun getBaseDir(): File {
        return baseDir
    }
    
    fun isStorageAvailable(): Boolean {
        return try {
            Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && baseDir.exists()
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
            Base Directory: ${baseDir.absolutePath}
            Settings File: ${settingsFile.absolutePath}
            Logs Directory: ${logsDir.absolutePath}
            Data Directory: ${dataDir.absolutePath}
            External Storage State: ${Environment.getExternalStorageState()}
            Storage Available: ${isStorageAvailable()}
            Base Dir Exists: ${baseDir.exists()}
            Settings File Exists: ${settingsFile.exists()}
            """.trimIndent()
        } catch (e: Exception) {
            "Error getting storage info: ${e.message}"
        }
    }
}