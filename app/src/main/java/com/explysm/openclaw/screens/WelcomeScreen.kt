package com.explysm.openclaw.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.explysm.openclaw.data.SettingsRepository
import com.explysm.openclaw.utils.Logger
import com.explysm.openclaw.utils.TermuxRunner

@Composable
fun WelcomeScreen(navController: NavController, settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val onboardingCompleted by settingsRepository.onboardingCompleted.collectAsState(initial = false)
    var isNavigating by remember { mutableStateOf(false) }

    Logger.i("WelcomeScreen", "Composed. onboardingCompleted=$onboardingCompleted")

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isNavigating) {
            CircularProgressIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "OpenClaw",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your personal AI companion",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = {
                        if (isNavigating) return@Button
                        isNavigating = true
                        
                        try {
                            if (onboardingCompleted) {
                                Logger.i("WelcomeScreen", "Navigating to main")
                                navController.navigate("main")
                            } else {
                                if (TermuxRunner.isTermuxInstalled(context)) {
                                    Logger.i("WelcomeScreen", "Navigating to onboarding")
                                    navController.navigate("onboarding")
                                } else {
                                    isNavigating = false
                                    Toast.makeText(context, "Install Termux first", Toast.LENGTH_LONG).show()
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux/"))
                                    context.startActivity(intent)
                                }
                            }
                        } catch (e: Exception) {
                            Logger.e("WelcomeScreen", "Navigation failed", e)
                            isNavigating = false
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(if (onboardingCompleted) "Go to Dashboard" else "Get Started")
                }
            }
        }
    }
}
