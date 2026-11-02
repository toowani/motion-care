package ai.motioncare.app.theme

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Collaboration example: the original virtual idol "루미 (LUMI)" on a small neon stage.
 * Shows how the icon slot could host a licensed entertainment IP later. The character is
 * fully original (no real person, group, VTuber or existing character).
 *
 * All geometry is in local units of [size], drawn after translating to the centre, so the
 * cached shaders only depend on size + mood.
 */
object IdolTheme : MoodTheme {
    override val id: String = "idol"
    override val title: String = "가상 아이돌 콜라보"
    override val subtitle: String = "콜라보 예시 · 오리지널 가상 아이돌 루미"
    override val animated: Boolean = true

    override fun label(mood: Mood): String = when (mood) {
        Mood.CALM -> "루미랑 신나게"
        Mood.UNEASY -> "루미가 걱정해"
        Mood.SICK -> "같이 쉬어 가요"
    }

    override fun cueLine(left: Boolean): String =
        if (left) "루미가 먼저 알려 줄게요. 곧 왼쪽으로 돌아요!"
        else "루미가 먼저 알려 줄게요. 곧 오른쪽으로 돌아요!"

    // ---------------------------------------------------------------- colours
    private fun c(v: Long): Int = v.toInt()

    private val SKIN = c(0xFFFFE6D8L)
    private val OUTLINE = c(0xB33B2A5AL)
    private val DARK = c(0xFF3B2A5AL)
    private val BROW = c(0xFF4A3070L)
    private val WHITE = c(0xFFFFFFFFL)
    private val STAR_FILL = c(0xFFFFD54AL)
    private val STAR_EDGE = c(0xFFE89A1CL)
    private val TIE = c(0xFFFF6FB5L)
    private val SPARKLE = c(0xFFFFF6C2L)
    private val MOUTH_SMILE = c(0xFFE2557FL)
    private val MOUTH_O = c(0xFFB8436AL)
    private val MOUTH_WAVE = c(0xFF8A3A5EL)
    private val BLUSH = c(0xFFFF8FB3L)
    private val PALE = c(0xFFBFD9F2L)
    private val WATER = c(0xFF5AA9E6L)
    private val SWEAT = c(0xFF8CCBF2L)
    private val ZZ = c(0xFF3E5F96L)
    private val SHADOW = c(0x33402060L)

    private fun ringColor(mood: Mood): Int = when (mood) {
        Mood.CALM -> c(0xFFE85BC2L)
        Mood.UNEASY -> c(0xFFB98BC9L)
        Mood.SICK -> c(0xFF7FAEE0L)
    }

    // ---------------------------------------------------------------- preallocated drawing objects
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val backdropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val conePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val irisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val path = Path()
    private val tailPath = Path()
    private val starPath = Path()
    private val rect = RectF()
    private val rect2 = RectF()

    private const val TAU = 6.2831855f

    // ---------------------------------------------------------------- shader cache (size + mood)
    private class ShaderSet(
        val size: Float,
        val backdrop: Shader,
        val cone: Shader,
        val floor: Shader,
        val hair: Shader,
        val dress: Shader,
        val iris: Shader,
    )

    private val cache = arrayOfNulls<ShaderSet>(3)

    private fun shaders(u: Float, mood: Mood): ShaderSet {
        val i = mood.ordinal
        val hit = cache[i]
        if (hit != null && hit.size == u) return hit
        val built = build(u, mood)
        cache[i] = built
        return built
    }

