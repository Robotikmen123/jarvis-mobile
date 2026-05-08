package com.jarvis.mobile.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jarvis.mobile.config.ConfigStore

/**
 * Receives BOOT_COMPLETED (and equivalents) and starts JarvisService so the
 * voice assistant comes back online automatically after a reboot, without
 * the user having to open the app first.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val ok = action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
                action == "android.intent.action.QUICKBOOT_POWERON" ||
                action == "com.htc.intent.action.QUICKBOOT_POWERON"
        if (!ok) return
        if (!ConfigStore.isConfigured(ctx)) return
        val svc = Intent(ctx, JarvisService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(svc)
        else ctx.startService(svc)
    }
}
