package com.explysm.openclaw.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.min
import kotlin.math.pow

// JavaScript interface for file operations
class FileInterface(private val context: android.content.Context) {
    @JavascriptInterface
    fun getStorageInfo(): String {
        return try {
            StorageManager.getStorageInfo()
        } catch (e: Exception) {
            "Error getting storage info: ${e.message}"
        }
    }
    
    @JavascriptInterface
    fun getSettingsContent(): String {
        return try {
            StorageManager.getFileContent(StorageManager.getSettingsFile()) ?: "{}"
        } catch (e: Exception) {
            "{\"error\": \"Error reading settings: ${e.message}\"}"
        }
    }
    
    @JavascriptInterface
    fun getLogContent(): String {
        return try {
            Logger.getLogContents()
        } catch (e: Exception) {
            "Error reading logs: ${e.message}"
        }
    }
    
    @JavascriptInterface
    fun getLogPaths(): String {
        return try {
            Logger.getLogFilePaths()
        } catch (e: Exception) {
            "Error getting log paths: ${e.message}"
        }
    }
    
    @JavascriptInterface
    fun getExternalLogPath(): String? {
        return try {
            Logger.getExternalLogPath()
        } catch (e: Exception) {
            null
        }
    }
    
    @JavascriptInterface
    fun listFiles(directory: String): String {
        return try {
            val dir = when (directory) {
                "logs" -> StorageManager.getLogsDir()
                "data" -> StorageManager.getDataDir()
                "base" -> StorageManager.getBaseDir()
                else -> StorageManager.getBaseDir()
            }
            
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.joinToString("\n") { file ->
                    "${if (file.isDirectory) "DIR" else "FILE"}: ${file.name} (${file.length()} bytes)"
                } ?: "No files"
            } else {
                "Directory not found: ${dir.absolutePath}"
            }
        } catch (e: Exception) {
            "Error listing files: ${e.message}"
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MainScreen(navController: NavController, settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    Logger.i("MainScreen", "MainScreen composing...")
    
    // Collect settings
    val apiUrl by settingsRepository.apiUrl.collectAsState(initial = SettingsRepository.DEFAULT_API_URL)
    val pollInterval by settingsRepository.pollInterval.collectAsState(initial = SettingsRepository.DEFAULT_POLL_INTERVAL)
    
    Logger.d("MainScreen", "Settings collected: apiUrl=$apiUrl, pollInterval=$pollInterval")
    
    var status by remember { mutableStateOf("Unknown") }
    var isConnected by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }
    
    // Smart retry logic with exponential backoff
    var consecutiveFailures by remember { mutableStateOf(0) }
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
        Logger.d("MainScreen", "refreshStatus() called")
        scope.launch {
            isRefreshing = true
            val url = "$apiUrl/api/status"
            Logger.d("MainScreen", "Making API call to: $url")
            ApiClient.get(url) { result ->
                result.onSuccess { response ->
                    Logger.d("MainScreen", "API success: $response")
                    status = if (response.contains("running", ignoreCase = true)) "Running" else "Stopped"
                    isConnected = true
                    consecutiveFailures = 0
                    lastError = null
                }.onFailure { e ->
                    Logger.w("MainScreen", "API failed: ${e.message}", e)
                    isConnected = false
                    consecutiveFailures++
                    lastError = e.message
                    if (consecutiveFailures >= 3) {
                        status = "Error"
                    }
                }
                isRefreshing = false
                onComplete?.invoke()
                Logger.d("MainScreen", "refreshStatus() completed")
            }
        }
    }
    
    // Initial load and polling
    LaunchedEffect(apiUrl, pollInterval) {
        Logger.i("MainScreen", "LaunchedEffect triggered. Starting polling...")
        try {
            // Initial check
            refreshStatus()
            
            // Status polling loop with smart retry
            while (true) {
                val delayMs = calculateRetryDelay()
                Logger.d("MainScreen", "Waiting ${delayMs}ms before next poll")
                delay(delayMs)
                refreshStatus()
            }
        } catch (e: Exception) {
            Logger.e("MainScreen", "Exception in polling loop", e)
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
                        try {
                            ApiClient.post("$apiUrl/api/start", "") { result ->
                                result.onSuccess {
                                    status = "Running"
                                    Toast.makeText(context, "OpenClaw started.", Toast.LENGTH_SHORT).show()
                                }.onFailure { e ->
                                    Toast.makeText(context, "API failed, trying Termux...", Toast.LENGTH_SHORT).show()
                                    try {
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
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error starting bot: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Start OpenClaw", fontSize = 20.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        try {
                            ApiClient.post("$apiUrl/api/stop", "") { result ->
                                result.onSuccess {
                                    status = "Stopped"
                                    Toast.makeText(context, "OpenClaw stopped.", Toast.LENGTH_SHORT).show()
                                }.onFailure { e ->
                                    Toast.makeText(context, "API failed, trying Termux...", Toast.LENGTH_SHORT).show()
                                    try {
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
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error stopping bot: ${e.message}", Toast.LENGTH_LONG).show()
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
                
                Text(text = "Terminal/Logs:", fontSize = 18.sp)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                      AndroidView(
                          factory = { ctx ->
                              Logger.i("MainScreen", "Creating WebView with file access")
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
                                          Logger.w("MainScreen", "WebView error: $errorCode - $description for $failingUrl")
                                          // Suppress WebView errors - ttyd may not be running yet
                                          // This prevents crashes when local server is unavailable
                                      }
                                  }
                                  
                                  // Enable file access for external storage
                                  settings.javaScriptEnabled = true
                                  settings.domStorageEnabled = true
                                  settings.allowFileAccess = true
                                  settings.allowContentAccess = true
                                  settings.allowFileAccessFromFileURLs = true
                                  settings.allowUniversalAccessFromFileURLs = true
                                  
                                  // Add JavaScript interface for file operations
                                  addJavascriptInterface(FileInterface(context), "AndroidFileInterface")
                                  
                                  // Enable debug for development
                                  if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                      setWebContentsDebuggingEnabled(true)
                                  }
                                  
                                  val url = "http://127.0.0.1:7681"
                                  Logger.i("MainScreen", "Loading WebView URL: $url with file access enabled")
                                  loadUrl(url)
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