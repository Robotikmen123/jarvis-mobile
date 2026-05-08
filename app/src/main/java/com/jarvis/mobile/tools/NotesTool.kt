package com.jarvis.mobile.tools

import android.content.Context
import com.jarvis.mobile.memory.MemoryStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotesTool : JarvisTool {
    override val name = "save_note"

    override fun declaration() = functionDecl(
        name,
        "Save a quick free-form note for later (timestamped, stored in user memory)."
    ) {
        str("text", "Note content", required = true)
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val text = (args["text"] as? String)?.trim().orEmpty()
        if (text.isEmpty()) return "Not metni eksik."
        val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        MemoryStore.set(ctx, "notes", ts, text)
        return "Not kaydedildi."
    }
}
