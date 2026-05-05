package com.jarvis.mobile.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.JsonObject

object YouTubeTool : JarvisTool {
    override val name = "youtube_video"

    override fun declaration(): JsonObject = functionDecl(
        name,
        "Plays a YouTube video by search query, or opens a specific URL."
    ) {
        str("action", "play (default) or open")
        str("query",  "Search query for play")
        str("url",    "Video URL for open")
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val action = (args["action"] as? String)?.lowercase() ?: "play"
        val target = when (action) {
            "open" -> (args["url"] as? String)?.trim().orEmpty()
            else -> {
                val q = (args["query"] as? String)?.trim().orEmpty()
                if (q.isEmpty()) return "Query missing."
                "https://www.youtube.com/results?search_query=" + Uri.encode(q)
            }
        }
        if (target.isEmpty()) return "Target missing."

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return runCatching {
            ctx.startActivity(intent)
            "YouTube: $target"
        }.getOrElse { "Could not open YouTube: ${it.message}" }
    }
}
