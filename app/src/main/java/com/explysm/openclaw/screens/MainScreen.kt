package com.explysm.openclaw.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.explysm.openclaw.utils.TermuxRunner
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Stopped") }
    var chatLogs by remember { mutableStateOf("Welcome to OpenClaw chat.") }

    // Placeholder for API calls - will be replaced by actual ApiClient later
    val startOpenClaw: () -> Unit = {
        // Fallback to Termux command if API is not available/implemented yet
        TermuxRunner.runCommand(
            context,
            "moltbot gateway --port 18789 --verbose &", // Run in background
            "OpenClaw Gateway",
            background = true
        )
        status = "Running"
        Toast.makeText(context, "Starting OpenClaw gateway...", Toast.LENGTH_SHORT).show()
    }

    val stopOpenClaw: () -> Unit = {
        TermuxRunner.runCommand(
            context,
            "pkill -f \"moltbot gateway\"",
            "Stop OpenClaw Gateway",
            background = true
        )
        status = "Stopped"
        Toast.makeText(context, "Stopping OpenClaw gateway...", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        // Poll status every 10 seconds
        while (true) {
            // TODO: Implement actual API call to /api/status
            delay(10000)
        }
    }

    LaunchedEffect(Unit) {
        // Poll chat logs every 5 seconds
        while (true) {
            // TODO: Implement actual API call to /api/chat
            delay(5000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("OpenClaw: $status") })
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = startOpenClaw, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Start OpenClaw", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = stopOpenClaw, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Stop OpenClaw", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Chat/Logs:", fontSize = 18.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = chatLogs)
            }

            // Persistent bottom sheet or side panel for terminal/logs/commands
            // For simplicity, using a small WebView at the bottom for now
            Text(text = "Terminal/Logs (ttyd):", fontSize = 18.sp)
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                factory = {
                    WebView(it).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        loadUrl("http://127.0.0.1:7681")
                    }
                }, update = {
                    it.loadUrl("http://127.0.0.1:7681")
                })
        }
    }
}
