package ai.motioncare.app.theme

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.sin

/**
 * Flat, car-UI style traffic-light icon: a coloured disc with a minimal white line-art face.
 * CALM is still; UNEASY / SICK get a slowly breathing outer ring (and SICK a gentle sway).
 * Everything is drawn around the origin after translating to (cx, cy), so cached shaders only
 * depend on size and mood.
 */
object CleanTheme : MoodTheme {
    override val id = "clean"
    override val title = "깔끔한 기본"
    override val subtitle = "한눈에 보이는 신호등형"
    override val animated = true

    override fun label(mood: Mood): String = when (mood) {
        Mood.CALM -> "편안함"
        Mood.UNEASY -> "흔들림 주의"
        Mood.SICK -> "멀미 위험"
    }

    // Geometry as fractions of size. Max extent: ring 0.415 * 1.04 + 0.0175 = 0.449 < 0.46.
    private const val DISC = 0.38f
    private const val RING = 0.415f
    private const val RING_STROKE = 0.035f
    private const val TWO_PI = 6.2831855f

    private val CALM_COLOR = 0xFF15803D.toInt()
    private val UNEASY_COLOR = 0xFFD97706.toInt()
    private val SICK_COLOR = 0xFFC2410C.toInt()

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.BLACK }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
        alpha = 64
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.WHITE }

    private val path = Path()
    private val rect = RectF()

    private class Shaders(val disc: Shader, val shadow: Shader)

    // One slot per mood, rebuilt only when the size changes (no boxing / allocation per frame).
    private val cache = arrayOfNulls<Shaders>(3)
    private val cacheSize = FloatArray(3)

    private fun baseColor(mood: Mood): Int = when (mood) {
        Mood.CALM -> CALM_COLOR
        Mood.UNEASY -> UNEASY_COLOR
        Mood.SICK -> SICK_COLOR
    }

    private fun mix(a: Int, b: Int, f: Float): Int {
        val g = 1f - f
        return Color.argb(
            255,
            (Color.red(a) * g + Color.red(b) * f).toInt(),
            (Color.green(a) * g + Color.green(b) * f).toInt(),
            (Color.blue(a) * g + Color.blue(b) * f).toInt(),
        )
    }

    private fun shaders(size: Float, mood: Mood): Shaders {
        val i = mood.ordinal
        val hit = cache[i]
        if (hit != null && cacheSize[i] == size) return hit
        val base = baseColor(mood)
        val r = size * DISC
        val disc = LinearGradient(
            0f, -r, 0f, r,
            intArrayOf(mix(base, Color.WHITE, 0.16f), base, mix(base, Color.BLACK, 0.10f)),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        val shadow = RadialGradient(
            0f, size * 0.025f, size * RING,
            intArrayOf(0x38000000, 0x38000000, 0x00000000),
            floatArrayOf(0f, 0.86f, 1f),
            Shader.TileMode.CLAMP,
        )
        val s = Shaders(disc, shadow)
        cache[i] = s
        cacheSize[i] = size
        return s
    }

    /** 0..1 intensity inside the current mood. */
    private fun intensity(mood: Mood, level: Float): Float = when (mood) {
        Mood.CALM -> level / 0.6f
        Mood.UNEASY -> (level - 0.5f) / 0.5f
        Mood.SICK -> (level - 1f) / 0.5f
    }.coerceIn(0f, 1f)

    override fun draw(c: Canvas, cx: Float, cy: Float, size: Float, mood: Mood, level: Float, t: Float) {
        if (size <= 0f) return
        val k = intensity(mood, level.coerceIn(0f, 1.5f))
        val base = baseColor(mood)
        val sh = shaders(size, mood)
        val r = size * DISC

        c.save()
        c.translate(cx, cy)

        // soft drop shadow (gradient, no blur filter)
        shadowPaint.shader = sh.shadow
        c.drawCircle(0f, size * 0.025f, size * RING, shadowPaint)

        // breathing outer ring for UNEASY / SICK
        if (mood != Mood.CALM) {
            val period = if (mood == Mood.SICK) 2.6f else 3.6f
            val ph = 0.5f + 0.5f * sin(TWO_PI * t / period)
            val amp = if (mood == Mood.SICK) 0.03f + 0.01f * k else 0.02f + 0.005f * k
            ringPaint.color = base
            ringPaint.alpha = ((0.15f + 0.20f * ph) * 255f).toInt()
            ringPaint.strokeWidth = size * RING_STROKE
            c.drawCircle(0f, 0f, size * RING * (1f + amp * ph), ringPaint)
        }

        // disc
        discPaint.shader = sh.disc
        c.drawCircle(0f, 0f, r, discPaint)
        val tintAlpha = (k * 30f).toInt()
        if (tintAlpha > 0) {
            tintPaint.alpha = tintAlpha
            c.drawCircle(0f, 0f, r, tintPaint)
        }

        // subtle top-left gloss arc
        glossPaint.strokeWidth = r * 0.06f
        rect.set(-r * 0.80f, -r * 0.80f, r * 0.80f, r * 0.80f)
        c.drawArc(rect, 200f, 60f, false, glossPaint)

        // face
        if (mood == Mood.SICK) {
            c.rotate(sin(TWO_PI * t / 3.4f) * (2f + 2f * k))
        }
        linePaint.strokeWidth = r * 0.10f
        path.reset()
        when (mood) {
            Mood.CALM -> {
                val er = r * 0.075f
                c.drawCircle(-r * 0.30f, -r * 0.14f, er, dotPaint)
                c.drawCircle(r * 0.30f, -r * 0.14f, er, dotPaint)
                val y = r * 0.18f
                val depth = r * (0.20f - 0.08f * k)
                path.moveTo(-r * 0.32f, y)
                path.quadTo(0f, y + depth * 2f, r * 0.32f, y)
                c.drawPath(path, linePaint)
            }
            Mood.UNEASY -> {
                val er = r * 0.07f
                c.drawCircle(-r * 0.30f, -r * 0.10f, er, dotPaint)
                c.drawCircle(r * 0.30f, -r * 0.10f, er, dotPaint)
                // flat mouth, tilting slightly as unease grows
                path.moveTo(-r * 0.24f, r * 0.27f)
                path.lineTo(r * 0.24f, r * 0.27f - r * 0.06f * k)
                c.drawPath(path, linePaint)
                // small wavy "dizzy" line beside the head (upper right)
                path.reset()
                val x0 = r * 0.22f
                val y0 = -r * 0.56f
                val a = r * (0.07f + 0.04f * k)
                val w = r * 0.09f
                path.moveTo(x0, y0)
                path.quadTo(x0 + w, y0 - a, x0 + 2f * w, y0)
                path.quadTo(x0 + 3f * w, y0 + a, x0 + 4f * w, y0)
                linePaint.strokeWidth = r * 0.07f
                c.drawPath(path, linePaint)
            }
            Mood.SICK -> {
                // X eyes
                val e = r * (0.085f + 0.015f * k)
                val ey = -r * 0.12f
                for (sgn in 0..1) {
                    val ex = if (sgn == 0) -r * 0.30f else r * 0.30f
                    path.moveTo(ex - e, ey - e)
                    path.lineTo(ex + e, ey + e)
                    path.moveTo(ex - e, ey + e)
                    path.lineTo(ex + e, ey - e)
                }
                linePaint.strokeWidth = r * 0.085f
                c.drawPath(path, linePaint)
                // wavy mouth, amplitude breathing gently
                path.reset()
                val y = r * 0.30f
                val a = r * (0.06f + 0.04f * k) * (0.85f + 0.15f * sin(TWO_PI * t / 3f))
                val hw = r * 0.085f // half-wave width; 8 half-waves span 0.68r
                var x = -r * 0.34f
                path.moveTo(x, y)
                for (i in 0 until 4) {
                    path.quadTo(x + hw * 0.5f, y - a, x + hw, y)
                    path.quadTo(x + hw * 1.5f, y + a, x + hw * 2f, y)
                    x += hw * 2f
                }
                linePaint.strokeWidth = r * 0.09f
                c.drawPath(path, linePaint)
            }
        }

        c.restore()
    }
}
