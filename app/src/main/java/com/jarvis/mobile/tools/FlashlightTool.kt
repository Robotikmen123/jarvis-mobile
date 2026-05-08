package com.jarvis.mobile.tools

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

object FlashlightTool : JarvisTool {
    override val name = "flashlight"
    @Volatile private var lastState: Boolean = false

    override fun declaration() = functionDecl(
        name,
        "Turn the device flashlight on/off (or toggle if 'on' is omitted)."
    ) {
        bool("on", "true to turn on, false to turn off, omit to toggle")
    }

    override suspend fun execute(ctx: Context, args: Map<String, Any?>): String {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return runCatching {
            val ids = cm.cameraIdList
            val id = ids.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return@runCatching "Bu cihazda fener yok."
            val target = (args["on"] as? Boolean) ?: !lastState
            cm.setTorchMode(id, target)
            lastState = target
            if (target) "Fener acildi." else "Fener kapatildi."
        }.getOrElse { "Fener kontrol edilemedi: ${it.message}" }
    }
}
