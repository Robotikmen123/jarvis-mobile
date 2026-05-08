package com.jarvis.mobile.tools

import android.content.Context
import android.content.Intent
import android.net.Uri

object CallTool : JarvisTool {
    override val name = "call_number"

    override fun declaration() = functionDecl(
        name,
        "Open the phone dialer pre-filled with a number. The user must tap call."
    ) {
        str("number", "Phone number (digits, +, spaces, dashes are OK)", required = true)
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val num = (args["number"] as? String)?.trim().orEmpty()
        if (num.isEmpty()) return "Numara eksik."
        val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(num)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { ctx.startActivity(i); "Aramaya hazir: $num" }
            .getOrElse { "Arama acilamadi: ${it.message}" }
    }
}
