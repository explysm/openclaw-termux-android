package com.explysm.openclaw.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.explysm.openclaw.data.SettingsRepository
import com.explysm.openclaw.utils.ApiClient
import com.explysm.openclaw.utils.TermuxRunner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.min
import kotlin.math.pow

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MainScreen(navController: NavController, settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Collect settings
    val apiUrl by settingsRepository.apiUrl.collectAsState(initial = SettingsRepository.DEFAULT_API_URL)
    val pollInterval by settingsRepository.pollInterval.collectAsState(initial = SettingsRepository.DEFAULT_POLL_INTERVAL)
    
    var status by remember { mutableStateOf("Unknown") }
    var chatLogs by remember { mutableStateOf("Welcome to OpenClaw chat.") }
    var isConnected by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }
    
    // Smart retry logic with exponential backoff
    var consecutiveFailures by remember { mutableIntStateOf(0) }
    val maxRetryDelay = 60_000L // Max 60 seconds
    val baseDelay = pollInterval * 1000L
    
    fun calculateRetryDelay(): Long {
        return if (consecutiveFailures == 0) {
            baseDelay
        } else {
            min(baseDelay * (2.0.pow(consecutiveFailures.toDouble())).toLong(), maxRetryDelay)
        }
    }
    
    fun refreshStatus(onComplete: (() -> Unit)? = null) {
        scope.launch {
            isRefreshing = true
            ApiClient.get("$apiUrl/api/status") { result ->
                result.onSuccess {
                    status = if (it.contains("running", ignoreCase = true)) "Running" else "Stopped"
                    isConnected = true
                    consecutiveFailures = 0
                    lastError = null
                }.onFailure { e ->
                    isConnected = false
                    consecutiveFailures++
                    lastError = e.message
                    if (consecutiveFailures >= 3) {
                        status = "Error"
                    }
                }
                isRefreshing = false
                onComplete?.invoke()
            }
        }
    }
    
    fun refreshChat() {
        ApiClient.get("$apiUrl/api/chat") { result ->
            result.onSuccess {
                if (it.isNotBlank()) {
                    chatLogs = it
                }
            }.onFailure {
                // Silently fail for chat updates
            }
        }
    }
    
    fun exportLogs() {
        try {
            val timestamp = System.currentTimeMillis()
            val fileName = "openclaw_logs_$timestamp.txt"
            val file = File(context.cacheDir, fileName)
            file.writeText(chatLogs)
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "OpenClaw Logs")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Export Logs"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export logs: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Initial load and polling
    LaunchedEffect(apiUrl, pollInterval) {
        // Initial check
        refreshStatus()
        refreshChat()
        
        // Status polling loop with smart retry
        while (true) {
            val delayMs = calculateRetryDelay()
            delay(delayMs)
            refreshStatus()
        }
    }
    
    LaunchedEffect(apiUrl) {
        // Chat polling loop
        while (true) {
            delay(5000)
            refreshChat()
        }
    }
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { refreshStatus() }
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("OpenClaw: $status")
                        // Connection status indicator
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when {
                                isConnected && status == "Running" -> MaterialTheme.colorScheme.tertiary
                                isConnected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }
                },
                actions = {
                    IconButton(onClick = { refreshStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { exportLogs() }) {
                        Icon(Icons.Default.Share, contentDescription = "Export Logs")
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Connection status chip
                if (!isConnected || lastError != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "⚠",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    if (isConnected) "Connection unstable" else "API unreachable",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (!isConnected) {
                                    Text(
                                        "Is 'moltbot --android-app' running on termux?",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                lastError?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            if (!isConnected) {
                                Button(
                                    onClick = { TermuxRunner.openTermuxApp(context) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Open Termux")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Retry delay indicator
                if (consecutiveFailures > 0) {
                    Text(
                        "Retrying in ${calculateRetryDelay() / 1000}s (attempt ${consecutiveFailures})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        ApiClient.post("$apiUrl/api/start", "") { result ->
                            result.onSuccess {
                                status = "Running"
                                Toast.makeText(context, "OpenClaw started.", Toast.LENGTH_SHORT).show()
                            }.onFailure { e ->
                                Toast.makeText(context, "API failed, trying Termux...", Toast.LENGTH_SHORT).show()
                                val success = TermuxRunner.runCommand(
                                    context,
                                    "moltbot --android-app &",
                                    "OpenClaw Gateway",
                                    background = true
                                )
                                if (success) {
                                    Toast.makeText(context, "Command sent to Termux. Checking status...", Toast.LENGTH_SHORT).show()
                                    // Wait a moment then check if it actually started
                                    scope.launch {
                                        delay(3000)
                                        refreshStatus()
                                    }
                                } else {
                                    Toast.makeText(context, "Failed to send command to Termux", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Start OpenClaw", fontSize = 20.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        ApiClient.post("$apiUrl/api/stop", "") { result ->
                            result.onSuccess {
                                status = "Stopped"
                                Toast.makeText(context, "OpenClaw stopped.", Toast.LENGTH_SHORT).show()
                            }.onFailure { e ->
                                Toast.makeText(context, "API failed, trying Termux...", Toast.LENGTH_SHORT).show()
                                val success = TermuxRunner.runCommand(
                                    context,
                                    "pkill -f \"moltbot gateway\"",
                                    "Stop OpenClaw Gateway",
                                    background = true
                                )
                                if (success) {
                                    Toast.makeText(context, "Stop command sent to Termux", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to send stop command", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(text = "Stop OpenClaw", fontSize = 20.sp)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(text = "Chat/Logs:", fontSize = 18.sp)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = chatLogs,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "Terminal/Logs (ttyd):", fontSize = 18.sp)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webViewClient = object : WebViewClient() {
                                    override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                    ) {
                                        // Suppress WebView errors - ttyd may not be running yet
                                        // This prevents crashes when the local server is unavailable
                                    }
                                }
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                loadUrl("http://127.0.0.1:7681")
                            }
                        },
                        update = {
                            // Don't reload URL here - it causes constant reconnections
                            // The factory already loads the URL initially
                        }
                    )
                }
            }
            
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
