package com.towkirhs.handy.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HandyTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HandyRoot()
                }
            }
        }
    }
}

private enum class Screen { HOME, SETTINGS }

@Composable
private fun HandyRoot() {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var screen by remember { mutableStateOf(Screen.HOME) }

    // Re-check permissions every time the user comes back from a system settings page.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }

    val ready = remember(tick) {
        Permissions.mic(context) && Permissions.overlay(context) && Permissions.accessibility(context)
    }

    when {
        !ready -> SetupScreen(tick = tick, onChanged = { tick++ })
        screen == Screen.HOME -> HomeScreen(tick = tick, onOpenSettings = { screen = Screen.SETTINGS })
        else -> {
            BackHandler { screen = Screen.HOME }
            SettingsScreen(onBack = { screen = Screen.HOME })
        }
    }
}
