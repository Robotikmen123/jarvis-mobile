package com.jarvis.mobile.tools

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

object DndTool : JarvisTool {
    override val name = "do_not_disturb"

    override fun declaration() = functionDecl(
        name,
        "Enable or disable Do Not Disturb (interruption filter)."
    ) {
        bool("enabled", "true to turn on, false to turn off", required = true)
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val target = (args["enabled"] as? Boolean) ?: return "enabled missing."
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            ctx.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return "Rahatsiz Etme icin once izin gerekli, ayarlar acildi."
        }
        nm.setInterruptionFilter(
            if (target) NotificationManager.INTERRUPTION_FILTER_PRIORITY
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
        return if (target) "Rahatsiz Etme acildi." else "Rahatsiz Etme kapatildi."
    }
}
