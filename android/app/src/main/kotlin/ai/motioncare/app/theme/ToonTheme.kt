package ai.motioncare.app.theme

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin

/**
 * 2D cel-shaded cartoon: 부릉이, a small round car whose windshield is a pair of big eyes.
 * Thick ink outlines, flat fills with one darker shade band, bouncing on a short road.
 * CALM: sky blue, sparkly eyes and a big smile, steady bounce, blinks every 3.5 s.
 * UNEASY: yellow, eyes glancing aside under worried brows, rocking, one sweat drop.
 * SICK: green-tinted, spiral eyes, tongue out, squashed wheels, 3 stars orbiting above.
 */
object ToonTheme : MoodTheme {
    override val id = "toon"
    override val title = "애니메이션 친구"
    override val subtitle = "통통 튀는 꼬마 자동차 부릉이"
    override val animated = true

    override fun label(mood: Mood): String = when (mood) {
        Mood.CALM -> "씽씽 신나요"
        Mood.UNEASY -> "흔들흔들"
        Mood.SICK -> "빙글빙글"
    }

    override fun cueLine(left: Boolean): String? =
        if (left) "부릉이 출동! 곧 왼쪽으로 돌아요." else "부릉이 출동! 곧 오른쪽으로 돌아요."

    // ---- palette ----
    private val INK = 0xFF23233A.toInt()
    private val TIRE = 0xFF34344A.toInt()
    private val HUB = 0xFFDDE1EA.toInt()
    private val GLASS = 0xFFEFF8FF.toInt()
    private val MOUTH = 0xFF8C2B3E.toInt()
    private val TONGUE = 0xFFFF7E9D.toInt()
    private val BLUSH = 0x70FF6F91
    private val ROAD = 0xFF3B4058.toInt()
    private val DASH = 0xFFB9BFD0.toInt()
    private val SWEAT = 0xFF8FD3FF.toInt()
    private val STAR = 0xFFFFD84A.toInt()
    private val HEADLIGHT = 0xFFFFE680.toInt()
    private val TAILLIGHT = 0xFFFF8A65.toInt()

    private val CALM_A = 0xFF5EC8F2.toInt()
    private val CALM_B = 0xFF3FB0EC.toInt()
    private val UNEASY_A = 0xFFFFD84D.toInt()
    private val UNEASY_B = 0xFFFFB938.toInt()
    private val SICK_A = 0xFFBEDD7E.toInt()
    private val SICK_B = 0xFF98C756.toInt()

    // ---- layout (fractions of size, relative to the centre) ----
    private const val OY = 0.03f        // car origin below centre
    private const val CONTACT_Y = 0.242f // where tyres touch the road (global)
    private const val ROAD_Y = 0.252f
    private const val WHEEL_X = 0.165f
    private const val WHEEL_R = 0.072f
    private const val WHEEL_BOTTOM = 0.212f // local to car origin
    private const val EYE_Y = -0.145f
    private val EYE_X = floatArrayOf(-0.075f, 0.055f)
    private const val MOUTH_X = -0.01f

    private val TAU = 6.2831855f
    private val PI = 3.1415927f

    // ---- preallocated drawing objects ----
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val tmp = Path()
    private val work = Path()
    private val rect = RectF()

    /** Body/panel/star shapes for one icon size. Several sizes are on screen at once (home icon
     *  plus the three settings previews), so keep a few slots instead of rebuilding every frame. */
    private class Shapes {
        var size = -1f
        val body = Path()
        val panel = Path()
        val star = Path()
    }
    private val slots = arrayOf(Shapes(), Shapes(), Shapes())
    private var nextSlot = 0
    private var cur = slots[0]
    private val body: Path get() = cur.body
    private val panel: Path get() = cur.panel
    private val star: Path get() = cur.star

    /** Ground-shadow shaders, one per mood, built at a fixed reference radius and scaled by the
     *  canvas, so they never depend on size and are allocated at most once each. */
    private const val SH_R = 100f
    private val shadowShaders = arrayOfNulls<Shader>(3)

