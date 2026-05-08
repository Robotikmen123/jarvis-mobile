package com.jarvis.mobile.tools

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import java.time.LocalDateTime
import java.time.ZoneId

object CalendarTool : JarvisTool {
    override val name = "calendar_add"

    override fun declaration() = functionDecl(
        name,
        "Open the system calendar with a new event prefilled. " +
        "User confirms in the calendar app."
    ) {
        str("title",       "Event title", required = true)
        str("start_iso",   "ISO local datetime e.g. 2026-05-08T18:00", required = true)
        int("duration_min","Duration in minutes (default 60)")
        str("location",    "Location (optional)")
        str("description", "Description (optional)")
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val title = (args["title"]       as? String).orEmpty()
        val iso   = (args["start_iso"]   as? String).orEmpty()
        val dur   = (args["duration_min"] as? Number)?.toLong() ?: 60L
        val loc   = (args["location"]    as? String).orEmpty()
        val desc  = (args["description"] as? String).orEmpty()
        val startMs = runCatching {
            LocalDateTime.parse(iso).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrElse { return "Tarih formati bozuk: $iso" }
        val endMs = startMs + dur * 60_000L

        val i = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMs)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME,   endMs)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, loc)
            .putExtra(CalendarContract.Events.DESCRIPTION,    desc)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { ctx.startActivity(i); "Takvime ekleme onayi acildi." }
            .getOrElse { "Takvim acilamadi: ${it.message}" }
    }
}
