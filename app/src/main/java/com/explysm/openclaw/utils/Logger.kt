package com.explysm.openclaw.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

object Logger {
    enum class Level {
        DEBUG, INFO, WARN, ERROR
    }

    private var internalLogFile: File? = null
    private var internalWriter: BufferedWriter? = null
    private var externalLogFile: File? = null
    private var externalWriter: BufferedWriter? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val isWriting = AtomicBoolean(false)
    private var externalStorageEnabled = false

    fun init(context: Context) {
        try {
            // Initialize internal logging (fallback)
            val internalLogsDir = File(context.filesDir, "logs").apply { mkdirs() }
            internalLogFile = File(internalLogsDir, "app.log")
            internalWriter = BufferedWriter(FileWriter(internalLogFile, true))
            
            // Initialize external logging (primary)
            try {
                externalStorageEnabled = StorageManager.initialize(context)
                if (externalStorageEnabled) {
                    val externalLogsDir = StorageManager.getLogsDir()
                    externalLogFile = File(externalLogsDir, "app.log")
                    externalWriter = BufferedWriter(FileWriter(externalLogFile, true))
                    i("Logger", "External logging enabled at: ${externalLogFile?.absolutePath}")
                }
            } catch (e: Exception) {
                w("Logger", "Failed to initialize external logging, using internal only", e)
                externalStorageEnabled = false
            }
            
            i("Logger", "Logger initialized. Internal: ${internalLogFile?.absolutePath}, External enabled: $externalStorageEnabled")
        } catch (e: Exception) {
            android.util.Log.e("OpenClawLogger", "Failed to initialize logger", e)
        }
    }

    fun d(tag: String, message: String) {
        log(Level.DEBUG, tag, message)
    }

    fun i(tag: String, message: String) {
        log(Level.INFO, tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.WARN, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.ERROR, tag, message, throwable)
    }

    private fun log(level: Level, tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        val logEntry = StringBuilder()
            .append("[").append(timestamp).append("]")
            .append(" [").append(level.name).append("]")
            .append(" [").append(tag).append("] ")
            .append(message)

        throwable?.let {
            logEntry.append("\nException: ").append(it.message)
            logEntry.append("\nStack trace:\n")
            logEntry.append(it.stackTraceToString())
        }

        val logLine = logEntry.toString()

        // Always log to logcat immediately
        android.util.Log.println(
            when (level) {
                Level.DEBUG -> android.util.Log.DEBUG
                Level.INFO -> android.util.Log.INFO
                Level.WARN -> android.util.Log.WARN
                Level.ERROR -> android.util.Log.ERROR
            },
            tag,
            message
        )

        throwable?.let {
            android.util.Log.e(tag, "Exception", it)
        }

        // Write to files immediately with flush
        try {
            // Write to external storage if available
            externalWriter?.let { w ->
                synchronized(w) {
                    w.write(logLine)
                    w.newLine()
                    w.newLine()
                    w.flush()
                }
            }
            
            // Also write to internal storage as backup
            internalWriter?.let { w ->
                synchronized(w) {
                    w.write(logLine)
                    w.newLine()
                    w.newLine()
                    w.flush()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OpenClawLogger", "Failed to write to log files", e)
        }
    }

    fun getLogContents(): String {
        return try {
            // Try external log first, fallback to internal
            externalWriter?.flush()
            internalWriter?.flush()
            
            val externalContent = externalLogFile?.readText()
            val internalContent = internalLogFile?.readText()
            
            when {
                externalContent != null && externalContent.isNotBlank() -> externalContent
                internalContent != null && internalContent.isNotBlank() -> internalContent
                else -> "Log files not available"
            }
        } catch (e: Exception) {
            "Error reading log: ${e.message}"
        }
    }

    fun getLogFilePaths(): String {
        return buildString {
            appendLine("Log File Paths:")
            if (externalStorageEnabled && externalLogFile != null) {
                appendLine("External (Primary): ${externalLogFile?.absolutePath}")
            } else {
                appendLine("External: Not available")
            }
            appendLine("Internal (Backup): ${internalLogFile?.absolutePath}")
        }
    }

    fun getExternalLogPath(): String? {
        return if (externalStorageEnabled) externalLogFile?.absolutePath else null
    }

    fun getInternalLogPath(): String? {
        return internalLogFile?.absolutePath
    }

    fun clearLogs() {
        try {
            // Clear external log
            synchronized(externalWriter ?: return) {
                externalWriter?.close()
                externalLogFile?.writeText("")
                externalLogFile?.let { file ->
                    externalWriter = BufferedWriter(FileWriter(file, true))
                }
            }
            
            // Clear internal log
            synchronized(internalWriter ?: return) {
                internalWriter?.close()
                internalLogFile?.writeText("")
                internalLogFile?.let { file ->
                    internalWriter = BufferedWriter(FileWriter(file, true))
                }
            }
            
            i("Logger", "All logs cleared")
        } catch (e: Exception) {
            android.util.Log.e("OpenClawLogger", "Failed to clear logs", e)
        }
    }

    fun clearExternalLogs() {
        try {
            synchronized(externalWriter ?: return) {
                externalWriter?.close()
                externalLogFile?.writeText("")
                externalLogFile?.let { file ->
                    externalWriter = BufferedWriter(FileWriter(file, true))
                }
            }
            i("Logger", "External logs cleared")
        } catch (e: Exception) {
            android.util.Log.e("OpenClawLogger", "Failed to clear external logs", e)
        }
    }

    fun isExternalLoggingEnabled(): Boolean {
        return externalStorageEnabled
    }

    fun getLoggerInfo(): String {
        return buildString {
            appendLine("Logger Information:")
            appendLine("External Storage Enabled: $externalStorageEnabled")
            appendLine(getLogFilePaths())
            appendLine("Storage Available: ${StorageManager.isStorageAvailable()}")
            appendLine("Storage Base Dir: ${StorageManager.getBaseDir().absolutePath}")
        }
    }
}
