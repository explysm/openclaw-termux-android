package com.explysm.openclaw.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.explysm.openclaw.utils.TermuxRunner

@Composable
fun WelcomeScreen(navController: NavController) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to OpenClaw Android 🦞 – Install & onboard your personal AI",
            fontSize = 24.sp,
            modifier = Modifier.padding(16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            if (TermuxRunner.isTermuxInstalled(context)) {
                navController.navigate("onboarding")
            } else {
                Toast.makeText(context, "Install Termux first", Toast.LENGTH_LONG).show()
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://f-droid.org/packages/com.termux/")
                }
                context.startActivity(intent)
            }
        }) {
            Text(text = "Install & Onboard")
        }
    }
}
