package com.explysm.openclaw.data

import kotlinx.serialization.Serializable

@Serializable
data class SettingsData(
    val apiUrl: String = "http://127.0.0.1:5039",
    val pollInterval: Int = 10,
    val darkTheme: Boolean = false,
    val autoStart: Boolean = false,
    val enableNotifications: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val postOnboardingHelpShown: Boolean = false
)