package com.jarvis.mobile.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object BrightnessTool : JarvisTool {
    override val name = "brightness"

    override fun declaration() = functionDecl(
        name,
        "Set screen brightness from 0 (darkest) to 100 (brightest)."
    ) {
        int("level", "Brightness 0-100", required = true)
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val pct = (args["level"] as? Number)?.toInt()?.coerceIn(0, 100) ?: return "Seviye eksik."
        if (!Settings.System.canWrite(ctx)) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return "Parlaklik icin once 'Sistem ayarlarini degistir' izni gerekli, ayarlar acildi."
        }
        Settings.System.putInt(
            ctx.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Settings.System.putInt(
            ctx.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            (pct * 255 / 100).coerceAtLeast(1)
        )
        return "Parlaklik %$pct."
    }
}
