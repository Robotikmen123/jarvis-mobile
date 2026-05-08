package com.jarvis.mobile.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

object BatteryTool : JarvisTool {
    override val name = "battery_status"

    override fun declaration() = functionDecl(
        name,
        "Get the current battery percentage and charging state."
    )

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charging = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let {
            val s = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            s == BatteryManager.BATTERY_STATUS_CHARGING || s == BatteryManager.BATTERY_STATUS_FULL
        } ?: false
        return "Pil %$pct" + (if (charging) " (sarjda)" else "")
    }
}
