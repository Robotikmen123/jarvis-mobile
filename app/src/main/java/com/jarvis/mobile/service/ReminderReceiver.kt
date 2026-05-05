package com.jarvis.mobile.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jarvis.mobile.App
import com.jarvis.mobile.R
import com.jarvis.mobile.ui.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val message = intent.getStringExtra("message") ?: "Hatirlatma"
        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, App.CHANNEL_REMINDER)
            .setContentTitle("Jarvis: Hatirlatma")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notif)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.notify(message.hashCode(), n)
    }
}
