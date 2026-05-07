package com.jarvis.mobile.ui

import android.graphics.*
import com.jarvis.mobile.live.LiveSession
import kotlin.math.*
import kotlin.random.Random

/**
 * Self-contained renderer that holds all HUD animation state and draws a
 * single frame onto any [Canvas]. Used by [HudView] and by the live wallpaper
 * service so both share the exact same look.
 */
class HudRenderer(private val playBootIntro: Boolean = true) {

    var state: LiveSession.State = LiveSession.State.IDLE
        set(value) {
            if (value != field) {
                prevColor = currentColor
                colorTweenStart = System.currentTimeMillis()
            }
            field = value
        }

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
    private val waveform = FloatArray(48) { 0f }
    private var waveIdx = 0

    private val bootStartMs = System.currentTimeMillis()
    private val bootDurationMs = 3000L
    private val bootMessages = listOf(
        "▶ NEURAL CORE      INITIALIZING",
        "▶ MEMORY MATRIX    LOADED",
        "▶ AUDIO PIPELINE   ONLINE",
        "▶ GEMINI 2.5 LINK  HANDSHAKE OK",
        "▶ TOOL ARRAY       ARMED",
        "▶ J.A.R.V.I.S.     ONLINE."
    )

    private var prevColor: Int = 0xFF3A8A9A.toInt()
    private var currentColor: Int = 0xFF3A8A9A.toInt()
    private var colorTweenStart: Long = 0L
    private val colorTweenMs = 420L

