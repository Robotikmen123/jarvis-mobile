package com.jarvis.mobile.tools

import android.content.Context
import android.media.AudioManager

object VolumeTool : JarvisTool {
    override val name = "volume_control"

    override fun declaration() = functionDecl(
        name,
        "Set, raise, lower, mute or read the device media volume."
    ) {
        str("action", "One of: set, get, mute, unmute, up, down", required = true)
        int("level", "Volume level 0-100 (only for action=set)")
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val stream = AudioManager.STREAM_MUSIC
        val max = am.getStreamMaxVolume(stream)
        val flag = AudioManager.FLAG_SHOW_UI
        return when ((args["action"] as? String)?.lowercase()) {
            "set" -> {
                val pct = (args["level"] as? Number)?.toInt() ?: return "Level missing."
                val v = pct.coerceIn(0, 100) * max / 100
                am.setStreamVolume(stream, v, flag)
                "Ses %$pct."
            }
            "mute"   -> { am.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE,   flag); "Ses kapatildi." }
            "unmute" -> { am.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, flag); "Ses acildi." }
            "up"     -> { am.adjustStreamVolume(stream, AudioManager.ADJUST_RAISE,  flag); "Ses arttirildi." }
            "down"   -> { am.adjustStreamVolume(stream, AudioManager.ADJUST_LOWER,  flag); "Ses azaltildi." }
            else     -> "Ses %${am.getStreamVolume(stream) * 100 / max}."
        }
    }
}
