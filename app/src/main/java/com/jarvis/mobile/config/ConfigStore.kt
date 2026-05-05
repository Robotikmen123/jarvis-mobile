package com.jarvis.mobile.config

import android.content.Context

object ConfigStore {
    private const val FILE = "jarvis_config"
    private const val K_GEMINI    = "gemini_api_key"
    private const val K_ANTHROPIC = "anthropic_api_key"
    private const val K_VOICE     = "voice_name"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun geminiKey(ctx: Context): String? =
        prefs(ctx).getString(K_GEMINI, null)?.takeIf { it.isNotBlank() }

    fun setGeminiKey(ctx: Context, key: String) {
        prefs(ctx).edit().putString(K_GEMINI, key.trim()).apply()
    }

    fun anthropicKey(ctx: Context): String? =
        prefs(ctx).getString(K_ANTHROPIC, null)?.takeIf { it.isNotBlank() }

    fun setAnthropicKey(ctx: Context, key: String?) {
        val v = key?.trim().orEmpty()
        prefs(ctx).edit().run {
            if (v.isEmpty()) remove(K_ANTHROPIC) else putString(K_ANTHROPIC, v)
            apply()
        }
    }

    fun voiceName(ctx: Context): String =
        prefs(ctx).getString(K_VOICE, "Puck") ?: "Puck"

    fun setVoiceName(ctx: Context, voice: String) {
        prefs(ctx).edit().putString(K_VOICE, voice).apply()
    }

    fun isConfigured(ctx: Context): Boolean = !geminiKey(ctx).isNullOrBlank()
}
