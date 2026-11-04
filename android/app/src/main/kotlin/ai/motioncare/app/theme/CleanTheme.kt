package ai.motioncare.app.theme

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import ai.motioncare.app.ui.RiskRing
import androidx.core.graphics.PathParser
import kotlin.math.max
import kotlin.math.sin

/**
 * Car-UI traffic-light icon: a glossy coloured disc with a white face, wrapped in the shared
 * [RiskRing] that fills with the chance of sickness. The disc breathes and blinks.
 *
 * Geometry lives in a fixed 0..100 box scaled by size / 100. Shadows are drawn with the canvas
 * temporarily scaled back to pixels so their blur and offset stay in pixels at every size.
 */
object CleanTheme : MoodTheme {
    override val id = "clean"
    override val title = "깔끔한 기본"
    override val subtitle = "한눈에 보이는 신호등형"
    override val animated = true

    override fun label(mood: Mood): String = when (mood) {
        Mood.CALM -> "잔잔함"
        Mood.UNEASY -> "흔들림 있음"
        Mood.SICK -> "흔들림 많음"
    }

    private const val TAU = 6.2831855f
    private const val DEG = 57.29578f
    private const val R = 42f
    private const val RING_R = 46.5f
    private const val RING_W = 5f

    // hi, mid, lo, rim per mood (CALM, UNEASY, SICK)
    private val PAL = arrayOf(
        intArrayOf(0xFF6BD992.toInt(), 0xFF16A34A.toInt(), 0xFF138E40.toInt(), 0xFF0F7535.toInt()),
        intArrayOf(0xFFFDE68A.toInt(), 0xFFEAB308.toInt(), 0xFFD39F06.toInt(), 0xFFB98A04.toInt()),
        intArrayOf(0xFFF88A8A.toInt(), 0xFFDC2626.toInt(), 0xFFC21F1F.toInt(), 0xFFA51B1B.toInt()),
    )

    private fun pd(d: String): Path = PathParser.createPathFromPathData(d)

    private val smile by lazy { pd("M35.5 58 C41 66 59 66 64.5 58") }
    private val frown by lazy { pd("M38.5 63 C44 59.8 52 60 61.5 64.6") }
    private val browL by lazy { pd("M31.5 36.6 C34.2 35.2 36.6 34.4 39.2 34") }
    private val browR by lazy { pd("M68.5 36.6 C65.8 35.2 63.4 34.4 60.8 34") }
    private val wave by lazy {
        pd("M35 63.5 C39 56.5 44 56.5 47 61.5 C50 66.5 55 66.5 58 61.5 C60 58.3 63 58.3 65 61")
    }
    private val xL by lazy { pd("M31.5 39.5 L40 48 M40 39.5 L31.5 48") }
    private val xR by lazy { pd("M60 39.5 L68.5 48 M68.5 39.5 L60 48") }
    private val drop by lazy {
        pd("M75.5 38 C75.5 38 72.7 41.8 72.7 43.8 C72.7 45.4 73.9 46.6 75.5 46.6 C77.1 46.6 78.3 45.4 78.3 43.8 C78.3 41.8 75.5 38 75.5 38 Z")
    }
    private val discPath by lazy { Path().apply { addCircle(50f, 50f, R, Path.Direction.CW) } }

    // Canvas2D shadowBlur (sigma = blur / 2, px) -> Paint.setShadowLayer radius (sigma = 0.57735 r + 0.5).
    private fun blurRadius(blurPx: Float): Float = max((blurPx * 0.5f - 0.5f) / 0.57735f, 0.01f)

    private val bodyShaders by lazy { Array(3) { bodyShader(PAL[it]) } }

    private fun bodyShader(p: IntArray): Shader {
        val stops = floatArrayOf(0f, 0.5f, 0.88f, 1f)
        return if (Build.VERSION.SDK_INT >= 31) {
            // two-point conical, same as createRadialGradient(38,32,1, 50,50,R*1.04)
            RadialGradient(
                38f, 32f, 1f, 50f, 50f, R * 1.04f,
                longArrayOf(Color.pack(p[0]), Color.pack(p[1]), Color.pack(p[2]), Color.pack(p[3])),
                stops, Shader.TileMode.CLAMP,
            )
        } else {
            RadialGradient(46f, 44f, 48f, p, stops, Shader.TileMode.CLAMP)
        }
    }

