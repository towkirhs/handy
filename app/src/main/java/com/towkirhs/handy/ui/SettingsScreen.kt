package com.towkirhs.handy.ui

import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.towkirhs.handy.data.Prefs

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var jarvisOn by remember { mutableStateOf(Prefs.sendToJarvis) }
    var llmOn by remember { mutableStateOf(Prefs.llmEnabled) }
    var baseUrl by remember { mutableStateOf(Prefs.llmBaseUrl) }
    var apiKey by remember { mutableStateOf(Prefs.llmApiKey) }
    var model by remember { mutableStateOf(Prefs.llmModel) }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Smart cleanup with AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Optional. Sends only the transcribed text, never audio, to an OpenAI-compatible " +
                            "service you choose. Off by default. Everything else works offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = llmOn, onCheckedChange = {
                    llmOn = it
                    Prefs.llmEnabled = it
                })
            }
            if (llmOn) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it; Prefs.llmBaseUrl = it },
                    label = { Text("API base URL") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; Prefs.llmApiKey = it },
                    label = { Text("API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it; Prefs.llmModel = it },
                    label = { Text("Model") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }

        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Send to Jarvis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Also hands every finished dictation to the Jarvis app on this phone. " +
                            "Stays on the device. Open Jarvis once after installing it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = jarvisOn, onCheckedChange = {
                    jarvisOn = it
                    Prefs.sendToJarvis = it
                })
            }
        }

        SoftCard {
            Text("Voice keyboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Handy also works as a keyboard for dictating where the floating bar can't appear. " +
                    "Enable it, then switch to it from any text field.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                }) { Text("Enable") }
                OutlinedButton(onClick = {
                    context.getSystemService(InputMethodManager::class.java).showInputMethodPicker()
                }) { Text("Switch") }
            }
        }

        SoftCard {
            Text("Speech model", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Whisper base (English), bundled in the app and run on your phone. " +
                    "No internet is needed to dictate.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
