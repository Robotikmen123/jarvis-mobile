package com.jarvis.mobile.tools

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

object TimerTool : JarvisTool {
    override val name = "set_timer"

    override fun declaration() = functionDecl(
        name,
        "Start a countdown timer in the system clock app."
    ) {
        int("seconds", "Total seconds for the timer", required = true)
        str("label",   "Optional label")
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val s = (args["seconds"] as? Number)?.toInt() ?: return "Sure eksik."
        val lbl = (args["label"] as? String).orEmpty()
        val i = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH,    s)
            .putExtra(AlarmClock.EXTRA_MESSAGE,   lbl)
            .putExtra(AlarmClock.EXTRA_SKIP_UI,   true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { ctx.startActivity(i); "Sayac kuruldu (${s}s)." }
            .getOrElse { "Sayac kurulamadi: ${it.message}" }
    }
}