    private fun build(u: Float, mood: Mood): ShaderSet {
        val back = when (mood) {
            Mood.CALM -> intArrayOf(c(0xFFFFF0FAL), c(0xFFF9A8D9L), c(0xFF9A6BFFL))
            Mood.UNEASY -> intArrayOf(c(0xFFFBF0F6L), c(0xFFE9B3D3L), c(0xFF9B86CFL))
            Mood.SICK -> intArrayOf(c(0xFFF0F8FFL), c(0xFFBFDDF7L), c(0xFF86B3E3L))
        }
        val backdrop = RadialGradient(
            0f, 0.12f * u, 0.50f * u, back, floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        val coneTop = when (mood) {
            Mood.CALM -> c(0xB3FFFFFFL)
            Mood.UNEASY -> c(0x80FFFFFFL)
            Mood.SICK -> c(0x99E3F1FFL)
        }
        val cone = LinearGradient(
            0f, -0.42f * u, 0f, 0.30f * u,
            intArrayOf(coneTop, c(0x00FFFFFFL)), null, Shader.TileMode.CLAMP
        )
        val floorMid = when (mood) {
            Mood.CALM -> c(0x99FF8AD0L)
            Mood.UNEASY -> c(0x88E3A6CCL)
            Mood.SICK -> c(0x9990C2EEL)
        }
        val floor = RadialGradient(
            0f, 0.31f * u, 0.27f * u,
            intArrayOf(c(0xEEFFFFFFL), floorMid, c(0x00FFFFFFL)),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
        )
        val hair = LinearGradient(
            0f, -0.30f * u, 0f, 0.24f * u,
            intArrayOf(c(0xFF3DD6C6L), c(0xFF6C8CF5L), c(0xFF9B5CF0L)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        val dress = LinearGradient(
            0f, 0.10f * u, 0f, 0.29f * u,
            intArrayOf(c(0xFF8E6CF2L), c(0xFFFF7DBFL)), null, Shader.TileMode.CLAMP
        )
        val ey = EYE_Y * u
        val iris = LinearGradient(
            0f, ey - 0.042f * u, 0f, ey + 0.042f * u,
            intArrayOf(c(0xFF2A1856L), c(0xFF5B3FA8L), c(0xFF41D3C8L)),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        return ShaderSet(u, backdrop, cone, floor, hair, dress, iris)
    }

    // ---------------------------------------------------------------- layout (fractions of size)
    private const val HEAD_Y = -0.08f
    private const val EYE_Y = HEAD_Y + 0.03f
    private const val EYE_X = 0.075f
    private const val MOUTH_Y = HEAD_Y + 0.105f

    // ---------------------------------------------------------------- draw
    override fun draw(c: Canvas, cx: Float, cy: Float, size: Float, mood: Mood, level: Float, t: Float) {
        val u = size
        if (u <= 0f) return
        val sh = shaders(u, mood)
        // intensity inside the current mood, 0..1
        val k = when (mood) {
            Mood.CALM -> (level / 0.5f).coerceIn(0f, 1f)
            Mood.UNEASY -> ((level - 0.4f) / 0.6f).coerceIn(0f, 1f)
            Mood.SICK -> ((level - 1f) / 0.5f).coerceIn(0f, 1f)
        }

        c.save()
        c.translate(cx, cy)

        drawStage(c, u, mood, sh)

        // ---- character (bobs / sways as a whole)
        c.save()
        when (mood) {
            Mood.CALM -> c.translate(0f, sin(t * TAU / 2.0f) * 0.012f * u)
            Mood.UNEASY -> c.translate(0f, sin(t * TAU / 2.6f) * 0.006f * u)
            Mood.SICK -> {
                c.rotate(sin(t * TAU / 3.2f) * (2.5f + 3f * k), 0f, 0.31f * u)
                c.translate(0f, 0.008f * u)
            }
        }
        drawCharacter(c, u, mood, k, t, sh)
        c.restore()

        // ---- ambient icons in front of everything (never hidden by the character)
        when (mood) {
            Mood.CALM -> drawSparkles(c, u, t, 4, 1f - 0.25f * k, 2.4f, 1f)
            Mood.UNEASY -> drawSparkles(c, u, t, 2, 0.5f - 0.15f * k, 3.2f, 0.8f)
            Mood.SICK -> drawRest(c, u, t)
        }

        c.restore()
    }

    private fun drawStage(c: Canvas, u: Float, mood: Mood, sh: ShaderSet) {
        // neon stage disc
        backdropPaint.shader = sh.backdrop
        c.drawCircle(0f, 0f, 0.44f * u, backdropPaint)
        stroke.color = ringColor(mood)
        stroke.strokeWidth = 0.014f * u
        c.drawCircle(0f, 0f, 0.443f * u, stroke)

        // soft spotlight cone from the top
        path.reset()
        path.moveTo(-0.05f * u, -0.42f * u)
        path.lineTo(0.05f * u, -0.42f * u)
        path.lineTo(0.27f * u, 0.30f * u)
        path.lineTo(-0.27f * u, 0.30f * u)
        path.close()
        conePaint.shader = sh.cone
        c.drawPath(path, conePaint)

        // lit stage floor + neon rim
        rect.set(-0.27f * u, 0.245f * u, 0.27f * u, 0.375f * u)
        floorPaint.shader = sh.floor
        c.drawOval(rect, floorPaint)
        stroke.color = ringColor(mood)
        stroke.alpha = 200
        stroke.strokeWidth = 0.010f * u
        c.drawOval(rect, stroke)

        // soft contact shadow (translucent ellipse instead of blur)
        fill.color = SHADOW
        rect.set(-0.12f * u, 0.295f * u, 0.12f * u, 0.335f * u)
        c.drawOval(rect, fill)
    }

    private fun drawCharacter(c: Canvas, u: Float, mood: Mood, k: Float, t: Float, sh: ShaderSet) {
        val hy = HEAD_Y * u
        hairPaint.shader = sh.hair

        // ---- twin tails (behind everything), gentle sway at the tips
        val sway = sin(t * TAU / 2.8f) * 0.010f * u
        tailPath.reset()
        tailPath.moveTo(0.16f * u, hy - 0.10f * u)
        tailPath.cubicTo(0.30f * u, hy - 0.08f * u, 0.32f * u + sway, hy + 0.14f * u, 0.26f * u + sway, hy + 0.26f * u)
        tailPath.quadTo(0.24f * u + sway, hy + 0.30f * u, 0.22f * u + sway, hy + 0.25f * u)
        tailPath.cubicTo(0.24f * u, hy + 0.12f * u, 0.19f * u, hy + 0.02f * u, 0.15f * u, hy - 0.02f * u)
        tailPath.close()
        stroke.color = OUTLINE
        stroke.strokeWidth = 0.007f * u
        c.drawPath(tailPath, hairPaint)
        c.drawPath(tailPath, stroke)
        c.save()
        c.scale(-1f, 1f)
        c.drawPath(tailPath, hairPaint)
        c.drawPath(tailPath, stroke)
        c.restore()

        // ---- legs (white boots)
        fill.color = WHITE
        stroke.color = OUTLINE
        stroke.strokeWidth = 0.006f * u
        rect.set(-0.055f * u, 0.255f * u, -0.015f * u, 0.315f * u)
        c.drawRoundRect(rect, 0.015f * u, 0.015f * u, fill)
        c.drawRoundRect(rect, 0.015f * u, 0.015f * u, stroke)
        rect.set(0.015f * u, 0.255f * u, 0.055f * u, 0.315f * u)
        c.drawRoundRect(rect, 0.015f * u, 0.015f * u, fill)
        c.drawRoundRect(rect, 0.015f * u, 0.015f * u, stroke)

        // ---- arms down (the right one is raised later when UNEASY)
        val handY = if (mood == Mood.SICK) 0.235f else 0.22f
        val handX = if (mood == Mood.SICK) 0.10f else 0.12f
        drawArm(c, u, -0.06f, 0.13f, -handX, handY)
        if (mood != Mood.UNEASY) drawArm(c, u, 0.06f, 0.13f, handX, handY)

        // ---- dress
        path.reset()
        path.moveTo(-0.06f * u, 0.10f * u)
        path.lineTo(0.06f * u, 0.10f * u)
        path.lineTo(0.13f * u, 0.27f * u)
        path.quadTo(0f, 0.30f * u, -0.13f * u, 0.27f * u)
        path.close()
        dressPaint.shader = sh.dress
        c.drawPath(path, dressPaint)
        stroke.color = OUTLINE
        stroke.strokeWidth = 0.007f * u
        c.drawPath(path, stroke)
        // skirt frill
        path.reset()
        path.moveTo(-0.12f * u, 0.262f * u)
        path.quadTo(0f, 0.29f * u, 0.12f * u, 0.262f * u)
        stroke.color = WHITE
        stroke.strokeWidth = 0.012f * u
        c.drawPath(path, stroke)
        // star badge
        buildStar(starPath, 0f, 0.195f * u, 0.028f * u, 0.012f * u, 5, 0f)
        fill.color = STAR_FILL
        c.drawPath(starPath, fill)

        // ---- head: back hair, face
        c.drawCircle(0f, hy, 0.215f * u, hairPaint)
        stroke.color = OUTLINE
        stroke.strokeWidth = 0.007f * u
        c.drawCircle(0f, hy, 0.215f * u, stroke)
        fill.color = SKIN
        c.drawCircle(0f, hy + 0.012f * u, 0.182f * u, fill)
        if (mood == Mood.SICK) {
            fill.color = PALE
            fill.alpha = (40 + 60 * k).toInt()
            c.drawCircle(0f, hy + 0.012f * u, 0.182f * u, fill)
        }
        // collar (on top of the face's chin edge)
        path.reset()
        path.moveTo(-0.045f * u, 0.103f * u)
        path.lineTo(0f, 0.145f * u)
        path.lineTo(0.045f * u, 0.103f * u)
        path.close()
        fill.color = WHITE
        c.drawPath(path, fill)

        // ---- blush
        val blushY = hy + 0.085f * u
        when (mood) {
            Mood.CALM -> { fill.color = BLUSH; fill.alpha = (110 + 60 * k).toInt() }
            Mood.UNEASY -> { fill.color = BLUSH; fill.alpha = 70 }
            Mood.SICK -> { fill.color = PALE; fill.alpha = (90 + 80 * k).toInt() }
        }
        rect.set(-0.150f * u, blushY - 0.018f * u, -0.080f * u, blushY + 0.018f * u)
        c.drawOval(rect, fill)
        rect.set(0.080f * u, blushY - 0.018f * u, 0.150f * u, blushY + 0.018f * u)
        c.drawOval(rect, fill)

        // ---- bangs
        path.reset()
        path.moveTo(-0.205f * u, hy + 0.04f * u)
        path.cubicTo(-0.21f * u, hy - 0.30f * u, 0.21f * u, hy - 0.30f * u, 0.205f * u, hy + 0.04f * u)
        path.quadTo(0.16f * u, hy - 0.06f * u, 0.10f * u, hy - 0.045f * u)
        path.quadTo(0.05f * u, hy - 0.11f * u, 0f, hy - 0.055f * u)
        path.quadTo(-0.06f * u, hy - 0.115f * u, -0.11f * u, hy - 0.045f * u)
        path.quadTo(-0.17f * u, hy - 0.07f * u, -0.205f * u, hy + 0.04f * u)
        path.close()
        c.drawPath(path, hairPaint)
        stroke.color = OUTLINE
        stroke.strokeWidth = 0.006f * u
        c.drawPath(path, stroke)

        // ---- headset: band, ear pad, mic boom
        stroke.color = DARK
        stroke.strokeWidth = 0.013f * u
        rect.set(-0.205f * u, hy - 0.225f * u, 0.205f * u, hy + 0.185f * u)
        c.drawArc(rect, 195f, 150f, false, stroke)
        fill.color = DARK
        rect.set(-0.215f * u, hy - 0.015f * u, -0.170f * u, hy + 0.055f * u)
        c.drawOval(rect, fill)
        path.reset()
        path.moveTo(-0.185f * u, hy + 0.045f * u)
        path.quadTo(-0.15f * u, hy + 0.13f * u, -0.068f * u, hy + 0.115f * u)
        stroke.strokeWidth = 0.009f * u
        c.drawPath(path, stroke)
        fill.color = TIE
        c.drawCircle(-0.062f * u, hy + 0.114f * u, 0.014f * u, fill)

        // ---- hair ties + star hairpin
        fill.color = TIE
        c.drawCircle(0.165f * u, hy - 0.085f * u, 0.028f * u, fill)
        c.drawCircle(-0.165f * u, hy - 0.085f * u, 0.028f * u, fill)
        buildStar(starPath, 0.12f * u, hy - 0.165f * u, 0.044f * u, 0.019f * u, 5, 12f)
        fill.color = STAR_FILL
        c.drawPath(starPath, fill)
        stroke.color = STAR_EDGE
        stroke.strokeWidth = 0.006f * u
        c.drawPath(starPath, stroke)

        // ---- eyes + brows
        val ey = EYE_Y * u
        val lx = -EYE_X * u
        val rx = EYE_X * u
        when (mood) {
            Mood.CALM -> {
                drawOpenEye(c, u, lx, ey, 1f, sh)
                // cheerful wink
                path.reset()
                path.moveTo(rx - 0.034f * u, ey + 0.006f * u)
                path.quadTo(rx, ey - 0.036f * u, rx + 0.034f * u, ey + 0.006f * u)
                stroke.color = DARK
                stroke.strokeWidth = 0.013f * u
                c.drawPath(path, stroke)
            }
            Mood.UNEASY -> {
                drawOpenEye(c, u, lx, ey, 0.92f, sh)
                drawOpenEye(c, u, rx, ey, 0.92f, sh)
                drawBrows(c, u, ey, 0.020f + 0.014f * k)
            }
            Mood.SICK -> {
                val phase = t * TAU / 3f
                drawSwirlEye(c, u, lx, ey, phase)
                drawSwirlEye(c, u, rx, ey, phase)
                drawBrows(c, u, ey, 0.012f)
            }
        }

        // ---- mouth
        val my = MOUTH_Y * u
        when (mood) {
            Mood.CALM -> {
                path.reset()
                path.moveTo(-0.040f * u, my)
                path.quadTo(0f, my + 0.010f * u, 0.040f * u, my)
                path.quadTo(0f, my + 0.068f * u, -0.040f * u, my)
                path.close()
                fill.color = MOUTH_SMILE
                c.drawPath(path, fill)
            }
            Mood.UNEASY -> {
                val r = (0.015f + 0.004f * k) * u
                rect.set(-r, my - 0.002f * u, r, my + 0.002f * u + 2f * r)
                fill.color = MOUTH_O
                c.drawOval(rect, fill)
            }
            Mood.SICK -> {
                val a = (0.010f + 0.006f * k) * u * (0.85f + 0.15f * sin(t * TAU / 2.5f))
                val y0 = my + 0.012f * u
                path.reset()
                path.moveTo(-0.045f * u, y0)
                path.quadTo(-0.034f * u, y0 - a, -0.0225f * u, y0)
                path.quadTo(-0.011f * u, y0 + a, 0f, y0)
                path.quadTo(0.011f * u, y0 - a, 0.0225f * u, y0)
                path.quadTo(0.034f * u, y0 + a, 0.045f * u, y0)
                stroke.color = MOUTH_WAVE
                stroke.strokeWidth = 0.010f * u
                c.drawPath(path, stroke)
            }
        }

        // ---- UNEASY: raised, gently waving hand + sweat drop
        if (mood == Mood.UNEASY) {
            val wave = sin(t * TAU / 1.8f) * (0.012f + 0.010f * k)
            drawArm(c, u, 0.08f, 0.14f, 0.215f + wave, 0.0f)
            drawDrop(c, -0.235f * u, hy - 0.140f * u, 0.022f * u, SWEAT)
        }
    }

    private fun drawArm(c: Canvas, u: Float, sx: Float, sy: Float, hx: Float, hy: Float) {
        stroke.color = OUTLINE
        stroke.strokeWidth = 0.046f * u
        c.drawLine(sx * u, sy * u, hx * u, hy * u, stroke)
        stroke.color = SKIN
        stroke.strokeWidth = 0.034f * u
        c.drawLine(sx * u, sy * u, hx * u, hy * u, stroke)
        fill.color = SKIN
        c.drawCircle(hx * u, hy * u, 0.026f * u, fill)
        stroke.color = OUTLINE
        stroke.strokeWidth = 0.006f * u
        c.drawCircle(hx * u, hy * u, 0.026f * u, stroke)
    }

    private fun drawOpenEye(c: Canvas, u: Float, ex: Float, ey: Float, s: Float, sh: ShaderSet) {
        val w = 0.032f * u * s
        val h = 0.042f * u * s
        rect2.set(ex - w, ey - h, ex + w, ey + h)
        irisPaint.shader = sh.iris
        c.drawOval(rect2, irisPaint)
        // upper lash line
        stroke.color = DARK
        stroke.strokeWidth = 0.012f * u
        c.drawArc(rect2, 200f, 140f, false, stroke)
        // sparkly highlights
        fill.color = WHITE
        c.drawCircle(ex - 0.010f * u * s, ey - 0.017f * u * s, 0.012f * u * s, fill)
        c.drawCircle(ex + 0.012f * u * s, ey + 0.017f * u * s, 0.006f * u * s, fill)
    }

    private fun drawSwirlEye(c: Canvas, u: Float, ex: Float, ey: Float, phase: Float) {
        fill.color = WHITE
        fill.alpha = 230
        rect2.set(ex - 0.036f * u, ey - 0.044f * u, ex + 0.036f * u, ey + 0.044f * u)
        c.drawOval(rect2, fill)
        path.reset()
        val n = 28
        val turns = 2.25f
        for (i in 0..n) {
            val f = i / n.toFloat()
            val ang = phase + f * turns * TAU
            val r = (0.004f + 0.030f * f) * u
            val x = ex + cos(ang) * r
            val y = ey + sin(ang) * r * 1.15f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        stroke.color = DARK
        stroke.strokeWidth = 0.009f * u
        c.drawPath(path, stroke)
    }

    /** Worried brows: inner ends raised by [lift] (fraction of size). */
    private fun drawBrows(c: Canvas, u: Float, ey: Float, lift: Float) {
        stroke.color = BROW
        stroke.strokeWidth = 0.011f * u
        val outerY = ey - 0.062f * u
        val innerY = ey - 0.062f * u - lift * u
        c.drawLine(-0.112f * u, outerY, -0.045f * u, innerY, stroke)
        c.drawLine(0.112f * u, outerY, 0.045f * u, innerY, stroke)
    }

    private fun drawSparkles(c: Canvas, u: Float, t: Float, count: Int, alpha: Float, period: Float, scale: Float) {
        for (j in 0 until count) {
            // UNEASY keeps only two of the four positions (0 and 3)
            val i = if (count == 2) (if (j == 0) 0 else 3) else j
            val x: Float
            val y: Float
            val r: Float
            when (i) {
                0 -> { x = -0.31f; y = -0.22f; r = 0.035f }
                1 -> { x = 0.31f; y = -0.24f; r = 0.032f }
                2 -> { x = -0.36f; y = 0.06f; r = 0.028f }
                else -> { x = 0.36f; y = 0.10f; r = 0.028f }
            }
            val tw = 0.5f + 0.5f * sin(t * TAU / period + i * 1.7f)
            val s = (0.55f + 0.45f * tw) * scale
            buildStar(starPath, x * u, y * u, r * u * s, r * u * s * 0.32f, 4, 0f)
            fill.color = SPARKLE
            fill.alpha = (255f * alpha * (0.6f + 0.4f * tw)).toInt().coerceIn(0, 255)
            c.drawPath(starPath, fill)
        }
    }

    /** SICK: a water drop (have a sip, rest) and a small floating "zZ". */
    private fun drawRest(c: Canvas, u: Float, t: Float) {
        val bob = sin(t * TAU / 3f) * 0.008f * u
        drawDrop(c, -0.30f * u, -0.20f * u + bob, 0.040f * u, WATER)

        stroke.color = ZZ
        stroke.strokeWidth = 0.011f * u
        drawZ(c, 0.30f * u, -0.215f * u - bob, 0.030f * u)
        stroke.strokeWidth = 0.008f * u
        // kept clear of the star hairpin even at the far end of the SICK sway; corner stays < 0.45·size
        drawZ(c, 0.245f * u, -0.325f * u - bob, 0.017f * u)
    }

    private fun drawZ(c: Canvas, x: Float, y: Float, w: Float) {
        path.reset()
        path.moveTo(x - w, y - w)
        path.lineTo(x + w, y - w)
        path.lineTo(x - w, y + w)
        path.lineTo(x + w, y + w)
        c.drawPath(path, stroke)
    }

    private fun drawDrop(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        path.reset()
        path.moveTo(x, y - 1.7f * r)
        path.cubicTo(x + 0.4f * r, y - 1.0f * r, x + r, y - 0.5f * r, x + r, y)
        rect2.set(x - r, y - r, x + r, y + r)
        path.arcTo(rect2, 0f, 180f, false)
        path.cubicTo(x - r, y - 0.5f * r, x - 0.4f * r, y - 1.0f * r, x, y - 1.7f * r)
        path.close()
        fill.color = color
        c.drawPath(path, fill)
        stroke.color = OUTLINE
        stroke.strokeWidth = r * 0.14f // outline proportional to the drop
        c.drawPath(path, stroke)
        fill.color = WHITE
        fill.alpha = 200
        rect2.set(x - 0.55f * r, y - 0.55f * r, x - 0.15f * r, y + 0.05f * r)
        c.drawOval(rect2, fill)
    }

    /** Star with [points] tips into [p]; [rotDeg] rotates it. */
    private fun buildStar(p: Path, x: Float, y: Float, ro: Float, ri: Float, points: Int, rotDeg: Float) {
        p.reset()
        val rot = rotDeg / 180f * PI.toFloat()
        val step = PI.toFloat() / points
        for (i in 0 until points * 2) {
            val ang = -PI.toFloat() / 2f + rot + i * step
            val r = if (i % 2 == 0) ro else ri
            val px = x + cos(ang) * r
            val py = y + sin(ang) * r
            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
        }
        p.close()
    }
}