    private data class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float)
    private val sparks = mutableListOf<Spark>()
    private data class PulseWave(var r: Float, var alpha: Float, var width: Float)
    private val pulseWaves = mutableListOf<PulseWave>()
    private var lastPulseTick = -100
    private data class OrbitDot(var ang: Float, var rad: Float, var speed: Float, var life: Float)
    private val orbits = mutableListOf<OrbitDot>()
    private data class DataDigit(var x: Float, var y: Float, var ch: Char, var alpha: Int, var fall: Float, var size: Float)
    private val edgeDigits = mutableListOf<DataDigit>()
    private val hexChars = "0123456789ABCDEF".toCharArray()
    private data class Arc(var x1: Float, var y1: Float, var x2: Float, var y2: Float, var life: Float, val seed: Int)
    private val arcs = mutableListOf<Arc>()

    private var cx = 0f; private var cy = 0f; private var baseR = 0f
    private var width = 0; private var height = 0

    /** Advance one animation tick. */
    fun step() {
        tick++
        pulse = ((sin(tick * 0.05) + 1.0) * 0.5).toFloat()

        val spd = if (state == LiveSession.State.SPEAKING) 3.5f else 1f
        ringAngles[0] = (ringAngles[0] + 0.6f * spd) % 360f
        ringAngles[1] = (ringAngles[1] - 1.0f * spd + 360f) % 360f
        ringAngles[2] = (ringAngles[2] + 1.7f * spd) % 360f
        ringAngles[3] = (ringAngles[3] - 0.4f * spd + 360f) % 360f
        ringAngles[4] = (ringAngles[4] + 2.3f * spd) % 360f
        scanAngle = (scanAngle + if (state == LiveSession.State.THINKING) 4f else 1.8f) % 360f

        val active = state == LiveSession.State.LISTENING || state == LiveSession.State.SPEAKING
        waveform[waveIdx % 48] = if (active) Random.nextFloat() * 0.85f + 0.1f
                                 else (waveform[waveIdx % 48] * 0.80f)
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
        for (s in sparks) { s.x += s.vx; s.y += s.vy; s.vy += 0.25f; s.life -= 0.04f }
        sparks.removeAll { it.life <= 0f }

        val pulseInterval = when (state) {
            LiveSession.State.SPEAKING   -> 16
            LiveSession.State.LISTENING  -> 55
            LiveSession.State.THINKING   -> 30
            LiveSession.State.CONNECTING -> 25
            else -> 90
        }
        if (tick - lastPulseTick > pulseInterval && baseR > 0f) {
            pulseWaves += PulseWave(baseR * 0.55f, 1f, 1.4f)
            lastPulseTick = tick
        }
        for (w in pulseWaves) { w.r += 2.4f; w.alpha -= 0.014f }
        pulseWaves.removeAll { it.alpha <= 0f }

        if (orbits.size < 22 && baseR > 0f) {
            orbits += OrbitDot(
                ang = Random.nextFloat() * 360f,
                rad = baseR * (1.05f + Random.nextFloat() * 1.20f),
                speed = 0.4f + Random.nextFloat() * 1.4f,
                life = 1f
            )
        }
        for (p in orbits) { p.ang = (p.ang + p.speed) % 360f; p.life -= 0.0035f }
        orbits.removeAll { it.life <= 0f }

        if (tick % 3 == 0 && width > 0) {
            val onLeft = Random.nextBoolean()
            edgeDigits += DataDigit(
                x = if (onLeft) 6f + Random.nextFloat() * 14f else width - 22f - Random.nextFloat() * 14f,
                y = -14f,
                ch = hexChars.random(),
                alpha = 90 + Random.nextInt(120),
                fall = 1.4f + Random.nextFloat() * 2.2f,
                size = 10f + Random.nextFloat() * 2f
            )
        }
        for (d in edgeDigits) { d.y += d.fall; d.alpha -= 1 }
        edgeDigits.removeAll { it.alpha <= 0 || it.y > height + 20f }

        if (state == LiveSession.State.SPEAKING && tick % 7 == 0 && baseR > 0f) {
            val hexR = baseR * 0.54f * 0.52f
            val a1 = Random.nextInt(6)
            val a2 = (a1 + 2 + Random.nextInt(3)) % 6
            val ang1 = (a1 * 60f - 30f + ringAngles[0] * 0.3f) * PI.toFloat() / 180f
            val ang2 = (a2 * 60f - 30f + ringAngles[0] * 0.3f) * PI.toFloat() / 180f
            arcs += Arc(
                cx + cos(ang1) * hexR, cy + sin(ang1) * hexR,
                cx + cos(ang2) * hexR, cy + sin(ang2) * hexR,
                1f, Random.nextInt()
            )
        }
        for (a in arcs) a.life -= 0.10f
        arcs.removeAll { it.life <= 0f }
    }

    /** Draw one frame. The caller is responsible for invoking [step] each
     *  frame as well (typically right before draw). */
    fun draw(canvas: Canvas, w: Int, h: Int) {
        width = w; height = h
        val wf = w.toFloat(); val hf = h.toFloat()
        cx = wf / 2f; cy = hf * 0.40f
        baseR = min(wf, hf) * 0.30f

        currentColor = colorForState()
        val tweened = tweenedColor()
        val (r, g, b) = decompose(tweened)
        val bp = if (playBootIntro) bootProgress() else 1f

        // Solid background — important for wallpaper since the surface is
        // not pre-cleared by us.
        canvas.drawColor(Color.argb(255, 0, 6, 10))

        drawGrid(canvas, wf, hf, r, g, b)
        drawScanlines(canvas, wf, hf, r, g, b)
        drawEdgeDataStream(canvas, r, g, b)

        val hudFade = ((bp - 0.40f) / 0.55f).coerceIn(0f, 1f)
        if (hudFade > 0.01f) {
            canvas.saveLayerAlpha(0f, 0f, wf, hf, (255 * hudFade).toInt())
            drawCornerBrackets(canvas, wf, hf, tweened, r, g, b)
            drawTopBar(canvas, wf, r, g, b)
            drawSideData(canvas, wf, r, g, b)
            drawScanSweep(canvas, r, g, b)
            drawTickMarks(canvas, r, g, b)
            drawPulseWaves(canvas, r, g, b)
            drawOrbits(canvas, r, g, b)
            drawRings(canvas, r, g, b)
            drawCore(canvas, tweened, r, g, b)
            drawArcs(canvas, r, g, b)
            drawWaveform(canvas, r, g, b)
            drawSparks(canvas, r, g, b)
            drawStateLabel(canvas, tweened, r, g, b)
            canvas.restore()
        }

        if (playBootIntro && bp < 1f) drawBootOverlay(canvas, wf, hf, bp, r, g, b)
    }

    private fun bootProgress(): Float {
        val elapsed = System.currentTimeMillis() - bootStartMs
        return (elapsed.toFloat() / bootDurationMs).coerceIn(0f, 1f)
    }

    private fun drawBootOverlay(canvas: Canvas, w: Float, h: Float, p: Float, r: Int, g: Int, b: Int) {
        if (p < 0.45f) {
            val maskAlpha = (255 * (1f - p / 0.45f).coerceIn(0f, 1f)).toInt()
            canvas.drawColor(Color.argb(maskAlpha, 0, 6, 10))
        }
        if (p < 0.22f) {
            val pp = p / 0.22f
            val coreA = (255 * (1f - pp * 0.6f)).toInt()
            pFill.color = Color.argb(coreA, 255, 255, 255)
            canvas.drawCircle(cx, cy, 4f + pp * 10f, pFill)
            for (i in 1..6) {
                pFill.color = Color.argb(((coreA - i * 28).coerceAtLeast(0)), r, g, b)
                canvas.drawCircle(cx, cy, 6f + pp * (15f + i * 6f), pFill)
            }
        }
        if (p in 0.10f..0.60f) {
            val pp = ((p - 0.10f) / 0.50f).coerceIn(0f, 1f)
            for (i in 0 until 5) {
                val pi = pp - i * 0.10f
                if (pi <= 0f || pi > 1f) continue
                val rr = pi * baseR * 2.6f
                val a = ((1f - pi) * 200f).toInt().coerceIn(0, 255)
                pStroke.strokeWidth = 2f
                pStroke.pathEffect = null
                pStroke.color = Color.argb(a, r, g, b)
                canvas.drawCircle(cx, cy, rr, pStroke)
            }
        }
        if (p in 0.25f..0.65f) {
            val pp = (p - 0.25f) / 0.40f
            val y = pp * h
            pStroke.strokeWidth = 2f
            pStroke.color = Color.argb(220, r, g, b)
            canvas.drawLine(0f, y, w, y, pStroke)
            for (i in 1..7) {
                val a = (60 - i * 7).coerceAtLeast(0)
                pStroke.color = Color.argb(a, r, g, b)
                pStroke.strokeWidth = (i * 1.6f)
                canvas.drawLine(0f, y - i * 2.5f, w, y - i * 2.5f, pStroke)
                canvas.drawLine(0f, y + i * 2.5f, w, y + i * 2.5f, pStroke)
            }
        }
        if (p > 0.20f) {
            val pp = ((p - 0.20f) / 0.80f).coerceIn(0f, 1f)
            pText.textAlign = Paint.Align.LEFT
            pText.textSize = 13f
            pText.typeface = Typeface.MONOSPACE
            val totalChars = bootMessages.sumOf { it.length + 1 }
            val charsToShow = (pp * totalChars).toInt()
            var consumed = 0
            val baseY = h * 0.62f
            for ((i, msg) in bootMessages.withIndex()) {
                val nMsg = (charsToShow - consumed).coerceIn(0, msg.length)
                if (nMsg > 0) {
                    val lineY = baseY + i * 22f
                    val sub = msg.substring(0, nMsg)
                    val complete = nMsg == msg.length
                    val color = if (complete) Color.argb(220, r, g, b) else Color.argb(210, 255, 255, 255)
                    pText.color = color
                    canvas.drawText(sub, 32f, lineY, pText)
                    if (!complete && (tick / 6) % 2 == 0) {
                        val cw = pText.measureText(sub)
                        pFill.color = Color.argb(220, 255, 255, 255)
                        canvas.drawRect(32f + cw + 2f, lineY - 11f, 32f + cw + 9f, lineY + 2f, pFill)
                    }
                }
                consumed += msg.length + 1
                if (consumed > charsToShow) break
            }
        }
        if (p > 0.65f) {
            val pp = ((p - 0.65f) / 0.35f).coerceIn(0f, 1f)
            val a = (255 * pp * pp).toInt()
            pTextC.textAlign = Paint.Align.CENTER
            pTextC.textSize = 36f * (0.85f + pp * 0.15f)
            pTextC.color = Color.argb(a, r, g, b)
            canvas.drawText("J.A.R.V.I.S.", cx, cy - baseR * 0.05f, pTextC)
            pTextC.textSize = 12f
            pTextC.color = Color.argb((a * 0.6f).toInt(), 255, 255, 255)
            canvas.drawText("MARK V  •  NEURAL LINK ESTABLISHED", cx, cy + 22f, pTextC)
        }
    }

    private fun drawGrid(canvas: Canvas, w: Float, h: Float, r: Int, g: Int, b: Int) {
        pStroke.color = Color.argb(14, r, g, b)
        pStroke.strokeWidth = 0.7f; pStroke.pathEffect = null
        val step = 44f
        var x = 0f; while (x <= w) { canvas.drawLine(x, 0f, x, h, pStroke); x += step }
        var y = 0f; while (y <= h) { canvas.drawLine(0f, y, w, y, pStroke); y += step }
    }

    private fun drawScanlines(canvas: Canvas, w: Float, h: Float, r: Int, g: Int, b: Int) {
        pStroke.strokeWidth = 1f
        pStroke.pathEffect = null
        pStroke.color = Color.argb(10, r, g, b)
        val offset = (tick * 0.5f) % 6f
        var y = -offset
        while (y < h) { canvas.drawLine(0f, y, w, y, pStroke); y += 6f }
    }

    private fun drawEdgeDataStream(canvas: Canvas, r: Int, g: Int, b: Int) {
        pText.textAlign = Paint.Align.LEFT
        for (d in edgeDigits) {
            pText.textSize = d.size
            pText.color = Color.argb(d.alpha.coerceIn(0, 255), r, g, b)
            canvas.drawText(d.ch.toString(), d.x, d.y, pText)
        }
    }

    private fun drawCornerBrackets(canvas: Canvas, w: Float, h: Float, col: Int, r: Int, g: Int, b: Int) {
        val len = 58f; val pad = 24f
        pStroke.strokeWidth = 2.8f; pStroke.pathEffect = null
        pStroke.color = Color.argb(210, r, g, b)
        canvas.drawLine(pad, pad + len, pad, pad, pStroke)
        canvas.drawLine(pad, pad, pad + len, pad, pStroke)
        canvas.drawLine(w - pad - len, pad, w - pad, pad, pStroke)
        canvas.drawLine(w - pad, pad, w - pad, pad + len, pStroke)
        canvas.drawLine(pad, h - pad - len, pad, h - pad, pStroke)
        canvas.drawLine(pad, h - pad, pad + len, h - pad, pStroke)
        canvas.drawLine(w - pad - len, h - pad, w - pad, h - pad, pStroke)
        canvas.drawLine(w - pad, h - pad, w - pad, h - pad - len, pStroke)
        pFill.color = col
        for ((px, py) in listOf(pad to pad, w - pad to pad, pad to h - pad, w - pad to h - pad))
            canvas.drawCircle(px, py, 3.5f, pFill)
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

    private fun drawPulseWaves(canvas: Canvas, r: Int, g: Int, b: Int) {
        for (w in pulseWaves) {
            val a = (w.alpha * 130).toInt().coerceIn(0, 255)
            pStroke.strokeWidth = w.width
            pStroke.pathEffect = null
            pStroke.color = Color.argb(a, r, g, b)
            canvas.drawCircle(cx, cy, w.r, pStroke)
        }
    }

    private fun drawOrbits(canvas: Canvas, r: Int, g: Int, b: Int) {
        for (p in orbits) {
            val a = (p.life * 200).toInt().coerceIn(0, 255)
            val ang = p.ang * PI.toFloat() / 180f
            pFill.color = Color.argb(a, r, g, b)
            canvas.drawCircle(cx + cos(ang) * p.rad, cy + sin(ang) * p.rad, 1.9f, pFill)
            for (i in 1..4) {
                val ta = ((p.life - i * 0.10f) * 110).toInt().coerceIn(0, 200)
                if (ta <= 0) break
                val tang = (p.ang - i * p.speed * 1.6f) * PI.toFloat() / 180f
                pFill.color = Color.argb(ta, r, g, b)
                canvas.drawCircle(cx + cos(tang) * p.rad, cy + sin(tang) * p.rad, 1.2f, pFill)
            }
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
        for (l in 6 downTo 1) {
            val lr = baseR * (0.60f + l * 0.14f)
            pFill.color = Color.argb((4 + l * 5 + (pulse * 8).toInt()).coerceAtMost(255), r, g, b)
            canvas.drawCircle(cx, cy, lr, pFill)
        }
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
        pStroke.color = col; pStroke.strokeWidth = 2.2f; pStroke.pathEffect = null
        canvas.drawCircle(cx, cy, coreR, pStroke)
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
        pFill.color = Color.argb(140, r, g, b)
        for (i in 0 until 6) {
            val ang = (i * 60f - 30f + ringAngles[0] * 0.3f) * PI.toFloat() / 180f
            canvas.drawCircle(cx + cos(ang) * hexR, cy + sin(ang) * hexR, 2.5f, pFill)
        }
        pStroke.color = Color.argb(170, r, g, b); pStroke.strokeWidth = 1.5f
        val cr = coreR * 0.19f
        canvas.drawLine(cx - cr, cy, cx + cr, cy, pStroke)
        canvas.drawLine(cx, cy - cr, cx, cy + cr, pStroke)
        pFill.color = Color.WHITE
        canvas.drawCircle(cx, cy, 3.5f, pFill)
    }

    private fun drawArcs(canvas: Canvas, r: Int, g: Int, b: Int) {
        for (a in arcs) {
            val al = (a.life * 255).toInt().coerceIn(0, 255)
            val rng = Random(a.seed + (tick / 2))
            val path = Path()
            path.moveTo(a.x1, a.y1)
            val segs = 5
            for (i in 1 until segs) {
                val t = i.toFloat() / segs
                val mx = a.x1 + (a.x2 - a.x1) * t + (rng.nextFloat() - 0.5f) * 14f
                val my = a.y1 + (a.y2 - a.y1) * t + (rng.nextFloat() - 0.5f) * 14f
                path.lineTo(mx, my)
            }
            path.lineTo(a.x2, a.y2)
            pStroke.color = Color.argb((al * 0.45f).toInt(), r, g, b)
            pStroke.strokeWidth = 7f
            canvas.drawPath(path, pStroke)
            pStroke.color = Color.argb(al, 255, 255, 255)
            pStroke.strokeWidth = 2.4f
            canvas.drawPath(path, pStroke)
        }
    }

    private fun drawWaveform(canvas: Canvas, r: Int, g: Int, b: Int) {
        val waveW = baseR * 2.2f
        val barMaxH = baseR * 0.30f
        val baseY = cy + baseR * 0.78f
        val startX = cx - waveW / 2f
        val slotW = waveW / 48f
        for (i in 0 until 48) {
            val amp = waveform[(waveIdx + i) % 48]
            val bh = (amp * barMaxH).coerceAtLeast(1.5f)
            val x = startX + i * slotW
            val grad = LinearGradient(0f, baseY - bh, 0f, baseY + bh,
                Color.argb(60, r, g, b), Color.argb(220, r, g, b),
                Shader.TileMode.CLAMP)
            pFill.shader = grad
            canvas.drawRoundRect(RectF(x + 1f, baseY - bh, x + slotW - 1f, baseY + bh), 2f, 2f, pFill)
            pFill.shader = null
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
        val labelY = cy + baseR * 1.18f
        pTextC.textAlign = Paint.Align.CENTER
        pTextC.textSize = 22f; pTextC.color = col
        canvas.drawText(label, cx, labelY, pTextC)
        val half = pTextC.measureText(label) / 2f + 18f
        pStroke.color = Color.argb(90, r, g, b); pStroke.strokeWidth = 1f
        canvas.drawLine(cx - baseR * 1.15f, labelY - 5f, cx - half, labelY - 5f, pStroke)
        canvas.drawLine(cx + half, labelY - 5f, cx + baseR * 1.15f, labelY - 5f, pStroke)
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

    private fun tweenedColor(): Int {
        val t = ((System.currentTimeMillis() - colorTweenStart).toFloat() / colorTweenMs).coerceIn(0f, 1f)
        if (t >= 1f) return currentColor
        val (r1, g1, b1) = decompose(prevColor)
        val (r2, g2, b2) = decompose(currentColor)
        val ease = (1f - cos(t * PI.toFloat())) / 2f
        val r = (r1 + (r2 - r1) * ease).toInt()
        val g = (g1 + (g2 - g1) * ease).toInt()
        val bl = (b1 + (b2 - b1) * ease).toInt()
        return Color.argb(255, r, g, bl)
    }

    private fun decompose(c: Int) = Triple(Color.red(c), Color.green(c), Color.blue(c))
}
