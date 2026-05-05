package com.jarvis.mobile.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.jarvis.mobile.live.LiveSession
import kotlin.math.*
import kotlin.random.Random

class HudView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    @Volatile var state: LiveSession.State = LiveSession.State.IDLE

    private val pStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val pFill   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pText   = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE }
    private val pTextC  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }

    private var tick = 0
    private var pulse = 0f
    private var scanAngle = 0f
    private val ringAngles = FloatArray(5) { it * 72f }

    private val waveform = FloatArray(32) { 0f }
    private var waveIdx = 0

    private data class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float)
    private val sparks = mutableListOf<Spark>()

    // Updated each frame so the animation loop can spawn sparks at the right position
    private var cx = 0f; private var cy = 0f; private var baseR = 0f

    init {
        postOnAnimation(object : Runnable {
            override fun run() {
                tick++
                pulse = ((sin(tick * 0.05) + 1.0) * 0.5).toFloat()
                val spd = if (state == LiveSession.State.SPEAKING) 3.5f else 1f
                ringAngles[0] = (ringAngles[0] + 0.6f  * spd) % 360f
                ringAngles[1] = (ringAngles[1] - 1.0f  * spd + 360f) % 360f
                ringAngles[2] = (ringAngles[2] + 1.7f  * spd) % 360f
                ringAngles[3] = (ringAngles[3] - 0.4f  * spd + 360f) % 360f
                ringAngles[4] = (ringAngles[4] + 2.3f  * spd) % 360f
                scanAngle = (scanAngle + if (state == LiveSession.State.THINKING) 4f else 1.8f) % 360f

                val active = state == LiveSession.State.LISTENING || state == LiveSession.State.SPEAKING
                waveform[waveIdx % 32] = if (active) Random.nextFloat() * 0.85f + 0.1f
                                         else (waveform[waveIdx % 32] * 0.80f)
                waveIdx++

                if (state == LiveSession.State.SPEAKING && tick % 2 == 0 && baseR > 0f) {
                    repeat(2) {
                        val ang = Random.nextDouble(0.0, 2 * PI).toFloat()
                        val sr = baseR * 0.54f
                        sparks += Spark(
                            cx + cos(ang) * sr, cy + sin(ang) * sr,
                            cos(ang) * (3f + Random.nextFloat() * 7f),
                            sin(ang) * (3f + Random.nextFloat() * 7f) - 1f,
                            1f
                        )
                    }
                }
                val it = sparks.iterator()
                while (it.hasNext()) {
                    val s = it.next()
                    s.x += s.vx; s.y += s.vy; s.vy += 0.25f
                    s.life -= 0.04f
                    if (s.life <= 0f) it.remove()
                }

                invalidate()
                postOnAnimation(this)
            }
        })
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        cx = w / 2f; cy = h * 0.40f
        baseR = min(w, h) * 0.30f
        val col = colorForState()
        val (r, g, b) = decompose(col)

        drawGrid(canvas, w, h, r, g, b)
        drawCornerBrackets(canvas, w, h, col, r, g, b)
        drawTopBar(canvas, w, r, g, b)
        drawSideData(canvas, w, r, g, b)
        drawScanSweep(canvas, r, g, b)
        drawTickMarks(canvas, r, g, b)
        drawRings(canvas, r, g, b)
        drawCore(canvas, col, r, g, b)
        drawWaveform(canvas, r, g, b)
        drawSparks(canvas, r, g, b)
        drawStateLabel(canvas, col, r, g, b)
    }

    private fun drawGrid(canvas: Canvas, w: Float, h: Float, r: Int, g: Int, b: Int) {
        pStroke.color = Color.argb(14, r, g, b)
        pStroke.strokeWidth = 0.7f; pStroke.pathEffect = null
        val step = 44f
        var x = 0f; while (x <= w) { canvas.drawLine(x, 0f, x, h, pStroke); x += step }
        var y = 0f; while (y <= h) { canvas.drawLine(0f, y, w, y, pStroke); y += step }
    }

    private fun drawCornerBrackets(canvas: Canvas, w: Float, h: Float, col: Int, r: Int, g: Int, b: Int) {
        val len = 58f; val pad = 24f
        pStroke.strokeWidth = 2.8f; pStroke.pathEffect = null
        pStroke.color = Color.argb(210, r, g, b)
        // TL
        canvas.drawLine(pad, pad + len, pad, pad, pStroke)
        canvas.drawLine(pad, pad, pad + len, pad, pStroke)
        // TR
        canvas.drawLine(w - pad - len, pad, w - pad, pad, pStroke)
        canvas.drawLine(w - pad, pad, w - pad, pad + len, pStroke)
        // BL
        canvas.drawLine(pad, h - pad - len, pad, h - pad, pStroke)
        canvas.drawLine(pad, h - pad, pad + len, h - pad, pStroke)
        // BR
        canvas.drawLine(w - pad - len, h - pad, w - pad, h - pad, pStroke)
        canvas.drawLine(w - pad, h - pad, w - pad, h - pad - len, pStroke)
        // Corner dots
        pFill.color = col
        for ((px, py) in listOf(pad to pad, w - pad to pad, pad to h - pad, w - pad to h - pad))
            canvas.drawCircle(px, py, 3.5f, pFill)
        // Corner micro-labels
        pText.textAlign = Paint.Align.LEFT
        pText.textSize = 11f
        pText.color = Color.argb(100, r, g, b)
        canvas.drawText("00°N 00°E", pad + 8f, pad + 18f, pText)
        pText.textAlign = Paint.Align.RIGHT
        canvas.drawText("MARK V", w - pad - 8f, pad + 18f, pText)
        pText.textAlign = Paint.Align.LEFT
    }

    private fun drawTopBar(canvas: Canvas, w: Float, r: Int, g: Int, b: Int) {
        pStroke.color = Color.argb(80, r, g, b)
        pStroke.strokeWidth = 1f; pStroke.pathEffect = null
        canvas.drawLine(0f, 82f, w, 82f, pStroke)

        pText.textAlign = Paint.Align.LEFT
        pText.textSize = 20f
        pText.color = Color.argb(210, r, g, b)
        canvas.drawText("J.A.R.V.I.S  NEURAL LINK", 92f, 62f, pText)

        pText.textAlign = Paint.Align.RIGHT
        pText.textSize = 14f
        pText.color = Color.argb(130, r, g, b)
        canvas.drawText("GEMINI 2.5 FLASH  ●  ACTIVE", w - 92f, 62f, pText)
        pText.textAlign = Paint.Align.LEFT
    }

    private fun drawSideData(canvas: Canvas, w: Float, r: Int, g: Int, b: Int) {
        pText.textSize = 13f
        pText.color = Color.argb(80, r, g, b)
        val mid = cy - 20f
        val lx = 30f
        arrayOf("NEURAL INTERFACE", "SIG:  ████████░░", "LATENCY: --ms", "MEM:  ONLINE").forEachIndexed { i, s ->
            canvas.drawText(s, lx, mid + i * 22f, pText)
        }
        pText.textAlign = Paint.Align.RIGHT
        val proc = 40 + (pulse * 60).toInt()
        arrayOf("AUDIO  I/O  ACTIVE", "PROC:  ${proc}%", "TOOLS:  ARMED", "VER:  2.5F").forEachIndexed { i, s ->
            canvas.drawText(s, w - 30f, mid + i * 22f, pText)
        }
        pText.textAlign = Paint.Align.LEFT
    }

    private fun drawScanSweep(canvas: Canvas, r: Int, g: Int, b: Int) {
        if (state != LiveSession.State.THINKING && state != LiveSession.State.CONNECTING) return
        val sr = baseR * 2.3f
        val shader = SweepGradient(cx, cy,
            intArrayOf(Color.argb(0, r, g, b), Color.argb(65, r, g, b), Color.argb(0, r, g, b)),
            floatArrayOf(0f, 0.07f, 0.15f))
        pFill.shader = shader
        canvas.save(); canvas.rotate(scanAngle, cx, cy)
        canvas.drawCircle(cx, cy, sr, pFill)
        canvas.restore()
        pFill.shader = null
    }

    private fun drawTickMarks(canvas: Canvas, r: Int, g: Int, b: Int) {
        val tickR = baseR * 2.18f
        pStroke.strokeWidth = 1f; pStroke.pathEffect = null
        for (i in 0 until 72) {
            val ang = (i * 5f - 90f) * PI.toFloat() / 180f
            val major = i % 9 == 0
            val inner = tickR - if (major) 14f else 5f
            pStroke.color = Color.argb(if (major) 190 else 45, r, g, b)
            canvas.drawLine(cx + cos(ang) * inner, cy + sin(ang) * inner,
                cx + cos(ang) * (tickR + 2f), cy + sin(ang) * (tickR + 2f), pStroke)
        }
    }

    private fun drawRings(canvas: Canvas, r: Int, g: Int, b: Int) {
        data class Ring(val radius: Float, val segs: Int, val sweep: Float, val alpha: Int, val w: Float, val ai: Int)
        val rings = listOf(
            Ring(baseR * 2.15f,  8,  28f,  65, 1.2f, 0),
            Ring(baseR * 1.85f,  6,  48f,  85, 1.8f, 1),
            Ring(baseR * 1.62f, 10,  20f, 105, 1.4f, 2),
            Ring(baseR * 1.40f,  4,  72f, 135, 2.5f, 3),
            Ring(baseR * 1.20f,  3, 108f, 155, 3.2f, 4)
        )
        pStroke.pathEffect = null
        for (ring in rings) {
            val rect = RectF(cx - ring.radius, cy - ring.radius, cx + ring.radius, cy + ring.radius)
            pStroke.color = Color.argb(ring.alpha, r, g, b)
            pStroke.strokeWidth = ring.w
            val segAngle = 360f / ring.segs
            for (s in 0 until ring.segs)
                canvas.drawArc(rect, ringAngles[ring.ai] + s * segAngle, ring.sweep, false, pStroke)
        }
    }

    private fun drawCore(canvas: Canvas, col: Int, r: Int, g: Int, b: Int) {
        // Outer glow layers
        for (l in 6 downTo 1) {
            val lr = baseR * (0.60f + l * 0.14f)
            pFill.color = Color.argb((4 + l * 5 + (pulse * 8).toInt()).coerceAtMost(255), r, g, b)
            canvas.drawCircle(cx, cy, lr, pFill)
        }
        // Core with radial gradient
        val coreR = baseR * (0.54f + pulse * 0.04f)
        val shader = RadialGradient(cx, cy, coreR,
            intArrayOf(
                Color.argb(255, (r + 100).coerceAtMost(255), (g + 100).coerceAtMost(255), (b + 100).coerceAtMost(255)),
                col,
                Color.argb(200, r / 2, g / 2, b / 2)
            ),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        pFill.shader = shader
        canvas.drawCircle(cx, cy, coreR, pFill)
        pFill.shader = null

        // Core outline ring
        pStroke.color = col; pStroke.strokeWidth = 2.2f; pStroke.pathEffect = null
        canvas.drawCircle(cx, cy, coreR, pStroke)

        // Rotating inner hexagon
        val hexPath = Path()
        val hexR = coreR * 0.52f
        for (i in 0 until 6) {
            val ang = (i * 60f - 30f + ringAngles[0] * 0.3f) * PI.toFloat() / 180f
            val hx = cx + cos(ang) * hexR; val hy = cy + sin(ang) * hexR
            if (i == 0) hexPath.moveTo(hx, hy) else hexPath.lineTo(hx, hy)
        }
        hexPath.close()
        pStroke.color = Color.argb(110, r, g, b); pStroke.strokeWidth = 1f
        canvas.drawPath(hexPath, pStroke)

        // Hex vertices as small dots
        pFill.color = Color.argb(140, r, g, b)
        for (i in 0 until 6) {
            val ang = (i * 60f - 30f + ringAngles[0] * 0.3f) * PI.toFloat() / 180f
            canvas.drawCircle(cx + cos(ang) * hexR, cy + sin(ang) * hexR, 2.5f, pFill)
        }

        // Center crosshair
        pStroke.color = Color.argb(170, r, g, b); pStroke.strokeWidth = 1.5f
        val cr = coreR * 0.19f
        canvas.drawLine(cx - cr, cy, cx + cr, cy, pStroke)
        canvas.drawLine(cx, cy - cr, cx, cy + cr, pStroke)
        pFill.color = Color.WHITE
        canvas.drawCircle(cx, cy, 3.5f, pFill)
    }

    private fun drawWaveform(canvas: Canvas, r: Int, g: Int, b: Int) {
        val waveW = baseR * 2.0f
        val barMaxH = baseR * 0.26f
        val baseY = cy + baseR * 0.76f
        val startX = cx - waveW / 2f
        val slotW = waveW / 32f
        pFill.color = Color.argb(155, r, g, b)
        for (i in 0 until 32) {
            val amp = waveform[(waveIdx + i) % 32]
            val bh = (amp * barMaxH).coerceAtLeast(1.5f)
            val x = startX + i * slotW
            canvas.drawRoundRect(RectF(x + 1f, baseY - bh, x + slotW - 1f, baseY + bh), 2f, 2f, pFill)
        }
    }

    private fun drawSparks(canvas: Canvas, r: Int, g: Int, b: Int) {
        for (s in sparks) {
            val a = (s.life * 255).toInt().coerceIn(0, 255)
            pFill.color = Color.argb(a, r, g, b)
            canvas.drawCircle(s.x, s.y, (3.5f * s.life).coerceAtLeast(0.5f), pFill)
        }
    }

    private fun drawStateLabel(canvas: Canvas, col: Int, r: Int, g: Int, b: Int) {
        val label = when (state) {
            LiveSession.State.IDLE        -> "STANDBY"
            LiveSession.State.CONNECTING  -> "CONNECTING..."
            LiveSession.State.LISTENING   -> "LISTENING"
            LiveSession.State.THINKING    -> "PROCESSING"
            LiveSession.State.SPEAKING    -> "RESPONDING"
            LiveSession.State.MUTED       -> "MUTED"
            LiveSession.State.ERROR       -> "ERROR"
        }
        val labelY = cy + baseR * 1.14f
        pTextC.textSize = 22f; pTextC.color = col
        canvas.drawText(label, cx, labelY, pTextC)

        // Side decorators
        val half = pTextC.measureText(label) / 2f + 18f
        pStroke.color = Color.argb(90, r, g, b); pStroke.strokeWidth = 1f
        canvas.drawLine(cx - baseR * 1.1f, labelY - 5f, cx - half, labelY - 5f, pStroke)
        canvas.drawLine(cx + half, labelY - 5f, cx + baseR * 1.1f, labelY - 5f, pStroke)
        // Small diamond decorators
        val dx = half - 6f
        pFill.color = Color.argb(140, r, g, b)
        drawDiamond(canvas, cx - dx, labelY - 5f, 4f)
        drawDiamond(canvas, cx + dx, labelY - 5f, 4f)
    }

    private fun drawDiamond(canvas: Canvas, x: Float, y: Float, size: Float) {
        val path = Path()
        path.moveTo(x, y - size); path.lineTo(x + size, y)
        path.lineTo(x, y + size); path.lineTo(x - size, y); path.close()
        canvas.drawPath(path, pFill)
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
