package com.jarvis.mobile.tools

import android.content.Context
import android.content.Intent
import android.provider.MediaStore

object CameraTool : JarvisTool {
    override val name = "open_camera"

    override fun declaration() = functionDecl(
        name,
        "Open the camera app to take a photo."
    )

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { ctx.startActivity(i); "Kamera acildi." }
            .getOrElse { "Kamera acilamadi: ${it.message}" }
    }
}
