package com.jarvis.mobile.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.JsonObject

object SendMessageTool : JarvisTool {
    override val name = "send_message"

    override fun declaration(): JsonObject = functionDecl(
        name,
        "Opens a messaging app (WhatsApp, Telegram, SMS) with the message pre-filled. " +
        "User must tap send."
    ) {
        str("receiver", "Recipient name or phone number", required = true)
        str("message_text", "The message content", required = true)
        str("platform", "whatsapp | telegram | sms", required = true)
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val to       = (args["receiver"] as? String)?.trim().orEmpty()
        val text     = (args["message_text"] as? String).orEmpty()
        val platform = (args["platform"] as? String)?.lowercase()?.trim().orEmpty()
        if (to.isEmpty() || text.isEmpty()) return "Receiver or message missing."

        val intent: Intent = when (platform) {
            "whatsapp" -> Intent(Intent.ACTION_VIEW, Uri.parse(
                "https://wa.me/${to.filter { it.isDigit() || it == '+' }}?text=" + Uri.encode(text)
            ))
            "telegram" -> Intent(Intent.ACTION_VIEW, Uri.parse(
                "https://t.me/${to.removePrefix("@")}?text=" + Uri.encode(text)
            ))
            "sms" -> Intent(Intent.ACTION_VIEW, Uri.parse("smsto:$to")).apply {
                putExtra("sms_body", text)
            }
            else -> Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        }.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        return runCatching {
            ctx.startActivity(intent)
            "Opened $platform with message draft for $to."
        }.getOrElse { "Could not open $platform: ${it.message}" }
    }
}
