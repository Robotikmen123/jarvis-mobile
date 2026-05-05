package com.jarvis.mobile.tools

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.JsonObject

object WebSearchTool : JarvisTool {
    override val name = "web_search"

    override fun declaration(): JsonObject = functionDecl(
        name,
        "Searches the web. Opens the device's default browser/search app with the query."
    ) {
        str("query", "Search query", required = true)
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val q = (args["query"] as? String)?.trim().orEmpty()
        if (q.isEmpty()) return "Query missing."

        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, q)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            ctx.startActivity(intent)
            "Searching for: $q"
        }.getOrElse {
            val fallback = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            ctx.startActivity(fallback)
            "Searching for: $q"
        }
    }
}