    private fun ensure(s: Float, mood: Mood) {
        cur = shapesFor(s)

        val idx = mood.ordinal
        var sh = shadowShaders[idx]
        if (sh == null) {
            val col = when (mood) {
                Mood.CALM -> 0x44305A80
                Mood.UNEASY -> 0x44806020
                Mood.SICK -> 0x44507030
            }
            val clear = col and 0x00FFFFFF
            val half = clear or (((col ushr 24) / 2) shl 24)
            sh = RadialGradient(
                0f, 0f, SH_R,
                intArrayOf(col, half, clear),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            shadowShaders[idx] = sh
        }
        shadowPaint.shader = sh
    }

    private fun shapesFor(s: Float): Shapes {
        for (slot in slots) if (slot.size == s) return slot
        val slot = slots[nextSlot]
        nextSlot = (nextSlot + 1) % slots.size
        build(slot, s)
        return slot
    }

    private fun build(slot: Shapes, s: Float) {
        slot.size = s
        val b = slot.body
        b.reset()
        rect.set(-0.28f * s, -0.045f * s, 0.28f * s, 0.14f * s)
        b.addRoundRect(rect, 0.07f * s, 0.07f * s, Path.Direction.CW)
        tmp.reset()
        rect.set(-0.21f * s, -0.27f * s, 0.19f * s, 0f)
        tmp.addRoundRect(rect, 0.11f * s, 0.11f * s, Path.Direction.CW)
        b.op(tmp, Path.Op.UNION)
        tmp.reset()

        val pn = slot.panel
        pn.reset()
        rect.set(-0.175f * s, -0.237f * s, 0.155f * s, -0.06f * s)
        pn.addRoundRect(rect, 0.075f * s, 0.075f * s, Path.Direction.CW)

        val st = slot.star
        st.reset()
        val outer = 0.032f * s
        val inner = outer * 0.48f
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val a = -PI / 2f + i * PI / 5f
            val x = cos(a) * r
            val y = sin(a) * r
            if (i == 0) st.moveTo(x, y) else st.lineTo(x, y)
        }
        st.close()
    }

    override fun draw(c: Canvas, cx: Float, cy: Float, size: Float, mood: Mood, level: Float, t: Float) {
        val s = size
        if (s <= 1f) return
        ensure(s, mood)

        val k = when (mood) {
            Mood.CALM -> clamp01(level / 0.6f)
            Mood.UNEASY -> clamp01((level - 0.5f) / 0.5f)
            Mood.SICK -> clamp01((level - 0.8f) / 0.7f) // HIGH holds down to ~0.8 (exit hysteresis)
        }
        val bodyColor = when (mood) {
            Mood.CALM -> lerpColor(CALM_A, CALM_B, k)
            Mood.UNEASY -> lerpColor(UNEASY_A, UNEASY_B, k)
            Mood.SICK -> lerpColor(SICK_A, SICK_B, k)
        }
        val shadeColor = darken(bodyColor, 0.8f)

        val bodyDy: Float
        val angle: Float
        val spinPeriod: Float
        when (mood) {
            Mood.CALM -> {
                bodyDy = -0.016f * s * (0.5f - 0.5f * cos(TAU * t / 1.6f))
                angle = 0f
                spinPeriod = 2f
            }
            Mood.UNEASY -> {
                bodyDy = -0.010f * s * (0.5f - 0.5f * cos(TAU * t / 1.5f))
                angle = (3f + 3f * k) * sin(TAU * t / 2.4f)
                spinPeriod = 2.6f
            }
            Mood.SICK -> {
                bodyDy = 0.008f * s + 0.014f * s * sin(TAU * t / 3.2f)
                angle = (5f + 3f * k) * sin(TAU * t / 3.2f + 0.8f)
                spinPeriod = 3.6f
            }
        }

        c.save()
        c.translate(cx, cy)

        // soft ground shadow (gradient, no blur filter)
        c.save()
        c.translate(0f, CONTACT_Y * s)
        c.scale(0.27f * s / SH_R, 0.038f * s / SH_R)
        c.drawCircle(0f, 0f, SH_R, shadowPaint)
        c.restore()

        drawRoad(c, s, t)
        drawSpeedLines(c, s, t, mood, (OY * s) + bodyDy)

        // the car rocks around its contact point on the road
        c.save()
        c.rotate(angle, 0f, CONTACT_Y * s)

        c.save()
        c.translate(0f, OY * s + bodyDy)
        drawBody(c, s, bodyColor, shadeColor)
        when (mood) {
            Mood.CALM -> drawCalmFace(c, s, k, t)
            Mood.UNEASY -> drawUneasyFace(c, s, k, t)
            Mood.SICK -> drawSickFace(c, s, k, t, shadeColor)
        }
        c.restore()

        c.save()
        c.translate(0f, OY * s)
        drawWheels(c, s, mood, k, t, spinPeriod)
        c.restore()

        c.restore() // rotation

        when (mood) {
            Mood.CALM -> drawTwinkles(c, s, t, bodyDy)
            Mood.SICK -> drawStars(c, s, t, bodyDy)
            Mood.UNEASY -> Unit
        }

        c.restore()
    }

