package com.jarvis.mobile.tools

import android.content.Context
import android.content.Intent
import android.provider.Settings

object SettingsTool : JarvisTool {
    override val name = "open_settings"

    override fun declaration() = functionDecl(
        name,
        "Open a system settings page (wifi, bluetooth, sound, display, " +
        "battery, apps, location, date, accessibility, root, etc.)."
    ) {
        str("page", "wifi|bluetooth|sound|display|battery|apps|location|date|accessibility|root", required = true)
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val page = (args["page"] as? String)?.lowercase()?.trim().orEmpty()
        val action = when (page) {
            "wifi"          -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth"     -> Settings.ACTION_BLUETOOTH_SETTINGS
            "sound"         -> Settings.ACTION_SOUND_SETTINGS
            "display"       -> Settings.ACTION_DISPLAY_SETTINGS
            "battery"       -> Intent.ACTION_POWER_USAGE_SUMMARY
            "apps"          -> Settings.ACTION_APPLICATION_SETTINGS
            "location"      -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "date"          -> Settings.ACTION_DATE_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "root", ""      -> Settings.ACTION_SETTINGS
            else            -> Settings.ACTION_SETTINGS
        }
        val i = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { ctx.startActivity(i); "Ayarlar acildi: $page" }
            .getOrElse { "Ayarlar acilamadi: ${it.message}" }
    }
}
