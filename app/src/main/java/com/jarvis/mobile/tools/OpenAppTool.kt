package com.jarvis.mobile.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.JsonObject

object OpenAppTool : JarvisTool {
    override val name = "open_app"

    override fun declaration(): JsonObject = functionDecl(
        name,
        "Opens any app on the phone (WhatsApp, Spotify, YouTube, Chrome, etc.) " +
        "or a website URL. Always call this — never just say you opened it."
    ) {
        str("app_name", "App display name (e.g. WhatsApp) or URL", required = true)
    }

    private val ALIASES = mapOf(
        "whatsapp"   to "com.whatsapp",
        "telegram"   to "org.telegram.messenger",
        "instagram"  to "com.instagram.android",
        "spotify"    to "com.spotify.music",
        "youtube"    to "com.google.android.youtube",
        "chrome"     to "com.android.chrome",
        "firefox"    to "org.mozilla.firefox",
        "discord"    to "com.discord",
        "twitter"    to "com.twitter.android",
        "x"          to "com.twitter.android",
        "facebook"   to "com.facebook.katana",
        "tiktok"     to "com.zhiliaoapp.musically",
        "gmail"      to "com.google.android.gm",
        "maps"       to "com.google.android.apps.maps",
        "netflix"    to "com.netflix.mediaclient",
        "twitch"     to "tv.twitch.android.app",
        "snapchat"   to "com.snapchat.android",
        "linkedin"   to "com.linkedin.android",
        "github"     to "com.github.android",
        "settings"   to "com.android.settings",
        "camera"     to "com.android.camera",
        "calculator" to "com.android.calculator2",
        "clock"      to "com.android.deskclock",
        "calendar"   to "com.google.android.calendar",
    )

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val raw = (args["app_name"] as? String)?.trim().orEmpty()
        if (raw.isEmpty()) return "App name missing."

        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return openUrl(ctx, raw)
        }

        val key = raw.lowercase().replace(" ", "")
        val pkg = ALIASES[key] ?: findByLabel(ctx, raw)
        if (pkg != null) {
            val launch = ctx.packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(launch)
                return "Opened $raw."
            }
        }
        return openUrl(ctx, "https://www.google.com/search?q=" + Uri.encode(raw))
    }

    private fun openUrl(ctx: Context, url: String): String {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { ctx.startActivity(i); "Opened $url." }
            .getOrElse { "Failed to open $url: ${it.message}" }
    }

    private fun findByLabel(ctx: Context, name: String): String? {
        val pm = ctx.packageManager
        val target = name.lowercase()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0)
        return apps.firstOrNull { ri ->
            ri.loadLabel(pm).toString().lowercase().contains(target)
        }?.activityInfo?.packageName
    }
}