    // ---------------------------------------------------------------- scenery

    private fun drawRoad(c: Canvas, s: Float, t: Float) {
        stroke.color = ROAD
        stroke.strokeWidth = 0.022f * s
        c.drawLine(-0.32f * s, ROAD_Y * s, 0.32f * s, ROAD_Y * s, stroke)

        // small dashes drifting backwards under the road, fading at both ends
        stroke.color = DASH
        stroke.strokeWidth = 0.014f * s
        val span = 0.56f
        val y = 0.295f * s
        for (i in 0 until 3) {
            val pos = posMod(i * (span / 3f) - t * 0.1f, span)
            val f = pos / span
            stroke.alpha = (255f * sin(PI * f)).toInt().coerceIn(0, 255)
            val x0 = (-0.30f + pos) * s
            c.drawLine(x0, y, x0 + 0.05f * s, y, stroke)
        }
        stroke.alpha = 255
    }

    private fun drawSpeedLines(c: Canvas, s: Float, t: Float, mood: Mood, originY: Float) {
        stroke.color = INK
        stroke.strokeWidth = 0.018f * s
        val baseAlpha = when (mood) {
            Mood.CALM -> 150
            Mood.UNEASY -> 120
            Mood.SICK -> 85
        }
        val xEnd = -0.315f * s
        for (i in 0 until 3) {
            val yl = when (i) { 0 -> -0.13f; 1 -> -0.04f; else -> 0.05f }
            val base = when (i) { 0 -> 0.07f; 1 -> 0.09f; else -> 0.06f }
            val pulse = 0.5f + 0.5f * sin(TAU * t / 1.6f + i * 1.7f)
            val len = base * (0.7f + 0.3f * pulse) * s
            stroke.alpha = (baseAlpha * (0.75f + 0.25f * pulse)).toInt()
            val y = originY + yl * s
            c.drawLine(xEnd - len, y, xEnd, y, stroke)
        }
        stroke.alpha = 255
    }

    // ---------------------------------------------------------------- car

    private fun drawBody(c: Canvas, s: Float, bodyColor: Int, shadeColor: Int) {
        fill.color = bodyColor
        c.drawPath(body, fill)

        // one cel-shade band along the bottom
        c.save()
        c.clipPath(body)
        fill.color = shadeColor
        c.drawRect(-0.3f * s, 0.075f * s, 0.3f * s, 0.15f * s, fill)
        c.restore()

        // flat glint on the rear of the bonnet line
        fill.color = Color.WHITE
        fill.alpha = 170
        rect.set(-0.262f * s, -0.03f * s, -0.218f * s, -0.01f * s)
        c.drawOval(rect, fill)
        fill.alpha = 255

        stroke.color = INK
        stroke.strokeWidth = 0.025f * s
        c.drawPath(body, stroke)

        // lights
        stroke.strokeWidth = 0.012f * s
        fill.color = HEADLIGHT
        rect.set(0.233f * s, -0.002f * s, 0.271f * s, 0.04f * s)
        c.drawOval(rect, fill)
        c.drawOval(rect, stroke)
        fill.color = TAILLIGHT
        rect.set(-0.272f * s, 0.022f * s, -0.24f * s, 0.05f * s)
        c.drawOval(rect, fill)
        c.drawOval(rect, stroke)

        // windshield = face panel
        fill.color = GLASS
        c.drawPath(panel, fill)
        stroke.strokeWidth = 0.016f * s
        c.drawPath(panel, stroke)
    }

