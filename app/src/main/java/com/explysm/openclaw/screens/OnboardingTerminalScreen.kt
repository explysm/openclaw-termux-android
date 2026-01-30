package com.explysm.openclaw.screens

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.explysm.openclaw.data.SettingsRepository
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingTerminalScreen(navController: NavController, settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var showLoadingDialog by remember { mutableStateOf(true) }
    
    fun sendKey(key: String, isArrow: Boolean = true) {
        webViewRef.value?.let { webView ->
            // Focus the WebView first
            webView.requestFocus()
            
            // Try multiple approaches to send the key
            webView.evaluateJavascript(
                """
                (function() {
                    // Try to find the terminal input element
                    const terminal = document.querySelector('textarea') || 
                                   document.querySelector('input') || 
                                   document.querySelector('.terminal') ||
                                   document.querySelector('[contenteditable]') ||
                                   document.body;
                    
                    // Create and dispatch keyboard events
                    const keyCode = ${when(key) {
                        "Up" -> "38"
                        "Down" -> "40"
                        "Left" -> "37"
                        "Right" -> "39"
                        "Enter" -> "13"
                        else -> "0"
                    }};
                    const code = '${if (isArrow) "Arrow$key" else key}';
                    const keyName = '$key';
                    
                    // Dispatch keydown
                    const keydownEvent = new KeyboardEvent('keydown', {
                        key: keyName,
                        code: code,
                        keyCode: keyCode,
                        which: keyCode,
                        bubbles: true,
                        cancelable: true
                    });
                    terminal.dispatchEvent(keydownEvent);
                    
                    // For Enter, also try to submit/input
                    if (keyName === 'Enter') {
                        const keyupEvent = new KeyboardEvent('keyup', {
                            key: keyName,
                            code: code,
                            keyCode: keyCode,
                            which: keyCode,
                            bubbles: true,
                            cancelable: true
                        });
                        terminal.dispatchEvent(keyupEvent);
                        
                        // Try to trigger input event as well
                        const inputEvent = new InputEvent('input', {
                            inputType: 'insertLineBreak',
                            bubbles: true,
                            cancelable: true
                        });
                        terminal.dispatchEvent(inputEvent);
                    }
                    
                    return 'Key sent: ' + keyName;
                })();
                """, null
            )
        }
    }
    
    fun openKeyboard(context: Context) {
        webViewRef.value?.let { webView ->
            webView.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Onboarding") }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // First row: Keyboard and Enter buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Keyboard button to open system keyboard
                    Button(onClick = { openKeyboard(context) }) {
                        Text("Keyboard")
                    }
                    
                    // Enter key button
                    Button(onClick = { sendKey("Enter", isArrow = false) }) {
                        Text("Enter")
                    }
                }
                
                // Second row: Arrow keys
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { sendKey("Left") }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left Arrow")
                    }
                    Column {
                        IconButton(onClick = { sendKey("Up") }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up Arrow")
                        }
                        IconButton(onClick = { sendKey("Down") }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down Arrow")
                        }
                    }
                    IconButton(onClick = { sendKey("Right") }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right Arrow")
                    }
                }
                
                // Continue button
                Button(
                    onClick = {
                        scope.launch {
                            // Save onboarding completed FIRST to prevent race condition
                            settingsRepository.setOnboardingCompleted(true)
                            // Then navigate - clear entire back stack since start destination might not exist
                            navController.navigate("main") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    Text("Continue to Dashboard")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = {
                    WebView(it).apply {
                        webViewRef.value = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.setSupportZoom(false)
                        loadUrl("http://127.0.0.1:7681")
                    }
                },
                update = {
                    webViewRef.value = it
                }
            )
            
            // Loading dialog
            if (showLoadingDialog) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Onboard loading may take a while . . .",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(onClick = { showLoadingDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
            }
        }
    }
}
