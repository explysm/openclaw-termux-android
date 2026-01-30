package com.explysm.openclaw.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingTerminalScreen(navController: NavController) {
    val webViewRef = remember { mutableListOf<WebView?>(null) }
    
    fun sendKey(keyCode: String) {
        webViewRef[0]?.evaluateJavascript(
            """
            (function() {
                const event = new KeyboardEvent('keydown', {
                    key: '$keyCode',
                    code: 'Arrow$keyCode',
                    keyCode: ${when(keyCode) {
                        "Up" -> "38"
                        "Down" -> "40"
                        "Left" -> "37"
                        "Right" -> "39"
                        else -> "0"
                    }},
                    bubbles: true
                });
                document.dispatchEvent(event);
            })();
            """, null
        )
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
                // Arrow keys row
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                
                Button(
                    onClick = {
                        navController.navigate("main") {
                            popUpTo("onboarding_terminal") { inclusive = true }
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
                        webViewRef[0] = this
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
                },
                update = {
                    webViewRef[0] = it
                    it.loadUrl("http://127.0.0.1:7681")
                }
            )
        }
    }
}
