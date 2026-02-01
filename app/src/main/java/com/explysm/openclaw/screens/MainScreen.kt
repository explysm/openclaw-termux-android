package com.explysm.openclaw.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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
    val settings by settingsRepository.settings.collectAsState()
    
    var status by remember { mutableStateOf("Unknown") }
    var isConnected by remember { mutableStateOf(true) }
    var lastError by remember { mutableStateOf<String?>(null) }
    var showPostOnboardingDialog by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    // Determine terminal URL from API URL
    val terminalUrl = remember(apiUrl) {
        try {
            val uri = android.net.Uri.parse(apiUrl)
            "http://${uri.host ?: "127.0.0.1"}:7681"
        } catch (e: Exception) {
            "http://127.0.0.1:7681"
        }
    }
    
    // Check if we should show the post-onboarding dialog
    LaunchedEffect(settings) {
        if (settings.onboardingCompleted && !settings.postOnboardingHelpShown) {
            showPostOnboardingDialog = true
        }
    }
    
    fun refreshStatus() {
        ApiClient.get("$apiUrl/api/status") { result ->
            result.onSuccess { response ->
                val newStatus = if (response.contains("running", ignoreCase = true)) "Running" else "Stopped"
                if (status != "Running" && newStatus == "Running") {
                    // Service just started, reload terminal
                    scope.launch {
                        delay(1500) // Give ttyd a moment to start
                        webViewRef?.reload()
                    }
                }
                status = newStatus
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
    
    if (showPostOnboardingDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Setup Complete!") },
            text = {
                Column {
                    Text("To start the OpenClaw API, please open Termux and run the following command:")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val command = "moltbot --android-app"
                    
                    // Code block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = command,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Moltbot Command", command)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Command copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                // Simplified "Copy" text as icon isn't standard in M3 Icons
                                Text("Copy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    TermuxRunner.openTermuxApp(context)
                }) {
                    Text("Open Termux")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPostOnboardingDialog = false
                    scope.launch {
                        settingsRepository.setPostOnboardingHelpShown(true)
                    }
                }) {
                    Text("Close")
                }
            }
        )
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
                            scope.launch {
                                delay(1500)
                                webViewRef?.reload()
                            }
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Terminal:", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { 
                    Logger.i("MainScreen", "Manual terminal refresh requested")
                    webViewRef?.reload() 
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Terminal")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewRef = this
                            @SuppressLint("SetJavaScriptEnabled")
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            settings.setSupportZoom(false)
                            
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Logger.d("MainScreen", "Terminal WebView finished loading: $url")
                                }
                                
                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    Logger.e("MainScreen", "Terminal WebView error: $errorCode - $description")
                                }
                            }
                            
                            setBackgroundColor(android.graphics.Color.BLACK)
                            Logger.i("MainScreen", "Loading terminal URL: $terminalUrl")
                            loadUrl(terminalUrl)
                        }
                    },
                    update = {
                        webViewRef = it
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}