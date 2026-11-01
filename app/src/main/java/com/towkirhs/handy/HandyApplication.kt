package com.towkirhs.handy

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.towkirhs.handy.data.History
import com.towkirhs.handy.data.Prefs

class HandyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        History.init(this)
        val channel = NotificationChannel(CHANNEL_BAR, "Floating bar", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_BAR = "handy_bar"
    }
}
