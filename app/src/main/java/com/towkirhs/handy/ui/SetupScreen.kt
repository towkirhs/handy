package com.towkirhs.handy.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** First-run checklist. [tick] changes whenever the user returns from a settings page so the checks re-run. */
@Composable
fun SetupScreen(tick: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onChanged() }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Welcome to Handy", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "Talk instead of type, in any app. Handy turns your speech into clean text entirely on your phone.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        StepCard(
            title = "1. Microphone",
            body = "Handy listens only while you are dictating. Audio never leaves your phone.",
            done = Permissions.mic(context),
            action = "Allow"
        ) { permission.launch(Manifest.permission.RECORD_AUDIO) }

        StepCard(
            title = "2. Show over other apps",
            body = "Lets the floating bar appear on top of whatever you are typing in.",
            done = Permissions.overlay(context),
            action = "Open"
        ) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            )
        }

        StepCard(
            title = "3. Accessibility",
            body = "Lets Handy notice when you tap a text field and insert your words there. " +
                "If the switch is greyed out: App info, then the three-dot menu, then Allow restricted settings.",
            done = Permissions.accessibility(context),
            action = "Open"
        ) { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }

        if (Build.VERSION.SDK_INT >= 33) {
            StepCard(
                title = "4. Notification (optional)",
                body = "Shows a small notification while the bar is on, with a quick way to turn it off.",
                done = Permissions.notifications(context),
                action = "Allow"
            ) { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
    }
}
