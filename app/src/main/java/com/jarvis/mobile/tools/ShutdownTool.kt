package com.jarvis.mobile.tools

import android.content.Context
import com.google.gson.JsonObject

object ShutdownTool : JarvisTool {
    override val name = "shutdown_jarvis"

    override fun declaration(): JsonObject = functionDecl(
        name,
        "Closes the Jarvis session when the user explicitly asks to stop or says goodbye."
    )

    @Volatile var requested: Boolean = false

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        requested = true
        return "Shutdown acknowledged."
    }
}
