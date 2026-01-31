package com.explysm.openclaw.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.explysm.openclaw.R
import com.explysm.openclaw.data.SettingsRepository
import com.explysm.openclaw.utils.Logger
import com.explysm.openclaw.utils.TermuxRunner

@Composable
fun WelcomeScreen(navController: NavController, settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val onboardingCompleted by settingsRepository.onboardingCompleted.collectAsState(initial = null)
    var isNavigating by remember { mutableStateOf(false) }

    // Auto-navigate if onboarding is already completed
    LaunchedEffect(onboardingCompleted) {
        if (onboardingCompleted == true && !isNavigating) {
            isNavigating = true
            navController.navigate("main") {
                popUpTo("welcome") { inclusive = true }
            }
        }
    }

    // Show loading while checking or navigating
    if (onboardingCompleted == null || onboardingCompleted == true || isNavigating) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo / Icon
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_lobster),
                    contentDescription = "OpenClaw Logo",
                    modifier = Modifier.size(100.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "OpenClaw",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your personal AI companion, powered by Termux.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = {
                    val isTermuxInstalled = TermuxRunner.isTermuxInstalled(context)
                    if (isTermuxInstalled) {
                        navController.navigate("onboarding")
                    } else {
                        Toast.makeText(context, "Install Termux first", Toast.LENGTH_LONG).show()
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://f-droid.org/packages/com.termux/")
                        }
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://github.com/explysm/openclaw")
                }
                context.startActivity(intent)
            }) {
                Text("Learn more on GitHub")
            }
        }
    }
}