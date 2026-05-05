package com.jarvis.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ensureChannels()
        instance = this
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_VOICE) == null) {
            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_VOICE, "Jarvis Voice", NotificationManager.IMPORTANCE_LOW
            ))
        }
        if (nm.getNotificationChannel(CHANNEL_REMINDER) == null) {
            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_REMINDER, "Reminders", NotificationManager.IMPORTANCE_HIGH
            ))
        }
    }

    companion object {
        const val CHANNEL_VOICE    = "jarvis_voice"
        const val CHANNEL_REMINDER = "jarvis_reminder"
        lateinit var instance: App
            private set
    }
}