    private fun drawWheels(c: Canvas, s: Float, mood: Mood, k: Float, t: Float, spinPeriod: Float) {
        val r = WHEEL_R * s
        val rx: Float
        val ry: Float
        if (mood == Mood.SICK) {
            rx = r * (1.08f + 0.04f * k)
            ry = r * (0.86f - 0.06f * k)
        } else {
            rx = r
            ry = r
        }
        val wy = WHEEL_BOTTOM * s - ry
        val spin = TAU * t / spinPeriod
        for (i in 0 until 2) {
            val wx = if (i == 0) -WHEEL_X * s else WHEEL_X * s
            fill.color = TIRE
            rect.set(wx - rx, wy - ry, wx + rx, wy + ry)
            c.drawOval(rect, fill)
            stroke.color = INK
            stroke.strokeWidth = 0.02f * s
            c.drawOval(rect, stroke)

            val hx = rx * 0.42f
            val hy = ry * 0.42f
            fill.color = HUB
            rect.set(wx - hx, wy - hy, wx + hx, wy + hy)
            c.drawOval(rect, fill)
            stroke.strokeWidth = 0.011f * s
            c.drawOval(rect, stroke)

            // spokes turning slowly
            stroke.strokeWidth = 0.009f * s
            val ca = cos(spin)
            val sa = sin(spin)
            val lx = hx * 0.7f
            val ly = hy * 0.7f
            c.drawLine(wx - ca * lx, wy - sa * ly, wx + ca * lx, wy + sa * ly, stroke)
            c.drawLine(wx + sa * lx, wy - ca * ly, wx - sa * lx, wy + ca * ly, stroke)
        }
    }

    // ---------------------------------------------------------------- faces

    private fun drawEyeWhite(c: Canvas, s: Float, ex: Float, ey: Float, rx: Float, ry: Float) {
        rect.set(ex - rx, ey - ry, ex + rx, ey + ry)
        fill.color = Color.WHITE
        c.drawOval(rect, fill)
    }

    private fun outlineEye(c: Canvas, s: Float, ex: Float, ey: Float, rx: Float, ry: Float) {
        rect.set(ex - rx, ey - ry, ex + rx, ey + ry)
        stroke.color = INK
        stroke.strokeWidth = 0.016f * s
        c.drawOval(rect, stroke)
    }

    private fun drawCheeks(c: Canvas, s: Float, color: Int) {
        fill.color = color
        rect.set(-0.2f * s, 0.0f * s, -0.14f * s, 0.032f * s)
        c.drawOval(rect, fill)
        rect.set(0.12f * s, 0.0f * s, 0.18f * s, 0.032f * s)
        c.drawOval(rect, fill)
    }

    private fun drawCalmFace(c: Canvas, s: Float, k: Float, t: Float) {
        val blinking = posMod(t, 3.5f) > 3.35f
        val rx = 0.052f * s
        val ry = 0.064f * s
        val ey = EYE_Y * s
        for (i in 0 until 2) {
            val ex = EYE_X[i] * s
            if (blinking) {
                // happy closed eye: ^
                stroke.color = INK
                stroke.strokeWidth = 0.018f * s
                rect.set(ex - rx * 0.9f, ey - ry * 0.35f, ex + rx * 0.9f, ey + ry * 0.9f)
                c.drawArc(rect, 200f, 140f, false, stroke)
            } else {
                drawEyeWhite(c, s, ex, ey, rx, ry)
                fill.color = INK
                rect.set(ex - 0.03f * s, ey - 0.035f * s, ex + 0.038f * s, ey + 0.055f * s)
                c.drawOval(rect, fill)
                fill.color = Color.WHITE
                c.drawCircle(ex - 0.006f * s, ey - 0.008f * s, 0.015f * s, fill)
                c.drawCircle(ex + 0.018f * s, ey + 0.03f * s, 0.0075f * s, fill)
                outlineEye(c, s, ex, ey, rx, ry)
            }
        }

        drawCheeks(c, s, BLUSH)

        // big open smile with a little tongue
        val mx = MOUTH_X * s
        val my = 0.005f * s
        val w = 0.05f * s
        val d = (0.05f + 0.01f * (1f - k)) * s
        work.reset()
        work.moveTo(mx - w, my)
        work.lineTo(mx + w, my)
        work.cubicTo(mx + w, my + d * 1.3f, mx - w, my + d * 1.3f, mx - w, my)
        work.close()
        fill.color = MOUTH
        c.drawPath(work, fill)
        c.save()
        c.clipPath(work)
        fill.color = TONGUE
        rect.set(mx - 0.018f * s, my + d * 0.62f, mx + 0.034f * s, my + d * 1.15f)
        c.drawOval(rect, fill)
        c.restore()
        stroke.color = INK
        stroke.strokeWidth = 0.016f * s
        c.drawPath(work, stroke)
    }

