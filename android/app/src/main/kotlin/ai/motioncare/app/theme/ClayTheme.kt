package ai.motioncare.app.theme

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * "말랑 3D 친구": 몽글이, an original soft clay / mochi blob drawn with gradients to look 3D.
 * Light comes from the upper left; a translucent ellipse fakes the ground shadow.
 *
 * Everything below the shadow is drawn in a local space whose origin is the ground point under
 * the body (y grows downward, the body spans y in [-h, 0]), so squash, stretch and tilt are plain
 * canvas transforms and the body shader can stay cached.
 */
object ClayTheme : MoodTheme {
    override val id = "clay"
    override val title = "말랑 3D 친구"
    override val subtitle = "말랑한 찰흙 캐릭터 몽글이"
    override val animated = true

    override fun label(mood: Mood): String = when (mood) {
        Mood.CALM -> "몽글몽글 좋아"
        Mood.UNEASY -> "살짝 울렁"
        Mood.SICK -> "어질어질"
    }

    override fun cueLine(left: Boolean): String =
        if (left) "몽글이가 알려 줄게. 곧 왼쪽으로 돌아요!"
        else "몽글이가 알려 줄게. 곧 오른쪽으로 돌아요!"

    private const val TWO_PI = 6.2831855f
    private val SIDES = intArrayOf(-1, 1)
    private const val TINT_STEPS = 6

    // ---- colours: light (highlight), mid, edge (shade) per mood; "strong" mid used at high level
    private val LIGHT = intArrayOf(0xFFEFFFF7.toInt(), 0xFFFFF8E2.toInt(), 0xFFF4F2FA.toInt())
    private val MID = intArrayOf(0xFFA4E6C8.toInt(), 0xFFFFD58E.toInt(), 0xFFCACBE0.toInt())
    private val MID_STRONG = intArrayOf(0xFF8EDDB8.toInt(), 0xFFFFB98A.toInt(), 0xFFB9D2B4.toInt())
    private val EDGE = intArrayOf(0xFF5DBE98.toInt(), 0xFFEE9F62.toInt(), 0xFF8FA597.toInt())
    private val RIM = intArrayOf(0x664A9C7C, 0x66C9783E, 0x66707F78)

    private const val INK = 0xFF4A3B47.toInt()
    private const val MOUTH = 0xFF6B3A48.toInt()
    private const val TONGUE = 0xFFFF8FA0.toInt()

