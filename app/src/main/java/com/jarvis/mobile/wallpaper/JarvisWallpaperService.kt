package com.jarvis.mobile.wallpaper

import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import com.jarvis.mobile.live.LiveSession
import com.jarvis.mobile.ui.HudRenderer

/**
 * Live wallpaper that draws the JARVIS HUD using [HudRenderer]. Runs without
 * any LiveSession — a pure ambient animation in LISTENING state colour.
 */
class JarvisWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = JarvisEngine()

    private inner class JarvisEngine : Engine() {
        // Wallpaper has no boot intro — it shouldn't replay every time the
        // engine wakes up. Just the ambient HUD.
        private val renderer = HudRenderer(playBootIntro = false).apply {
            state = LiveSession.State.LISTENING
        }

        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var width = 0
        private var height = 0

        private val frameCallback = Choreographer.FrameCallback { drawFrame() }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            if (visible) Choreographer.getInstance().postFrameCallback(frameCallback)
            else Choreographer.getInstance().removeFrameCallback(frameCallback)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            width = w
            height = h
            if (visible) drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacksAndMessages(null)
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: android.graphics.Canvas? = null
            try {
                canvas = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                    holder.lockHardwareCanvas()
                else
                    holder.lockCanvas()
                if (canvas != null) {
                    renderer.step()
                    renderer.draw(canvas, width, height)
                }
            } catch (_: Throwable) {
                // Surface might have been destroyed mid-frame; bail silently.
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas) } catch (_: Throwable) {}
                }
            }
            if (visible) Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }
}
