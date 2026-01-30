package com.explysm.openclaw.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.explysm.openclaw.utils.TermuxRunner

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // Run initial setup script
        TermuxRunner.runCommand(
            context,
            "export ANDROID_APP=1 && curl -s https://explysm.github.io/moltbot-termux/install.sh | sh",
            "OpenClaw Setup",
            background = true
        )
        // Chain with ttyd for onboarding
        TermuxRunner.runCommand(
            context,
            "pkg install ttyd -y && ttyd -p 7681 --interface 127.0.0.1 --writable --once bash -c \"moltbot onboard\"",
            "OpenClaw Onboarding",
            background = false // Keep this in foreground for user to see
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        // TODO: Potentially monitor URL changes or add a timer to detect onboarding completion
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

        FloatingActionButton(
            onClick = {
                navController.navigate("main") {
                    popUpTo("onboarding") { inclusive = true }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Done, "Done")
        }
    }
}
