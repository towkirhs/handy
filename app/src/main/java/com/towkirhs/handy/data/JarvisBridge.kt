package com.towkirhs.handy.data

import android.content.Context
import android.content.Intent

/** Hands each finished dictation to the Jarvis app, if it is installed. */
object JarvisBridge {
    private const val PACKAGE = "com.towkirhs.jarcis"
    private const val ACTION = "com.towkirhs.jarcis.NEW_NOTE"

    fun send(context: Context, text: String) {
        if (!Prefs.sendToJarvis) return
        val intent = Intent(ACTION).setPackage(PACKAGE).putExtra("text", text)
        context.sendBroadcast(intent)
    }
}
