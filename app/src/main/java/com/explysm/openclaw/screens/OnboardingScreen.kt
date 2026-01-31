package com.explysm.openclaw.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.explysm.openclaw.data.SettingsRepository
import com.explysm.openclaw.utils.Logger
import com.explysm.openclaw.utils.TermuxRunner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(navController: NavController, settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRunCommandAvailable by remember { mutableStateOf<Boolean?>(null) }
    
    LaunchedEffect(Unit) {
        isRunCommandAvailable = TermuxRunner.isRunCommandAvailable(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Onboarding") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                Logger.i("OnboardingScreen", "Navigating to terminal screen")
                navController.navigate("onboarding_terminal")
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Continue")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Install OpenClaw", style = MaterialTheme.typography.headlineMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Please run this command in Termux to start the installation:", textAlign = TextAlign.Center)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val command = "curl -s https://explysm.github.io/openclaw-termux/install-android-app.sh | sh"
            
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = command, 
                        fontWeight = FontWeight.Bold, 
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Command", command))
                    }) {
                        Text("Copy Command")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { TermuxRunner.openTermuxApp(context) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Open Termux", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isRunCommandAvailable == true) {
                Button(
                    onClick = {
                        TermuxRunner.runCommand(
                            context,
                            command,
                            "OpenClaw Setup"
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Run Automatically", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "After the command finishes in Termux, return here and tap the arrow button below to view the setup terminal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}