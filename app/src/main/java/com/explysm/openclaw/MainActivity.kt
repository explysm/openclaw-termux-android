package com.explysm.openclaw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.explysm.openclaw.data.SettingsRepository
import com.explysm.openclaw.screens.MainScreen
import com.explysm.openclaw.screens.OnboardingScreen
import com.explysm.openclaw.screens.OnboardingTerminalScreen
import com.explysm.openclaw.screens.SettingsScreen
import com.explysm.openclaw.screens.WelcomeScreen
import com.explysm.openclaw.ui.theme.OpenClawAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Keep splash screen visible until content is ready
        splashScreen.setKeepOnScreenCondition { false }
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val settingsRepository = remember { SettingsRepository(this) }
            val isDarkTheme by settingsRepository.isDarkTheme.collectAsState(initial = false)
            
            OpenClawAndroidTheme(
                darkTheme = isDarkTheme || isSystemInDarkTheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OpenClawApp(settingsRepository)
                }
            }
        }
    }
}

@Composable
fun OpenClawApp(settingsRepository: SettingsRepository) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(navController = navController)
        }
        composable("onboarding") {
            OnboardingScreen(navController = navController)
        }
        composable("onboarding_terminal") {
            OnboardingTerminalScreen(navController = navController)
        }
        composable("main") {
            MainScreen(navController = navController, settingsRepository = settingsRepository)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
    }
}
