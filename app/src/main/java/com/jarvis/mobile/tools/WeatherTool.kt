package com.jarvis.mobile.tools

import android.content.Context
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder

object WeatherTool : JarvisTool {
    override val name = "weather_report"

    override fun declaration(): JsonObject = functionDecl(
        name,
        "Returns the current weather (temperature) for the given city."
    ) {
        str("city", "City name", required = true)
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val city = (args["city"] as? String)?.trim().orEmpty()
        if (city.isEmpty()) return "City missing."
        return withContext(Dispatchers.IO) {
            runCatching {
                val q = URLEncoder.encode(city, "UTF-8")
                val url = URL("https://wttr.in/$q?format=%t+%C&M&lang=tr")
                val conn = url.openConnection().apply {
                    setRequestProperty("User-Agent", "curl/8.0")
                    connectTimeout = 5000
                    readTimeout    = 5000
                }
                val raw = conn.getInputStream().bufferedReader().use { it.readText().trim() }
                "Hava ($city): $raw"
            }.getOrElse { "Hava bilgisi alinamadi: ${it.message}" }
        }
    }
}
