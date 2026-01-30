package com.explysm.openclaw.utils

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

    fun runCommand(
        context: Context,
        command: String,
        notificationTitle: String,
        background: Boolean = true,
        failOnError: Boolean = false,
        workingDirectory: String? = null
    ) {
        if (!isTermuxInstalled(context)) {
            Toast.makeText(context, "Termux is not installed.", Toast.LENGTH_LONG).show()
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$TERMUX_PACKAGE_NAME"))
            context.startActivity(marketIntent)
            return
        }

        val intent = Intent(ACTION_RUN_COMMAND).apply {
            setClassName(TERMUX_PACKAGE_NAME, TERMUX_SERVICE)
            putExtra(EXTRA_COMMAND_PATH, "bash")
            putExtra(EXTRA_ARGUMENTS, arrayOf("-c", command))
            putExtra(EXTRA_BACKGROUND, background)
            putExtra(EXTRA_NOTIFICATION_TITLE, notificationTitle)
            putExtra(EXTRA_FAIL_ON_ERROR, failOnError)
            workingDirectory?.let {
                putExtra(EXTRA_WORKDIR, it)
            }
        }
        try {
            if (background) {
                // For background commands, try sending as a broadcast
                context.sendBroadcast(intent)
                Toast.makeText(context, "Termux background command sent.", Toast.LENGTH_SHORT).show()
            } else {
                // For foreground commands or as a fallback, launch Termux directly
                context.startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                Toast.makeText(context, "Termux foreground command launched.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to run Termux command: ${e.message}", Toast.LENGTH_LONG).show()
            // Fallback: Launch Termux app if the above fails
            val launchAppIntent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE_NAME)
            launchAppIntent?.let {
                context.startActivity(it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
