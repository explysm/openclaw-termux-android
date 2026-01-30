package com.explysm.openclaw.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast

object TermuxRunner {

    private const val TERMUX_PACKAGE_NAME = "com.termux"
    private const val TERMUX_API_PACKAGE_NAME = "com.termux.api"
    private const val TERMUX_SERVICE = "com.termux.app.TermuxService"
    private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    private const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    private const val EXTRA_NOTIFICATION_TITLE = "com.termux.RUN_COMMAND_NOTIFICATION_TITLE"
    private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    private const val EXTRA_FAIL_ON_ERROR = "com.termux.RUN_COMMAND_FAIL_ON_ERROR"


    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE_NAME, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isRunCommandAvailable(context: Context): Boolean {
        if (!isTermuxInstalled(context)) return false
        val intent = Intent(ACTION_RUN_COMMAND).apply {
            setPackage(TERMUX_PACKAGE_NAME)
        }
        return intent.resolveActivity(context.packageManager) != null
    }

    fun runCommand(
        context: Context,
        command: String,
        notificationTitle: String,
        background: Boolean = true,
        failOnError: Boolean = false,
        workingDirectory: String? = null
    ): Boolean {
        if (!isTermuxInstalled(context)) {
            Toast.makeText(context, "Termux is not installed. Please install Termux from F-Droid.", Toast.LENGTH_LONG).show()
            // Try Play Store first, fall back to browser
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$TERMUX_PACKAGE_NAME"))
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/$TERMUX_PACKAGE_NAME/"))
            
            try {
                if (marketIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(marketIntent)
                } else {
                    context.startActivity(webIntent)
                }
            } catch (e: Exception) {
                context.startActivity(webIntent)
            }
            return false
        }

        val intent = Intent(ACTION_RUN_COMMAND).apply {
            putExtra(EXTRA_COMMAND_PATH, "/data/data/com.termux/files/usr/bin/bash")
            putExtra(EXTRA_ARGUMENTS, arrayOf("-c", command))
            putExtra(EXTRA_BACKGROUND, background)
            putExtra(EXTRA_NOTIFICATION_TITLE, notificationTitle)
            putExtra(EXTRA_FAIL_ON_ERROR, failOnError)
            workingDirectory?.let {
                putExtra(EXTRA_WORKDIR, it)
            }
            setPackage(TERMUX_PACKAGE_NAME)
            component = ComponentName(TERMUX_PACKAGE_NAME, "com.termux.app.TermuxServiceReceiver")
        }
        try {
            // Check if Termux can handle this intent
            if (intent.resolveActivity(context.packageManager) == null) {
                throw Exception("Termux RUN_COMMAND not available. Please ensure Termux and Termux:API are properly installed.")
            }
            
            if (background) {
                // For background commands, broadcast to Termux service receiver
                context.sendBroadcast(intent)
                Toast.makeText(context, "Termux background command sent.", Toast.LENGTH_SHORT).show()
                return true
            } else {
                // For foreground commands, launch Termux activity
                context.startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                Toast.makeText(context, "Termux foreground command launched.", Toast.LENGTH_SHORT).show()
                return true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to run Termux command: ${e.message}", Toast.LENGTH_LONG).show()
            // Fallback: Launch Termux app directly so user can run commands manually
            openTermuxApp(context)
            return false
        }
    }

    fun openTermuxApp(context: Context): Boolean {
        val launchAppIntent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE_NAME)
        return if (launchAppIntent?.resolveActivity(context.packageManager) != null) {
            context.startActivity(launchAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } else {
            Toast.makeText(context, "Cannot launch Termux. Please open Termux manually.", Toast.LENGTH_LONG).show()
            false
        }
    }
}
