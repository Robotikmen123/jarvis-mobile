package com.jarvis.mobile.live

import android.content.Context
import com.jarvis.mobile.memory.MemoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Welcome {

    suspend fun build(ctx: Context): String = withContext(Dispatchers.IO) {
        val city    = MemoryStore.getValue(ctx, "identity",    "city")               ?: "Istanbul"
        val address = MemoryStore.getValue(ctx, "preferences", "address_preference") ?: "efendim"
        val bedtime = MemoryStore.getValue(ctx, "preferences", "bedtime")            ?: "22:30"

        val now    = Date()
        val clock  = SimpleDateFormat("HH.mm", Locale.US).format(now).removePrefix("0")
        val temp   = fetchTemperatureC(city)
        val until  = formatUntilBedtime(now, bedtime)

        val tempPart = if (temp != null) "hava $temp derece" else "hava bilgisi alinamadi"
        "Hosgeldiniz $address. Saat $clock, $tempPart, yatma saatine $until."
    }

    private fun fetchTemperatureC(city: String): Int? = runCatching {
        val q = java.net.URLEncoder.encode(city, "UTF-8")
        val url = URL("https://wttr.in/$q?format=%t&M")
        val conn = url.openConnection().apply {
            setRequestProperty("User-Agent", "curl/8.0")
            connectTimeout = 4000
            readTimeout    = 4000
        }
        val raw = conn.getInputStream().bufferedReader().use { it.readText().trim() }
        raw.filter { it.isDigit() || it == '-' }.toIntOrNull()
    }.getOrNull()

    private fun formatUntilBedtime(now: Date, bedtime: String): String {
        val (h, m) = parseBedtime(bedtime)
        val cal = Calendar.getInstance().apply {
            time = now
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var tomorrow = false
        if (cal.time <= now) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
            tomorrow = true
        }
        val diffMin = ((cal.timeInMillis - now.time) / 60_000L).toInt()
        val hours   = diffMin / 60
        val minutes = diffMin % 60

        val parts = mutableListOf<String>()
        if (hours > 0)   parts += "$hours saat"
        if (minutes > 0) parts += "$minutes dakika"
        val span = if (parts.isEmpty()) "az bir sure" else parts.joinToString(" ")
        return if (tomorrow) "(yarin %02d.%02d) %s kaldi".format(h, m, span)
               else          "$span kaldi"
    }

    private fun parseBedtime(raw: String): Pair<Int, Int> {
        val cleaned = raw.trim().replace(".", ":")
        return runCatching {
            val (h, m) = cleaned.split(":", limit = 2)
            (h.toInt() % 24) to (m.toInt() % 60)
        }.getOrDefault(22 to 30)
    }
}
