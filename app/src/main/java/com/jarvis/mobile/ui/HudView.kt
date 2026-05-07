package com.jarvis.mobile.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.jarvis.mobile.live.LiveSession

/**
 * Thin View wrapper around [HudRenderer]. The renderer holds all animation
 * state and drawing logic; this class only forwards size + drives the
 * per-frame invalidate loop.
 */
class HudView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private val renderer = HudRenderer(playBootIntro = true)

    var state: LiveSession.State
        get() = renderer.state
        set(value) { renderer.state = value }

    init {
        postOnAnimation(object : Runnable {
            override fun run() {
                renderer.step()
                invalidate()
                postOnAnimation(this)
            }
        })
    }

    override fun onDraw(canvas: Canvas) {
        renderer.draw(canvas, width, height)
    }
}
