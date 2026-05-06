package com.jarvis.mobile.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.jarvis.mobile.live.LiveSession

/**
 * In-app HUD. Delegates all drawing to [HudRenderer]; mirrors state into
 * [HudStateHolder] so the live wallpaper engine stays in sync.
 */
class HudView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private val renderer = HudRenderer()

    var state: LiveSession.State
        get() = renderer.state
        set(value) {
            renderer.state = value
            HudStateHolder.state = value
        }

    init {
        postOnAnimation(object : Runnable {
            override fun run() {
                renderer.advance()
                invalidate()
                postOnAnimation(this)
            }
        })
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        renderer.draw(canvas, width, height, bottomReserve = 0)
    }
}
