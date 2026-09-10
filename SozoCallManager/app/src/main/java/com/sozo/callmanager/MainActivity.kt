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
        setContent {
            SozoTheme {
                var screen by remember {
                    mutableStateOf(if (DemoSession.isLoggedIn) Screen.SETUP else Screen.LOGIN)
                }

                when (screen) {
                    Screen.LOGIN -> LoginScreen(onLoginSuccess = { screen = Screen.SETUP })
                    Screen.SETUP -> SetupScreen(onSetupComplete = { screen = Screen.MAIN })
                    Screen.MAIN -> MainScaffold(onLogout = { screen = Screen.LOGIN })
                }
            }
        }
    }
}
