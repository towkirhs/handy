package com.towkirhs.handy.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.towkirhs.handy.access.HandyAccessibilityService

object Permissions {
    fun mic(c: Context) =
        ContextCompat.checkSelfPermission(c, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun overlay(c: Context) = Settings.canDrawOverlays(c)

    fun accessibility(c: Context): Boolean {
        val me = ComponentName(c, HandyAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun notifications(c: Context) =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun keyboardEnabled(c: Context) =
        c.getSystemService(InputMethodManager::class.java).enabledInputMethodList.any { it.packageName == c.packageName }
}
