package com.jarvis.mobile.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.jarvis.mobile.ui.HudRenderer
import com.jarvis.mobile.ui.HudStateHolder

/**
 * Live wallpaper engine that renders the same J.A.R.V.I.S HUD as the in-app
 * view. State is mirrored from [HudStateHolder] so the wallpaper reflects
 * whatever the running [com.jarvis.mobile.live.LiveSession] is doing.
 */
class JarvisWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = HudEngine()

    private inner class HudEngine : Engine() {

        private val renderer = HudRenderer()
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var width = 0
        private var height = 0

        private val frameRunner = object : Runnable {
            override fun run() {
                renderer.state = HudStateHolder.state
                renderer.advance()
                drawFrame()
                handler.removeCallbacks(this)
                if (visible) handler.postDelayed(this, 33L) // ~30 fps
            }
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            if (v) handler.post(frameRunner) else handler.removeCallbacks(frameRunner)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            width = w; height = h
            super.onSurfaceChanged(holder, format, w, h)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(frameRunner)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(frameRunner)
            super.onDestroy()
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.parseColor("#00060A"))
                    val w = if (width > 0) width else canvas.width
                    val h = if (height > 0) height else canvas.height
                    renderer.draw(canvas, w, h, bottomReserve = 0)
                }
            } catch (_: Throwable) {
                // Surface might be gone; ignore.
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas) } catch (_: Throwable) {}
                }
            }
        }
    }
}