    private val highlightShader by lazy {
        RadialGradient(
            36f, 28f, 26f,
            intArrayOf(Color.argb(77, 255, 255, 255), Color.argb(26, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
    }
    private val sickTintShader by lazy {
        LinearGradient(
            0f, 60f, 0f, 92f,
            intArrayOf(Color.argb(0, 127, 29, 29), Color.argb(70, 127, 29, 29), Color.argb(120, 110, 22, 22)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
    }
    private val dropShader by lazy {
        LinearGradient(0f, 38f, 0f, 46.6f, 0xFFEAF8FF.toInt(), 0xFF9AD8F8.toInt(), Shader.TileMode.CLAMP)
    }

    private val discShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        setShadowLayer(blurRadius(5f), 0f, 2f, Color.argb(56, 15, 23, 42))
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ridgeWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.5f }
    private val ridgeBlack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
        color = Color.argb(7, 0, 0, 0)
    }
    private val RIDGE_ALPHA = intArrayOf(15, 14, 12, 11, 9, 8) // 0.060 .. 0.030
    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val innerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        setShadowLayer(blurRadius(4f), 0f, 1.2f, Color.argb(51, 0, 0, 0))
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val specPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(179, 255, 255, 255)
    }
    private val faceFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        setShadowLayer(blurRadius(1.2f), 0f, 0.9f, Color.argb(46, 0, 0, 0))
    }
    private val faceStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
        setShadowLayer(blurRadius(1.2f), 0f, 0.9f, Color.argb(46, 0, 0, 0))
    }
    private val dropShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFC2E8FB.toInt()
        setShadowLayer(blurRadius(1f), 0f, 0.6f, Color.argb(41, 0, 0, 0))
    }
    private val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val tmpPath = Path()
    private val matrix = Matrix()
    private val rect = RectF()

    /** Strokes a 0..100-box path while the canvas is in pixel scale [u]. */
    private fun strokePx(c: Canvas, p: Path, width: Float, u: Float) {
        p.transform(matrix, tmpPath)
        faceStroke.strokeWidth = width * u
        c.drawPath(tmpPath, faceStroke)
    }

    private fun eyesPx(c: Canvas, x: Float, y: Float, rx: Float, ry: Float, u: Float) {
        rect.set((x - rx) * u, (y - ry) * u, (x + rx) * u, (y + ry) * u)
        c.drawOval(rect, faceFill)
        val x2 = 100f - x
        rect.set((x2 - rx) * u, (y - ry) * u, (x2 + rx) * u, (y + ry) * u)
        c.drawOval(rect, faceFill)
    }

    override fun draw(c: Canvas, cx: Float, cy: Float, size: Float, mood: Mood, level: Float, t: Float) {
        if (size <= 0f) return
        val pal = PAL[mood.ordinal]
        val period = when (mood) {
            Mood.SICK -> 2.6f
            Mood.UNEASY -> 3.2f
            Mood.CALM -> 4.0f
        }
        val breath = 0.5f + 0.5f * sin(TAU * t / period)
        val k = size / 100f

        c.save()
        c.translate(cx, cy)
        c.scale(k, k)
        c.translate(-50f, -50f)

        RiskRing.draw(c, 50f, 50f, RING_R, RING_W)

        // disc (breathing)
        val s = 1f + 0.018f * (breath - 0.5f) * 2f * (if (mood == Mood.CALM) 0.7f else 1f)
        c.translate(50f, 50f)
        c.scale(s, s)
        c.translate(-50f, -50f)
        val u = k * s
        val inv = 1f / u
        matrix.setScale(u, u)

        // soft drop shadow
        c.save()
        c.scale(inv, inv)
        discShadowPaint.color = pal[2]
        c.drawCircle(50f * u, 50f * u, R * u, discShadowPaint)
        c.restore()
        // body gradient (light from top-left)
        bodyPaint.shader = bodyShaders[mood.ordinal]
        c.drawCircle(50f, 50f, R, bodyPaint)

        c.save()
        c.clipPath(discPath)
        // Fresnel-lens ridges (very faint; vanish at small sizes)
        for (i in 0 until 6) {
            val r = 8f + i * 6.2f
            ridgeWhite.color = Color.argb(RIDGE_ALPHA[i], 255, 255, 255)
            c.drawCircle(50f, 50f, r, ridgeWhite)
            c.drawCircle(50f, 50f, r + 0.7f, ridgeBlack)
        }
        // SICK: lower face darkens (no green tint: green means calm)
        if (mood == Mood.SICK) {
            tintPaint.shader = sickTintShader
            c.drawRect(0f, 60f, 100f, 100f, tintPaint)
        }
        // soft inner shadow
        c.save()
        c.scale(inv, inv)
        innerShadowPaint.strokeWidth = 8f * u
        c.drawCircle(50f * u, 50f * u, (R + 4f) * u, innerShadowPaint)
        c.restore()
        c.restore()

        // soft top-left highlight, limited to the disc
        highlightPaint.shader = highlightShader
        c.drawCircle(50f, 50f, R, highlightPaint)
        // specular dot
        c.save()
        c.translate(28.5f, 27.5f)
        c.rotate(-0.75f * DEG)
        rect.set(-2.6f, -1.5f, 2.6f, 1.5f)
        c.drawOval(rect, specPaint)
        c.restore()

        // face
        c.save()
        if (mood == Mood.SICK) {
            c.translate(50f, 52f)
            c.rotate(sin(TAU * t / 3.4f) * 0.05f * DEG)
            c.translate(-50f, -52f)
        }
        c.scale(inv, inv)
        val blink = if ((t + 1.6f) % 4.2f < 0.12f) 0.15f else 1f
        when (mood) {
            Mood.CALM -> {
                eyesPx(c, 36f, 44f, 3.7f, 5.3f * blink, u)
                strokePx(c, smile, 4.8f, u)
            }
            Mood.UNEASY -> {
                eyesPx(c, 37f, 46f, 3.9f, 5.0f * blink, u)
                strokePx(c, browL, 2.6f, u)
                strokePx(c, browR, 2.6f, u)
                strokePx(c, frown, 4.6f, u)
            }
            Mood.SICK -> {
                strokePx(c, xL, 4.8f, u)
                strokePx(c, xR, 4.8f, u)
                strokePx(c, wave, 5.0f, u)
            }
        }
        c.restore()

        // sweat drop: only when big enough to read
        if (mood == Mood.UNEASY && size >= 96f) {
            c.save()
            c.translate(0f, breath)
            c.save()
            c.scale(inv, inv)
            drop.transform(matrix, tmpPath)
            c.drawPath(tmpPath, dropShadowPaint)
            c.restore()
            dropPaint.shader = dropShader
            c.drawPath(drop, dropPaint)
            c.restore()
        }

        c.restore()
    }
}
