package com.explysm.openclaw.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.explysm.openclaw.data.SettingsRepository
import com.explysm.openclaw.utils.ApiClient
import com.explysm.openclaw.utils.Logger
import com.explysm.openclaw.utils.StorageManager
import com.explysm.openclaw.utils.TermuxRunner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val apiUrl by settingsRepository.apiUrl.collectAsState(initial = SettingsRepository.DEFAULT_API_URL)
    val pollInterval by settingsRepository.pollInterval.collectAsState(initial = 10)
    
    var status by remember { mutableStateOf("Unknown") }
    var isConnected by remember { mutableStateOf(true) }
    var lastError by remember { mutableStateOf<String?>(null) }
    
    fun refreshStatus() {
        ApiClient.get("$apiUrl/api/status") { result ->
            result.onSuccess { response ->
                status = if (response.contains("running", ignoreCase = true)) "Running" else "Stopped"
                isConnected = true
                lastError = null
            }.onFailure { e ->
                isConnected = false
                lastError = e.message
                status = "Error"
            }
        }
    }
    
    LaunchedEffect(apiUrl, pollInterval) {
        while (true) {
            refreshStatus()
            delay(pollInterval * 1000L)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpenClaw: $status") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
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
            // Connection status warning
            if (!isConnected) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("⚠", fontSize = 24.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "API unreachable",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Is 'moltbot --android-app' running in Termux?",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(
                            onClick = { TermuxRunner.openTermuxApp(context) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Open Termux")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    ApiClient.post("$apiUrl/api/start", "") { result ->
                        result.onSuccess { 
                            status = "Running"
                            isConnected = true
                        }.onFailure { e ->
                            isConnected = false
                            lastError = e.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    ApiClient.post("$apiUrl/api/stop", "") { result ->
                        result.onSuccess { 
                            status = "Stopped"
                            isConnected = true
                        }.onFailure { e ->
                            isConnected = false
                            lastError = e.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Stop")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Terminal:")
            
            Card(
                modifier = Modifier.fillMaxWidth().height(400.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            loadUrl("http://127.0.0.1:7681")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
