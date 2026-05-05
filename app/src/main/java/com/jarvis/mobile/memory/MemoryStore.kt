package com.jarvis.mobile.memory

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MemoryStore {
    private const val FILENAME = "long_term.json"
    private val gson = Gson()
    private val today: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val emptyDoc: JsonObject
        get() = JsonObject().apply {
            for (k in listOf("identity", "preferences", "projects", "relationships", "wishes", "notes")) {
                add(k, JsonObject())
            }
        }

    private fun file(ctx: Context) = File(ctx.filesDir, FILENAME)

    @Synchronized
    fun load(ctx: Context): JsonObject {
        val f = file(ctx)
        if (!f.exists()) return emptyDoc
        return runCatching {
            val parsed = JsonParser.parseString(f.readText(Charsets.UTF_8))
            if (parsed.isJsonObject) parsed.asJsonObject else emptyDoc
        }.getOrElse { emptyDoc }
    }

    @Synchronized
    fun save(ctx: Context, doc: JsonObject) {
        file(ctx).writeText(gson.toJson(doc), Charsets.UTF_8)
    }

    fun set(ctx: Context, category: String, key: String, value: String) {
        val doc = load(ctx)
        val cat = if (doc.has(category) && doc[category].isJsonObject) {
            doc.getAsJsonObject(category)
        } else {
            JsonObject().also { doc.add(category, it) }
        }
        val entry = JsonObject().apply {
            addProperty("value", value)
            addProperty("updated", today)
        }
        cat.add(key, entry)
        save(ctx, doc)
    }

    fun getValue(ctx: Context, category: String, key: String): String? {
        val doc = load(ctx)
        val cat = doc.getAsJsonObject(category) ?: return null
        val entry = cat.getAsJsonObject(key) ?: return null
        return entry.get("value")?.asString
    }

    fun formatForPrompt(ctx: Context, maxChars: Int = 2200): String {
        val doc = load(ctx)
        val lines = mutableListOf<String>()
        for ((cat, value) in doc.entrySet()) {
            if (!value.isJsonObject) continue
            val obj = value.asJsonObject
            if (obj.size() == 0) continue
            val parts = obj.entrySet().mapNotNull { (k, v) ->
                if (!v.isJsonObject) null
                else v.asJsonObject.get("value")?.asString?.let { "$k=$it" }
            }
            if (parts.isNotEmpty()) lines += "[$cat] " + parts.joinToString("; ")
        }
        if (lines.isEmpty()) return ""
        val joined = lines.joinToString("\n")
        return if (joined.length <= maxChars) "[USER MEMORY]\n$joined\n" else
            "[USER MEMORY]\n${joined.substring(0, maxChars)}...\n"
    }
}
