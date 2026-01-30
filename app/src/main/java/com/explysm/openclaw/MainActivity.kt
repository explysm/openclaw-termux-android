package com.explysm.openclaw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.explysm.openclaw.screens.MainScreen
import com.explysm.openclaw.screens.OnboardingScreen
import com.explysm.openclaw.screens.WelcomeScreen
import com.explysm.openclaw.ui.theme.OpenClawAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenClawAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OpenClawApp()
                }
            }
        }
    }
}

@Composable
fun OpenClawApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(navController = navController)
        }
        composable("onboarding") {
            OnboardingScreen(navController = navController)
        }
        composable("main") {
            MainScreen(navController = navController)
        }
    }
}
