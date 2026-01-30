package com.explysm.openclaw.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

import androidx.navigation.NavController
import com.explysm.openclaw.data.SettingsRepository
import com.explysm.openclaw.utils.TermuxRunner

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OnboardingScreen(navController: NavController, settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRunCommandAvailable by remember { mutableStateOf<Boolean?>(null) }
    var showManualSetup by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Check if RUN_COMMAND is available
        val runCommandAvailable = TermuxRunner.isRunCommandAvailable(context)
        isRunCommandAvailable = runCommandAvailable
        
        if (runCommandAvailable) {
            // Run initial setup script
            TermuxRunner.runCommand(
                context,
                "export ANDROID_APP=1 && curl -s https://explysm.github.io/moltbot-termux/install.sh | sh",
                "OpenClaw Setup",
                background = true
            )
            // Chain with ttyd for onboarding
            TermuxRunner.runCommand(
                context,
                "pkg install ttyd -y && ttyd -p 7681 --interface 127.0.0.1 --writable --once bash -c \"moltbot onboard\"",
                "OpenClaw Onboarding",
                background = false // Keep this in foreground for user to see
            )
        } else {
            // Show manual setup UI
            showManualSetup = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isRunCommandAvailable == null -> {
                // Loading state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Checking Termux setup...")
                }
            }
            showManualSetup -> {
                // Manual setup UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        "Manual Setup Required",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    Text(
                        "Termux:API is not configured. Please run these commands manually in Termux:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Installation commands card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Installation Commands:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val commands = """export ANDROID_APP=1
curl -s https://explysm.github.io/moltbot-termux/install.sh | sh"""
                                        val clip = ClipData.newPlainText("Installation Commands", commands)
                                        clipboard.setPrimaryClip(clip)
                                    }
                                ) {
                                    Text("Copy")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = """export ANDROID_APP=1
curl -s https://explysm.github.io/moltbot-termux/install.sh | sh""",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Onboarding command card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Then run onboarding:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val command = """pkg install ttyd -y && ttyd -p 7681 --interface 127.0.0.1 --writable --once bash -c "moltbot onboard""""
                                        val clip = ClipData.newPlainText("Onboarding Command", command)
                                        clipboard.setPrimaryClip(clip)
                                    }
                                ) {
                                    Text("Copy")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = """pkg install ttyd -y && ttyd -p 7681 --interface 127.0.0.1 --writable --once bash -c "moltbot onboard"""",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            TermuxRunner.openTermuxApp(context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Termux")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "After running the commands in Termux, return here and tap the checkmark button",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                // Automatic setup - navigate to terminal screen after brief delay
                LaunchedEffect(Unit) {
                    delay(2000) // Give time for ttyd to start
                    navController.navigate("onboarding_terminal") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
                
                // Show loading while preparing terminal
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Starting onboarding terminal...")
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
            }
        }

        // Show Done/Continue button
        if (isRunCommandAvailable != null) {
            FloatingActionButton(
                onClick = {
                    if (showManualSetup) {
                        // Manual setup: go to terminal first
                        navController.navigate("onboarding_terminal") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    } else {
                        // Automatic setup: skip terminal, go to main
                        scope.launch {
                            // Save onboarding completed FIRST to prevent race condition
                            settingsRepository.setOnboardingCompleted(true)
                            // Then navigate
                            navController.navigate("main") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Done, if (showManualSetup) "Continue" else "Done")
            }
        }
    }
}
