package com.explysm.openclaw.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.explysm.openclaw.data.SettingsRepository
import com.explysm.openclaw.utils.TermuxRunner

@Composable
fun WelcomeScreen(navController: NavController, settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val onboardingCompleted by settingsRepository.onboardingCompleted.collectAsState(initial = false)
    var hasNavigated by remember { mutableStateOf(false) }

    // Auto-navigate if onboarding is already completed
    LaunchedEffect(onboardingCompleted) {
        if (onboardingCompleted && !hasNavigated) {
            hasNavigated = true
            navController.navigate("main") {
                popUpTo("welcome") { inclusive = true }
            }
        }
    }

    // Show loading while checking
    if (onboardingCompleted) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading...")
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to OpenClaw Android 🦞 – Install & onboard your personal AI",
            fontSize = 24.sp,
            modifier = Modifier.padding(16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            if (TermuxRunner.isTermuxInstalled(context)) {
                navController.navigate("onboarding")
            } else {
                Toast.makeText(context, "Install Termux first", Toast.LENGTH_LONG).show()
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://f-droid.org/packages/com.termux/")
                }
                context.startActivity(intent)
            }
        }) {
            Text(text = "Install & Onboard")
        }
    }
}
