package com.jarvis.mobile.tools

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.gson.JsonObject
import com.jarvis.mobile.service.ReminderReceiver
import java.text.SimpleDateFormat
import java.util.Locale

object ReminderTool : JarvisTool {
    override val name = "reminder"

    override fun declaration(): JsonObject = functionDecl(
        name,
        "Sets a timed reminder using Android AlarmManager. Triggers a notification."
    ) {
        str("date",    "YYYY-MM-DD",  required = true)
        str("time",    "HH:MM (24h)", required = true)
        str("message", "Reminder text", required = true)
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val date    = (args["date"]    as? String).orEmpty()
        val time    = (args["time"]    as? String).orEmpty()
        val message = (args["message"] as? String).orEmpty()
        if (date.isEmpty() || time.isEmpty() || message.isEmpty()) {
            return "Date, time, or message missing."
        }

        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val targetMs = runCatching { fmt.parse("$date $time")?.time }.getOrNull()
            ?: return "Could not parse date/time."

        val intent = Intent(ctx, ReminderReceiver::class.java).apply {
            putExtra("message", message)
        }
        val reqCode = (targetMs / 1000).toInt()
        val pi = PendingIntent.getBroadcast(
            ctx, reqCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val am = ctx.getSystemService(AlarmManager::class.java)
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.set(AlarmManager.RTC_WAKEUP, targetMs, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMs, pi)
            }
            "Reminder set for $date $time."
        }.getOrElse { "Reminder failed: ${it.message}" }
    }
}
