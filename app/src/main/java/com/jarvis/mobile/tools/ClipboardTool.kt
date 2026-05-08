package com.jarvis.mobile.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardTool : JarvisTool {
    override val name = "clipboard"

    override fun declaration() = functionDecl(
        name,
        "Read or write the system clipboard."
    ) {
        str("action", "'get' or 'set'", required = true)
        str("text",   "Text to copy (only for action=set)")
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return when ((args["action"] as? String)?.lowercase()) {
            "set" -> {
                val text = (args["text"] as? String).orEmpty()
                cm.setPrimaryClip(ClipData.newPlainText("jarvis", text))
                "Panoya kopyalandi."
            }
            else -> {
                val text = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                if (text.isEmpty()) "Pano bos." else "Panoda: $text"
            }
        }
    }
}
