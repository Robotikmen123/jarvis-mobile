package com.jarvis.mobile.tools

import android.content.Context
import com.google.gson.JsonObject
import com.jarvis.mobile.memory.MemoryStore

object SaveMemoryTool : JarvisTool {
    override val name = "save_memory"

    override fun declaration(): JsonObject = functionDecl(
        name,
        "Save an important fact about the user (name, city, preferences, etc.). " +
        "Call silently when user reveals personal info. Values must be in English."
    ) {
        str("category", "identity | preferences | projects | relationships | wishes | notes",
            required = true)
        str("key",   "snake_case key", required = true)
        str("value", "concise value", required = true)
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val cat   = (args["category"] as? String)?.trim().orEmpty()
        val key   = (args["key"]      as? String)?.trim().orEmpty()
        val value = (args["value"]    as? String)?.trim().orEmpty()
        if (cat.isEmpty() || key.isEmpty() || value.isEmpty()) {
            return "Missing category, key or value."
        }
        MemoryStore.set(ctx, cat, key, value)
        return "ok"
    }
}