    private fun drawUneasyFace(c: Canvas, s: Float, k: Float, t: Float) {
        val rx = 0.052f * s
        val ry = 0.058f * s
        val ey = EYE_Y * s
        // glancing aside, drifting slowly
        val look = 0.75f + 0.25f * sin(TAU * t / 2.6f)
        for (i in 0 until 2) {
            val ex = EYE_X[i] * s
            drawEyeWhite(c, s, ex, ey, rx, ry)
            val px = ex + 0.022f * s * look
            val py = ey + 0.008f * s
            fill.color = INK
            rect.set(px - 0.025f * s, py - 0.032f * s, px + 0.025f * s, py + 0.032f * s)
            c.drawOval(rect, fill)
            fill.color = Color.WHITE
            c.drawCircle(px - 0.007f * s, py - 0.012f * s, 0.008f * s, fill)
            outlineEye(c, s, ex, ey, rx, ry)
        }

        // worried brows (inner ends raised)
        stroke.color = INK
        stroke.strokeWidth = 0.015f * s
        val bl = EYE_X[0] * s
        val br = EYE_X[1] * s
        val lift = 0.004f * s * k
        c.drawLine(bl - 0.04f * s, ey - 0.07f * s, bl + 0.03f * s, ey - 0.086f * s - lift, stroke)
        c.drawLine(br - 0.03f * s, ey - 0.086f * s - lift, br + 0.04f * s, ey - 0.07f * s, stroke)

        drawCheeks(c, s, 0x40FF6F91)

        // wobbly mouth
        val a = (0.012f + 0.008f * k) * s
        wavy(work, MOUTH_X * s - 0.045f * s, 0.09f * s, 0.028f * s, a, 2)
        stroke.strokeWidth = 0.016f * s
        c.drawPath(work, stroke)

        // one sweat drop sliding down slowly beside the cabin
        val p = posMod(t, 2.4f) / 2.4f
        val dx = 0.215f * s
        val dy = (-0.205f + 0.06f * p) * s
        val dr = (0.02f + 0.01f * k) * s
        work.reset()
        work.moveTo(dx, dy - 1.7f * dr)
        work.quadTo(dx + dr * 1.1f, dy - 0.4f * dr, dx + dr, dy)
        rect.set(dx - dr, dy - dr, dx + dr, dy + dr)
        work.arcTo(rect, 0f, 180f, false)
        work.quadTo(dx - dr * 1.1f, dy - 0.4f * dr, dx, dy - 1.7f * dr)
        work.close()
        // fade in at the top and out at the bottom so the loop restart never pops
        val alpha = (255f * minOf(1f, 2.5f * sin(PI * p))).toInt().coerceIn(0, 255)
        fill.color = SWEAT
        fill.alpha = alpha
        c.drawPath(work, fill)
        stroke.strokeWidth = 0.011f * s
        stroke.alpha = alpha
        c.drawPath(work, stroke)
        fill.color = Color.WHITE
        fill.alpha = alpha
        c.drawCircle(dx - dr * 0.35f, dy - dr * 0.2f, dr * 0.28f, fill)
        fill.alpha = 255
        stroke.alpha = 255
    }

