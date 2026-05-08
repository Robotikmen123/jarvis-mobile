package com.jarvis.mobile.tools

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

object AlarmTool : JarvisTool {
    override val name = "set_alarm"

    override fun declaration() = functionDecl(
        name,
        "Set an alarm at a specific time in the system clock app."
    ) {
        int("hour",   "Hour 0-23", required = true)
        int("minute", "Minute 0-59", required = true)
        str("label",  "Optional label")
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val h = (args["hour"]   as? Number)?.toInt()?.coerceIn(0, 23) ?: return "Saat eksik."
        val m = (args["minute"] as? Number)?.toInt()?.coerceIn(0, 59) ?: return "Dakika eksik."
        val lbl = (args["label"] as? String).orEmpty()
        val i = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR,    h)
            .putExtra(AlarmClock.EXTRA_MINUTES, m)
            .putExtra(AlarmClock.EXTRA_MESSAGE, lbl)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { ctx.startActivity(i); "Alarm: %02d:%02d".format(h, m) }
            .getOrElse { "Alarm kurulamadi: ${it.message}" }
    }
}