    // ---- preallocated drawing objects
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.WHITE }
    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val inkStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = INK
    }
    private val inkFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = INK }
    private val mouthFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = MOUTH }
    private val mouthStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = MOUTH
    }
    private val tonguePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = TONGUE }
    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.WHITE }
    private val dropFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0xFFA6DCFF.toInt() }
    private val dropStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; color = 0xFF5AA6DC.toInt()
    }
    private val gloomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = 0x887A6FB0.toInt()
    }

    private val bodyPath = Path()
    private val linePath = Path()
    private val dropPath = Path()
    private val rect = RectF()

    // ---- shader cache: one body shader per mood keyed by size + tint step, one shadow keyed by size
    private val bodyShaders = arrayOfNulls<Shader>(3)
    private val bodyKeySize = FloatArray(3) { -1f }
    private val bodyKeyTint = IntArray(3) { -1 }
    private var shadowShader: Shader? = null
    private var shadowKeySize = -1f
    private val bodyStops = floatArrayOf(0f, 0.5f, 1f)
    private val shadowStops = floatArrayOf(0f, 0.55f, 1f)

    /**
     * 0..1 progress inside the current mood, from the 0..1.5 level (index / thetaHigh). With the
     * default params caution starts at 0.6 (0.9 / 1.5) and, with the 0.8 exit hysteresis, CAUTION
     * can last down to ~0.48 and HIGH down to 0.8.
     */
    private fun intensity(mood: Mood, level: Float): Float = when (mood) {
        Mood.CALM -> level / 0.6f
        Mood.UNEASY -> (level - 0.5f) / 0.5f
        Mood.SICK -> (level - 0.8f) / 0.7f
    }.coerceIn(0f, 1f)

    private fun mix(a: Int, b: Int, f: Float): Int {
        val g = f.coerceIn(0f, 1f)
        val aa = (a ushr 24) and 0xFF; val ba = (b ushr 24) and 0xFF
        val ar = (a shr 16) and 0xFF; val br = (b shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF; val bg = (b shr 8) and 0xFF
        val ab = a and 0xFF; val bb = b and 0xFF
        return Color.argb(
            (aa + (ba - aa) * g).roundToInt(),
            (ar + (br - ar) * g).roundToInt(),
            (ag + (bg - ag) * g).roundToInt(),
            (ab + (bb - ab) * g).roundToInt(),
        )
    }

    private fun bodyShader(mood: Mood, size: Float, k: Float, w: Float, h: Float): Shader {
        val i = mood.ordinal
        val tint = (k * TINT_STEPS).roundToInt()
        val cached = bodyShaders[i]
        if (cached != null && bodyKeySize[i] == size && bodyKeyTint[i] == tint) return cached
        val f = tint / TINT_STEPS.toFloat()
        val colors = intArrayOf(LIGHT[i], mix(MID[i], MID_STRONG[i], f), EDGE[i])
        val s = RadialGradient(-0.38f * w, -0.78f * h, 1.35f * h, colors, bodyStops, Shader.TileMode.CLAMP)
        bodyShaders[i] = s
        bodyKeySize[i] = size
        bodyKeyTint[i] = tint
        return s
    }

    private fun shadowShader(size: Float, radius: Float): Shader {
        val cached = shadowShader
        if (cached != null && shadowKeySize == size) return cached
        val s = RadialGradient(
            0f, 0f, radius,
            intArrayOf(0x40303040, 0x22303040, 0x00303040),
            shadowStops, Shader.TileMode.CLAMP,
        )
        shadowShader = s
        shadowKeySize = size
        return s
    }

    override fun draw(c: Canvas, cx: Float, cy: Float, size: Float, mood: Mood, level: Float, t: Float) {
        if (size <= 0f) return
        val s = size
        val tt = if (animated) t else 0f
        val k = intensity(mood, level)
        val w = 0.30f * s          // half width at the widest point
        val h = 0.58f * s          // full height
        val groundY = cy + 0.32f * s

        // ---- posture per mood
        val breath = sin(TWO_PI * tt / 2.5f)
        var baseSx = 1f
        var baseSy = 1f
        var tilt = 0f
        val breathAmp: Float
        when (mood) {
            Mood.CALM -> breathAmp = 0.03f
            Mood.UNEASY -> {
                breathAmp = 0.022f
                tilt = (3f + 5f * k) * sin(TWO_PI * tt / 3f)
            }
            Mood.SICK -> {
                breathAmp = 0.015f
                baseSx = 1.06f + 0.02f * k
                baseSy = 0.90f - 0.03f * k
                tilt = (3f + 2.5f * k) * sin(TWO_PI * tt / 3.6f)
            }
        }
        val sy = baseSy * (1f + breathAmp * breath)
        val sx = baseSx * (1f - breathAmp * 0.8f * breath)

        // ---- ground shadow (shrinks a little as the body stretches up)
        val shadowR = 0.29f * s
        shadowPaint.shader = shadowShader(s, shadowR)
        c.save()
        c.translate(cx, groundY + 0.004f * s)
        c.scale(1f, 0.18f)
        c.drawCircle(0f, 0f, shadowR * (sx - 0.02f * breath), shadowPaint)
        c.restore()

        // ---- body space
        c.save()
        c.translate(cx, groundY)
        c.rotate(tilt)
        c.scale(sx, sy)

        val i = mood.ordinal
        bodyPaint.shader = bodyShader(mood, s, k, w, h)
        rimPaint.color = RIM[i]
        rimPaint.strokeWidth = 0.012f * s

        drawArms(c, mood, w, h, tt)
        buildBody(w, h)
        c.drawPath(bodyPath, bodyPaint)
        c.drawPath(bodyPath, rimPaint)

        drawGloss(c, mood, w, h, s)
        drawBlush(c, mood, k, w, h)
        drawFace(c, mood, k, w, h, s, tt)

        c.restore()
    }

    private fun buildBody(w: Float, h: Float) {
        val p = bodyPath
        p.reset()
        p.moveTo(0f, 0f)
        p.cubicTo(w * 0.78f, 0f, w, -h * 0.05f, w, -h * 0.35f)
        p.cubicTo(w, -h * 0.80f, w * 0.62f, -h, 0f, -h)
        p.cubicTo(-w * 0.62f, -h, -w, -h * 0.80f, -w, -h * 0.35f)
        p.cubicTo(-w, -h * 0.05f, -w * 0.78f, 0f, 0f, 0f)
        p.close()
    }

    /** Tiny stubby arms, drawn behind the body so only the outer half peeks out. */
    private fun drawArms(c: Canvas, mood: Mood, w: Float, h: Float, tt: Float) {
        val ax: Float
        val ay: Float
        val angle: Float
        when (mood) {
            Mood.CALM -> { ax = 0.98f * w; ay = -0.42f * h; angle = -35f + 8f * sin(TWO_PI * tt / 2.5f) }
            Mood.UNEASY -> { ax = 0.97f * w; ay = -0.32f * h; angle = 15f }
            Mood.SICK -> { ax = 0.95f * w; ay = -0.22f * h; angle = 60f }
        }
        rect.set(-0.17f * w, -0.11f * w, 0.17f * w, 0.11f * w)
        for (sign in SIDES) {
            c.save()
            c.translate(sign * ax, ay)
            c.rotate(sign * angle)
            c.translate(sign * 0.05f * w, 0f)
            c.drawOval(rect, bodyPaint)
            c.drawOval(rect, rimPaint)
            c.restore()
        }
    }

    private fun drawGloss(c: Canvas, mood: Mood, w: Float, h: Float, s: Float) {
        val a = if (mood == Mood.SICK) 0.55f else 1f
        c.save()
        c.translate(-0.40f * w, -0.78f * h)
        c.rotate(-28f)
        glossPaint.alpha = (120 * a).roundToInt()
        rect.set(-0.20f * w, -0.085f * w, 0.20f * w, 0.085f * w)
        c.drawOval(rect, glossPaint)
        glossPaint.alpha = (210 * a).roundToInt()
        rect.set(-0.10f * w, -0.045f * w, 0.06f * w, 0.035f * w)
        c.drawOval(rect, glossPaint)
        glossPaint.alpha = (200 * a).roundToInt()
        c.drawCircle(0.27f * w, 0.02f * w, 0.035f * w + 0.002f * s, glossPaint)
        c.restore()
    }

    private fun drawBlush(c: Canvas, mood: Mood, k: Float, w: Float, h: Float) {
        blushPaint.color = when (mood) {
            Mood.CALM -> mix(0x66FF8FA3, 0x88FF8FA3.toInt(), k)
            Mood.UNEASY -> 0x77FF8A8A
            Mood.SICK -> 0x40E0A0C0
        }
        for (sign in SIDES) {
            val bx = sign * 0.58f * w
            val by = -0.40f * h
            rect.set(bx - 0.14f * w, by - 0.075f * w, bx + 0.14f * w, by + 0.075f * w)
            c.drawOval(rect, blushPaint)
        }
    }

    private fun drawFace(c: Canvas, mood: Mood, k: Float, w: Float, h: Float, s: Float, tt: Float) {
        val ex = 0.36f * w
        val ey = -0.52f * h
        val my = -0.36f * h
        inkStroke.strokeWidth = 0.028f * s
        mouthStroke.strokeWidth = 0.024f * s

        when (mood) {
            Mood.CALM -> {
                // happy closed eyes: ^ ^
                for (sign in SIDES) {
                    val x = sign * ex
                    rect.set(x - 0.11f * w, ey - 0.08f * w, x + 0.11f * w, ey + 0.10f * w)
                    c.drawArc(rect, 205f, 130f, false, inkStroke)
                }
                // small open smile with a tongue
                val mw = 0.13f * w
                val depth = 0.15f * w
                linePath.reset()
                linePath.moveTo(-mw, my)
                linePath.quadTo(0f, my + depth * 2f, mw, my)
                linePath.close()
                c.drawPath(linePath, mouthFill)
                c.save()
                c.clipPath(linePath)
                c.drawCircle(0f, my + depth * 1.25f, 0.075f * w, tonguePaint)
                c.restore()
                mouthStroke.strokeWidth = 0.016f * s
                c.drawPath(linePath, mouthStroke)
            }
            Mood.UNEASY -> {
                // open dot eyes with a slow blink
                val cycle = tt % 3.6f
                val blink = if (cycle > 3.44f) 0.18f else 1f
                for (sign in SIDES) {
                    val x = sign * ex
                    val rx = 0.085f * w
                    val ry = 0.105f * w * blink
                    rect.set(x - rx, ey - ry, x + rx, ey + ry)
                    c.drawOval(rect, inkFill)
                    if (blink == 1f) c.drawCircle(x - 0.03f * w, ey - 0.04f * w, 0.03f * w, whitePaint)
                }
                // worried brows: inner ends raised
                inkStroke.strokeWidth = 0.02f * s
                val inner = -0.70f * h - 0.03f * h * k
                for (sign in SIDES) {
                    c.drawLine(sign * 0.52f * w, -0.645f * h, sign * 0.20f * w, inner, inkStroke)
                }
                // small wobbly mouth
                val amp = 0.035f * w * (1f + 0.4f * sin(TWO_PI * tt / 2f)) * (0.8f + 0.4f * k)
                drawWave(c, 0.12f * w, my + 0.02f * h, amp, 1f, 0f, mouthStroke)
            }
            Mood.SICK -> {
                // spiral eyes turning slowly
                inkStroke.strokeWidth = 0.017f * s
                val rot = TWO_PI * tt / 4f
                val er = 0.15f * w
                for (sign in SIDES) drawSpiral(c, sign * ex, ey, er, sign * rot)
                // gloom lines on the forehead
                gloomPaint.strokeWidth = 0.013f * s
                gloomPaint.alpha = (0x66 + 0x44 * k).roundToInt()
                for (j in -1..1) {
                    val x = j * 0.13f * w
                    val top = -0.90f * h + (if (j == 0) -0.02f * h else 0f)
                    c.drawLine(x, top, x, -0.75f * h, gloomPaint)
                }
                // wavy mouth
                val amp = 0.04f * w * (0.9f + 0.3f * k)
                drawWave(c, 0.20f * w, my + 0.03f * h, amp, 1.5f, TWO_PI * tt / 3f, mouthStroke)
                // sweat drop sliding down gently
                val slide = 0.03f * h * (0.5f + 0.5f * sin(TWO_PI * tt / 3f))
                drawDrop(c, 0.80f * w, -0.80f * h + slide, 0.10f * w + 0.02f * w * k, s)
            }
        }
    }

    /** Sine polyline from -halfW..halfW, [waves] full periods, with a moving [phase]. */
    private fun drawWave(c: Canvas, halfW: Float, y: Float, amp: Float, waves: Float, phase: Float, paint: Paint) {
        val n = 18
        linePath.reset()
        for (j in 0..n) {
            val u = j / n.toFloat()
            val x = -halfW + 2f * halfW * u
            val yy = y + amp * sin(TWO_PI * waves * u + phase)
            if (j == 0) linePath.moveTo(x, yy) else linePath.lineTo(x, yy)
        }
        c.drawPath(linePath, paint)
    }

    private fun drawSpiral(c: Canvas, x: Float, y: Float, r: Float, rot: Float) {
        val n = 30
        val turns = 2.25f
        linePath.reset()
        for (j in 0..n) {
            val u = j / n.toFloat()
            val a = rot + TWO_PI * turns * u
            val rr = r * u
            val px = x + rr * cos(a)
            val py = y + rr * sin(a)
            if (j == 0) linePath.moveTo(px, py) else linePath.lineTo(px, py)
        }
        c.drawPath(linePath, inkStroke)
    }

    private fun drawDrop(c: Canvas, x: Float, y: Float, r: Float, s: Float) {
        val p = dropPath
        p.reset()
        p.moveTo(x, y - 1.7f * r)
        p.cubicTo(x + 0.55f * r, y - 0.95f * r, x + r, y - 0.45f * r, x + r, y)
        rect.set(x - r, y - r, x + r, y + r)
        p.arcTo(rect, 0f, 180f, false)
        p.cubicTo(x - r, y - 0.45f * r, x - 0.55f * r, y - 0.95f * r, x, y - 1.7f * r)
        p.close()
        c.drawPath(p, dropFill)
        dropStroke.strokeWidth = 0.009f * s
        c.drawPath(p, dropStroke)
        glossPaint.alpha = 220
        rect.set(x - 0.55f * r, y - 0.55f * r, x - 0.15f * r, y + 0.05f * r)
        c.drawOval(rect, glossPaint)
    }
}