    private fun drawSickFace(c: Canvas, s: Float, k: Float, t: Float, shadeColor: Int) {
        val rx = 0.052f * s
        val ry = 0.06f * s
        val ey = EYE_Y * s
        val rot = TAU * t / 3f
        val turns = 2.25f
        val n = 30
        for (i in 0 until 2) {
            val ex = EYE_X[i] * s
            drawEyeWhite(c, s, ex, ey, rx, ry)
            val dir = if (i == 0) 1f else -1f
            work.reset()
            for (j in 0..n) {
                val f = j.toFloat() / n
                val ang = dir * (rot + f * turns * TAU)
                val x = ex + cos(ang) * f * 0.04f * s
                val y = ey + sin(ang) * f * 0.046f * s
                if (j == 0) work.moveTo(x, y) else work.lineTo(x, y)
            }
            stroke.color = INK
            stroke.strokeWidth = 0.012f * s
            c.drawPath(work, stroke)
            outlineEye(c, s, ex, ey, rx, ry)
        }

        // queasy cheeks: a darker green patch instead of blush
        drawCheeks(c, s, (shadeColor and 0x00FFFFFF) or 0x90000000.toInt())

        // tongue sticking out, wiggling a little
        val mx = MOUTH_X * s
        val my = 0.028f * s
        val tl = (0.045f + 0.006f * sin(TAU * t / 2f)) * s
        val tx0 = mx + 0.002f * s
        val tx1 = mx + 0.042f * s
        work.reset()
        work.moveTo(tx0, my)
        work.lineTo(tx1, my)
        work.cubicTo(tx1, my + tl, tx0, my + tl, tx0, my)
        work.close()
        fill.color = TONGUE
        c.drawPath(work, fill)
        stroke.color = INK
        stroke.strokeWidth = 0.013f * s
        c.drawPath(work, stroke)
        stroke.strokeWidth = 0.009f * s
        val mid = (tx0 + tx1) / 2f
        c.drawLine(mid, my + 0.008f * s, mid, my + tl * 0.5f, stroke)

        // wavy mouth line on top
        val a = (0.012f + 0.006f * k) * s
        wavy(work, mx - 0.06f * s, 0.12f * s, my, a, 3)
        stroke.strokeWidth = 0.016f * s
        c.drawPath(work, stroke)
    }

    // ---------------------------------------------------------------- extras

    private fun drawStars(c: Canvas, s: Float, t: Float, bodyDy: Float) {
        val oy = -0.30f * s + bodyDy
        val orx = 0.14f * s
        val ory = 0.03f * s
        for (i in 0 until 3) {
            val a = TAU * t / 3f + i * TAU / 3f
            val sa = sin(a)
            val depth = 0.75f + 0.25f * sa // front (lower) stars are bigger
            c.save()
            c.translate(cos(a) * orx, oy + sa * ory)
            c.rotate(a * 57.29578f * 0.5f)
            c.scale(depth, depth)
            fill.color = STAR
            fill.alpha = (255 * (0.7f + 0.3f * depth)).toInt().coerceIn(0, 255)
            c.drawPath(star, fill)
            stroke.color = INK
            stroke.strokeWidth = 0.011f * s
            stroke.alpha = fill.alpha
            c.drawPath(star, stroke)
            c.restore()
        }
        fill.alpha = 255
        stroke.alpha = 255
    }

    private fun drawTwinkles(c: Canvas, s: Float, t: Float, bodyDy: Float) {
        for (i in 0 until 2) {
            val x = if (i == 0) 0.27f * s else -0.25f * s
            val y = (if (i == 0) -0.24f else -0.29f) * s + bodyDy
            val pulse = 0.5f + 0.5f * sin(TAU * t / 2.2f + i * PI)
            val r = 0.03f * s * (0.55f + 0.45f * pulse)
            work.reset()
            work.moveTo(x, y - r)
            work.quadTo(x, y, x + r, y)
            work.quadTo(x, y, x, y + r)
            work.quadTo(x, y, x - r, y)
            work.quadTo(x, y, x, y - r)
            work.close()
            fill.color = STAR
            c.drawPath(work, fill)
            stroke.color = INK
            stroke.strokeWidth = 0.008f * s
            c.drawPath(work, stroke)
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun wavy(p: Path, x0: Float, w: Float, y: Float, a: Float, n: Int) {
        p.reset()
        p.moveTo(x0, y)
        val seg = w / n
        for (i in 0 until n) {
            val off = if (i % 2 == 0) -a else a
            val xa = x0 + seg * i
            p.cubicTo(xa + seg * 0.33f, y + off, xa + seg * 0.67f, y + off, xa + seg, y)
        }
    }

    private fun posMod(v: Float, m: Float): Float {
        val r = v % m
        return if (r < 0f) r + m else r
    }

    private fun clamp01(v: Float) = if (v < 0f) 0f else if (v > 1f) 1f else v

    private fun lerpColor(a: Int, b: Int, f: Float): Int {
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF
        val r = (ar + (br - ar) * f).toInt()
        val g = (ag + (bg - ag) * f).toInt()
        val bl = (ab + (bb - ab) * f).toInt()
        return Color.rgb(r, g, bl)
    }

    private fun darken(c: Int, f: Float): Int = Color.rgb(
        (((c shr 16) and 0xFF) * f).toInt(),
        (((c shr 8) and 0xFF) * f).toInt(),
        ((c and 0xFF) * f).toInt()
    )
}
