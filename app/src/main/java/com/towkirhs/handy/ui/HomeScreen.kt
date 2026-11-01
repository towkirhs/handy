package com.towkirhs.handy.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.towkirhs.handy.data.History
import com.towkirhs.handy.data.Prefs
import com.towkirhs.handy.overlay.OverlayService

@Composable
fun HomeScreen(tick: Int, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val entries by History.entries.collectAsState()
    var barOn by remember(tick) { mutableStateOf(OverlayService.running || Prefs.barEnabled) }
    var tryText by remember { mutableStateOf("") }

    // The app is visible here, so Android allows (re)starting the bar if it was on before a reboot or kill.
    LaunchedEffect(Unit) {
        if (Prefs.barEnabled && !OverlayService.running) OverlayService.start(context)
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Handy",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onOpenSettings) { Text("Settings") }
            }
        }
        item {
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Floating bar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (barOn) "On. It appears when you tap a text field." else "Off",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = barOn,
                        onCheckedChange = { want ->
                            if (want) {
                                barOn = OverlayService.start(context)
                            } else {
                                OverlayService.stop(context)
                                barOn = false
                            }
                        }
                    )
                }
            }
        }
        item {
            SoftCard {
                Text("Try it", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Tap the box, then tap the bubble and speak. Hold the bubble to talk and release to finish.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = tryText,
                    onValueChange = { tryText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    minLines = 3
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Recent",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (entries.isNotEmpty()) TextButton(onClick = { History.clear() }) { Text("Clear") }
            }
        }
        if (entries.isEmpty()) {
            item {
                Text(
                    "Your dictations will show up here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(entries) { entry ->
                SoftCard {
                    Text(entry.text, style = MaterialTheme.typography.bodyLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            DateUtils.getRelativeTimeSpanString(entry.time).toString(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { clipboard.setText(AnnotatedString(entry.text)) }) { Text("Copy") }
                    }
                }
            }
        }
    }
}
