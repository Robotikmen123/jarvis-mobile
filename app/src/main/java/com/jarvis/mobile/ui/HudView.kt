package com.jarvis.mobile.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.jarvis.mobile.live.LiveSession
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class HudView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    @Volatile var state: LiveSession.State = LiveSession.State.IDLE

    private val paintRing  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    private val paintHalo  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val paintCore  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }

    private var tick = 0
    private var pulse = 0f
    private val rings = floatArrayOf(0f, 120f, 240f)

    init {
        postOnAnimation(object : Runnable {
            override fun run() {
                tick++
                pulse = (sin(tick * 0.06).toFloat() + 1f) * 0.5f
                val speeds = if (state == LiveSession.State.SPEAKING) floatArrayOf(2.4f, -1.6f, 3.0f)
                             else floatArrayOf(0.8f, -0.5f, 1.2f)
                for (i in rings.indices) rings[i] = (rings[i] + speeds[i]) % 360
                invalidate()
                postOnAnimation(this)
            }
        })
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h / 2f
        val base = min(w, h) * 0.36f
        val color = colorForState()
        val (r, g, b) = decompose(color)

        // Halo
        paintHalo.color = Color.argb(30 + (pulse * 50).toInt(), r, g, b)
        canvas.drawCircle(cx, cy, base * 1.55f, paintHalo)
        paintHalo.color = Color.argb(80 + (pulse * 80).toInt(), r, g, b)
        canvas.drawCircle(cx, cy, base * 1.05f, paintHalo)

        // Core orb
        paintCore.color = color
        val coreR = base * (0.55f + pulse * 0.06f)
        canvas.drawCircle(cx, cy, coreR, paintCore)

        // Rings
        paintRing.color = color
        for ((i, ringR) in floatArrayOf(base * 1.3f, base * 1.65f, base * 2.0f).withIndex()) {
            val rect = RectF(cx - ringR, cy - ringR, cx + ringR, cy + ringR)
            paintRing.alpha = 130 - i * 30
            val sweep = 60f + i * 50f
            canvas.drawArc(rect, rings[i], sweep, false, paintRing)
        }

        // Sparks
        if (state == LiveSession.State.SPEAKING) {
            paintRing.alpha = 220
            for (k in 0 until 6) {
                val ang = Random.nextDouble(0.0, 2 * PI).toFloat()
                val r1 = base * 1.2f
                val r2 = base * (1.4f + Random.nextFloat() * 0.4f)
                val x1 = cx + cos(ang) * r1; val y1 = cy + sin(ang) * r1
                val x2 = cx + cos(ang) * r2; val y2 = cy + sin(ang) * r2
                canvas.drawLine(x1, y1, x2, y2, paintRing)
            }
        }

        // Label
        paintLabel.color = color
        canvas.drawText(state.name, cx, h - 60f, paintLabel)
    }

    private fun colorForState(): Int = when (state) {
        LiveSession.State.IDLE        -> 0xFF3A8A9A.toInt()
        LiveSession.State.CONNECTING  -> 0xFFFFCC00.toInt()
        LiveSession.State.LISTENING   -> 0xFF00D4FF.toInt()
        LiveSession.State.THINKING    -> 0xFFFF6B00.toInt()
        LiveSession.State.SPEAKING    -> 0xFF00FF88.toInt()
        LiveSession.State.MUTED       -> 0xFFFF3366.toInt()
        LiveSession.State.ERROR       -> 0xFFFF3355.toInt()
    }

    private fun decompose(c: Int) = Triple(Color.red(c), Color.green(c), Color.blue(c))
}
