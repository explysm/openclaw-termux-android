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
    
    // Collect settings
    val apiUrl by settingsRepository.apiUrl.collectAsState(initial = SettingsRepository.DEFAULT_API_URL)
    val pollInterval by settingsRepository.pollInterval.collectAsState(initial = SettingsRepository.DEFAULT_POLL_INTERVAL)
    
    var status by remember { mutableStateOf("Unknown") }
    var isConnected by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }
    
    // Smart retry logic with exponential backoff
    var consecutiveFailures by remember { mutableStateOf(0) }
    val maxRetryDelay = 60_000L 
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
            val url = "$apiUrl/api/status"
            ApiClient.get(url) { result ->
                result.onSuccess { response ->
                    status = if (response.contains("running", ignoreCase = true)) "Running" else "Stopped"
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
    
    // Initial load and polling
    LaunchedEffect(apiUrl, pollInterval) {
        refreshStatus()
        while (true) {
            val delayMs = calculateRetryDelay()
            delay(delayMs)
            refreshStatus()
        }
    }
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { refreshStatus() }
    )
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isConnected && status == "Running" -> Color(0xFF4CAF50)
                                isConnected -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(12.dp)
                        ) {}
                        Text(
                            "OpenClaw: $status",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
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
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Connection status chip
                if (!isConnected || lastError != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
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
                                    if (isConnected) "Connection unstable" else "API unreachable",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!isConnected) {
                                    Text(
                                        "Is 'moltbot --android-app' running in Termux?",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            if (!isConnected) {
                                TextButton(
                                    onClick = { TermuxRunner.openTermuxApp(context) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Open Termux")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
                
                // Primary Control Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Core Controls",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    ApiClient.post("$apiUrl/api/start", "") { result ->
                                        result.onSuccess { status = "Running" }
                                              .onFailure { /* Fallback handled */ }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Text("Start", fontWeight = FontWeight.Bold)
                            }
                            
                            FilledTonalButton(
                                onClick = {
                                    ApiClient.post("$apiUrl/api/stop", "") { result ->
                                        result.onSuccess { status = "Stopped" }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = MaterialTheme.shapes.large,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Stop", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Terminal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Terminal View",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { refreshStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Terminal", size(20.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Terminal Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    shape = MaterialTheme.shapes.medium,
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webViewClient = object : WebViewClient() {
                                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {}
                                }
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                addJavascriptInterface(FileInterface(context), "AndroidFileInterface")
                                loadUrl("http://127.0.0.1:7681")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}