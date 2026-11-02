package com.towkirhs.handy.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** Hands dictated text to the Jarvis app on the same phone. */
object JarvisBridge {
    private const val PACKAGE = "com.towkirhs.jarcis"
    private const val ACTION = "com.towkirhs.jarcis.NEW_NOTE"

    fun installed(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** Returns false if Jarvis isn't installed. */
    fun send(context: Context, text: String): Boolean {
        if (!installed(context)) return false
        context.sendBroadcast(Intent(ACTION).setPackage(PACKAGE).putExtra("text", text))
        return true
    }
}
