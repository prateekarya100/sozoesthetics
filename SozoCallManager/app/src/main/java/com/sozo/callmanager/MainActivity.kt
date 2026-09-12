package com.sozo.callmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.sozo.callmanager.data.DemoSession
import com.sozo.callmanager.ui.LoginScreen
import com.sozo.callmanager.ui.MainScaffold
import com.sozo.callmanager.ui.SetupScreen
import com.sozo.callmanager.ui.theme.SozoTheme

private enum class Screen { LOGIN, SETUP, MAIN }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bring the session back if Android killed our process mid-call —
        // otherwise this would incorrectly fall back to the Login screen.
        DemoSession.restoreIfNeeded(this)

        val initialScreen = when {
            !DemoSession.isLoggedIn -> Screen.LOGIN
            !DemoSession.hasCompletedSetup(this) -> Screen.SETUP
            else -> Screen.MAIN
        }

        setContent {
            SozoTheme {
                var screen by remember { mutableStateOf(initialScreen) }

                when (screen) {
                    Screen.LOGIN -> LoginScreen(onLoginSuccess = { screen = Screen.SETUP })
                    Screen.SETUP -> SetupScreen(onSetupComplete = { screen = Screen.MAIN })
                    Screen.MAIN -> MainScaffold(onLogout = { screen = Screen.LOGIN })
                }
            }
        }
    }
}
