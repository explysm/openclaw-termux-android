package com.explysm.openclaw.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
            if (isRunCommandAvailable != null) {
                FloatingActionButton(onClick = {
                    scope.launch {
                        settingsRepository.setOnboardingCompleted(true)
                        navController.navigate("main") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }) {
                    Icon(Icons.Default.Done, contentDescription = "Done")
                }
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
            
            Text("Please run this command in Termux:")
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val command = "curl -s https://explysm.github.io/openclaw-termux/install-android-app.sh | sh"
                    Text(text = command, fontWeight = FontWeight.Bold)
                    Button(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Command", command))
                    }) {
                        Text("Copy Command")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = { TermuxRunner.openTermuxApp(context) }) {
                Text("Open Termux")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (isRunCommandAvailable == true) {
                Button(onClick = {
                    TermuxRunner.runCommand(
                        context,
                        "curl -s https://explysm.github.io/openclaw-termux/install-android-app.sh | sh",
                        "OpenClaw Setup"
                    )
                }) {
                    Text("Run Automatically")
                }
            }
        }
    }
}