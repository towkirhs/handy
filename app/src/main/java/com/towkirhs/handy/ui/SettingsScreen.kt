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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.towkirhs.handy.data.ModelDownloader
import com.towkirhs.handy.data.Prefs
import com.towkirhs.handy.overlay.OverlayService

private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/openai"
private const val GEMINI_MODEL = "gemini-flash-latest"

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val download by ModelDownloader.state.collectAsState()
    LaunchedEffect(Unit) { ModelDownloader.refresh(context) }
    var jarvisOn by remember { mutableStateOf(Prefs.jarvisButton) }
    var sendAll by remember { mutableStateOf(Prefs.sendAllToJarvis) }
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
            Text("Jarvis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Jarvis bubble: an always-on J bubble. What you say into it goes only to Jarvis.",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(checked = jarvisOn, onCheckedChange = {
                    jarvisOn = it
                    Prefs.jarvisButton = it
                    OverlayService.restart(context)
                })
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Also send everything I dictate elsewhere (messages, notes) to Jarvis.",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(checked = sendAll, onCheckedChange = {
                    sendAll = it
                    Prefs.sendAllToJarvis = it
                })
            }
        }

        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Fix grammar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Rewrites what you say into properly written text, like Wispr Flow. With an online " +
                            "key below it uses that service. Without one, or when offline, it uses the " +
                            "on-device model. Only the transcribed text is ever sent, never audio.",
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
                Text(
                    "Online service (optional). Pick one, then paste your API key.",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = {
                        baseUrl = GEMINI_URL
                        model = GEMINI_MODEL
                        Prefs.llmBaseUrl = baseUrl
                        Prefs.llmModel = model
                    }) { Text("Gemini") }
                    OutlinedButton(onClick = {
                        baseUrl = Prefs.DEFAULT_BASE_URL
                        model = Prefs.DEFAULT_MODEL
                        Prefs.llmBaseUrl = baseUrl
                        Prefs.llmModel = model
                    }) { Text("OpenAI") }
                }
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
            Text(
                "On-device grammar (offline)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (ModelDownloader.supported()) {
                    "Works with no internet. One-time download, ${ModelDownloader.SIZE_LABEL}. It runs on " +
                        "your phone's processor, so it is slower than an online service."
                } else {
                    "Not available on this phone: it needs a 64-bit ARM processor."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (ModelDownloader.supported()) {
                Row(
                    Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (val d = download) {
                        ModelDownloader.State.Missing ->
                            Button(onClick = { ModelDownloader.start(context) }) { Text("Download") }
                        is ModelDownloader.State.Downloading ->
                            Text("Downloading ${d.percent}%", color = MaterialTheme.colorScheme.primary)
                        ModelDownloader.State.Ready -> {
                            Text("Ready", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { ModelDownloader.delete(context) }) { Text("Delete") }
                        }
                        is ModelDownloader.State.Failed -> Column {
                            Text(d.message, color = MaterialTheme.colorScheme.error)
                            Button(onClick = { ModelDownloader.start(context) }) { Text("Try again") }
                        }
                    }
                }
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
