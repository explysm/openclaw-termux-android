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

    private var logFile: File? = null
    private var writer: BufferedWriter? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val isWriting = AtomicBoolean(false)

    fun init(context: Context) {
        try {
            val logsDir = File(context.filesDir, "logs").apply { mkdirs() }
            logFile = File(logsDir, "app.log")
            writer = BufferedWriter(FileWriter(logFile, true))
            i("Logger", "Logger initialized. Log file: ${logFile?.absolutePath}")
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

        // Write to file immediately with flush
        try {
            writer?.let { w ->
                synchronized(w) {
                    w.write(logLine)
                    w.newLine()
                    w.newLine()
                    w.flush()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OpenClawLogger", "Failed to write to log file", e)
        }
    }

    fun getLogContents(): String {
        return try {
            writer?.flush()
            logFile?.readText() ?: "Log file not available"
        } catch (e: Exception) {
            "Error reading log: ${e.message}"
        }
    }

    fun getLogFilePath(): String? {
        return logFile?.absolutePath
    }

    fun clearLogs() {
        try {
            synchronized(writer ?: return) {
                writer?.close()
                logFile?.writeText("")
                writer = BufferedWriter(FileWriter(logFile, true))
            }
        } catch (e: Exception) {
            android.util.Log.e("OpenClawLogger", "Failed to clear logs", e)
        }
    }
}
