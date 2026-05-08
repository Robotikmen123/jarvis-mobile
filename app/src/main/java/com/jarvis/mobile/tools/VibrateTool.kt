package com.jarvis.mobile.tools

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrateTool : JarvisTool {
    override val name = "vibrate"

    override fun declaration() = functionDecl(
        name,
        "Vibrate the device for a given duration in milliseconds (default 300)."
    ) {
        int("ms", "Duration in milliseconds")
    }

    @Suppress("DEPRECATION")
    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val dur = (args["ms"] as? Number)?.toLong()?.coerceIn(20L, 5_000L) ?: 300L
        val vib: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(dur, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vib.vibrate(dur)
        }
        return "Titresim ${dur}ms."
    }
}
