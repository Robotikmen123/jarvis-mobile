package com.jarvis.mobile.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhoneStatusTool : JarvisTool {
    override val name = "phone_status"

    override fun declaration() = functionDecl(
        name,
        "One-shot phone status: battery %, charging, current time, current date."
    )

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charging = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let {
            val s = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            s == BatteryManager.BATTERY_STATUS_CHARGING || s == BatteryManager.BATTERY_STATUS_FULL
        } ?: false
        val now = Date()
        val tr = Locale("tr")
        val time = SimpleDateFormat("HH:mm", tr).format(now)
        val date = SimpleDateFormat("d MMMM EEEE", tr).format(now)
        return "Pil %$pct${if (charging) " (sarjda)" else ""}, saat $time, $date."
    }
}
