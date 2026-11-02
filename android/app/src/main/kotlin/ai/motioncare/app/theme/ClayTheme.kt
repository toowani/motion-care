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
import androidx.core.graphics.PathParser
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * "말랑 3D 친구": 몽글이, an original matte clay gumdrop with thumb presses, a rolled clay coil on
 * top and stubby mitten arms. Light comes from the upper left.
 *
 * Drawn in a fixed 0..100 box scaled by size / 100. The body lives in a local space whose origin is
 * the ground point under it (y grows downward, the body spans y in [-60, 1]), so hop, squash and
 * tilt are plain canvas transforms.
 */
object ClayTheme : MoodTheme {
    override val id = "clay"
    override val title = "말랑 3D 친구"
    override val subtitle = "말랑한 찰흙 캐릭터 몽글이"
    override val animated = true

    override fun label(mood: Mood): String = when (mood) {
        Mood.CALM -> "몽글몽글"
        Mood.UNEASY -> "살짝 울렁"
        Mood.SICK -> "어질어질"
    }

    override fun cueLine(left: Boolean): String =
        if (left) "몽글이가 알려 줄게. 곧 왼쪽으로 돌아요!"
        else "몽글이가 알려 줄게. 곧 오른쪽으로 돌아요!"

    private const val TAU = 6.2831855f
    private const val PI_F = 3.1415927f
    private const val DEG = 57.29578f
    private const val GX = 50f
    private const val GY = 86f
    private val SIDES = floatArrayOf(-1f, 1f)
    private val CLAMP = Shader.TileMode.CLAMP

    // ---- paths (0..100 units; only M/L/C/Q/Z)
    private const val BODY = "M-1 -60 C15 -61.5 28.5 -51 31.5 -34 C33.8 -21 37.6 -9 31.6 -2.6 C27.6 0.8 17 0.8 0 0.8 C-17 0.8 -28 0.8 -32.4 -2.8 C-38 -8 -35.4 -18 -32.2 -28.5 L-27.6 -45.5 C-23.5 -54.5 -13.5 -59.5 -1 -60 Z"
    private const val DROP = "M0 -6 C1.6 -2.6 4 -0.4 4 2.4 C4 4.9 2.2 6.6 0 6.6 C-2.2 6.6 -4 4.9 -4 2.4 C-4 -0.4 -1.6 -2.6 0 -6 Z"
    private const val SMILE = "M-6 -19 C-5.8 -12.2 5.8 -12.2 6 -19 C2 -17.8 -2 -17.8 -6 -19 Z"
    private const val OMOUTH = "M0.2 -18.4 C2.5 -18.6 3.4 -16.6 3.1 -15 C2.8 -13.2 1.5 -12.5 0 -12.7 C-1.7 -12.6 -3.1 -13.7 -3.1 -15.3 C-3.1 -17 -1.9 -18.3 0.2 -18.4 Z"
    private const val PUFFLIPS = "M-5.2 -12.6 C-3.6 -15.6 -1.4 -16.4 0 -16.2 C1.4 -16.4 3.6 -15.6 5.2 -12.6 C3 -13.8 1.6 -14.1 0 -13.9 C-1.6 -14.1 -3 -13.8 -5.2 -12.6 Z"

    private val bodyPath: Path by lazy { PathParser.createPathFromPathData(BODY) }
    private val dropPath: Path by lazy { PathParser.createPathFromPathData(DROP) }
    private val smilePath: Path by lazy { PathParser.createPathFromPathData(SMILE) }
    private val oMouthPath: Path by lazy { PathParser.createPathFromPathData(OMOUTH) }
    private val puffPath: Path by lazy { PathParser.createPathFromPathData(PUFFLIPS) }

    /** Big rectangle with the body as an even-odd hole; its blurred shadow gives the inner occlusion. */
    private val ringPath: Path by lazy {
        Path().apply {
            addRect(-70f, -90f, 70f, 20f, Path.Direction.CW)
            addPath(bodyPath)
            fillType = Path.FillType.EVEN_ODD
        }
    }

    /** Unit spiral (radius ~1.15) for the dizzy eyes. */
    private val spiralPath: Path by lazy {
        Path().apply {
            val n = 40
            for (i in 0..n) {
                val u = i / n.toFloat()
                val a = u * TAU * 2.2f
                val r = 0.15f + u
                if (i == 0) moveTo(cos(a) * r, sin(a) * r) else lineTo(cos(a) * r, sin(a) * r)
            }
        }
    }

    /** Fixed clay speckles as (x, y, r) triples. */
    private val GRAIN: FloatArray = FloatArray(120).also { a ->
        var x = 11L
        fun rnd(): Float {
            x = (x * 16807L) % 2147483647L
            return (x.toDouble() / 2147483647.0).toFloat()
        }
        var j = 0
        repeat(40) {
            a[j++] = -30f + rnd() * 60f
            a[j++] = -58f + rnd() * 56f
            a[j++] = 0.3f + rnd() * 0.4f
        }
    }

    /** Rolled clay coil as (x, y, width) triples, thinning toward the rolled-in end. */
    private const val COIL_N = 28
    private val COIL: FloatArray = FloatArray((COIL_N + 1) * 3).also { a ->
        for (i in 0..COIL_N) {
            val u = i / COIL_N.toFloat()
            val ang = 2.0f + u * TAU * 1.2f
            val r = 5.4f - 4.1f * u.pow(0.8f)
            a[i * 3] = 2.4f + cos(ang) * r
            a[i * 3 + 1] = -62.4f + sin(ang) * r
            a[i * 3 + 2] = 6.4f - 3.4f * u
        }
    }

    // ---- colours (RGB only): light (key side), mid, strong (mid at high intensity),
    // shade (terminator, desaturated), deep (occlusion), bounce, blush
    private class Palette(
        val light: Int, val mid: Int, val strong: Int, val shade: Int,
        val deep: Int, val bounce: Int, val blush: Int,
    )

    private val PALETTES = arrayOf(
        Palette(0xDFF6EA, 0xA8E2C5, 0x98DBBA, 0x7DB89D, 0x3F7D63, 0x9BD1B6, 0xF2A0A6),
        Palette(0xFFEBDC, 0xF8C8A2, 0xF5B98E, 0xD69E7E, 0x9A624A, 0xEDBA98, 0xEE8C84),
        Palette(0xEDF3DA, 0xC4D5A2, 0xB7CB92, 0x97AA7C, 0x5F6E46, 0xBBC796, 0xC9D7A4),
    )

    private const val INK = 0x2B2126
    private const val MOUTH = 0x5A2C37
    private const val TONGUE = 0xF28C98
    private const val WHITE = 0xFFFFFF
    private const val GROUND = 0x1E2030
    private const val LIP = 0xFFFCF0
    private const val SHEEN = 0xFFFAEC

    // per-frame colours (drawn on the main thread only)
    private var light = 0
    private var mid = 0
    private var shade = 0
    private var deep = 0
    private var bounce = 0
    private var blush = 0
    private var curTilt = 0f

    // ---- preallocated drawing objects
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val shadowFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shadowStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val tmpPath = Path()
    private val armPath = Path()
    private val matrix = Matrix()
    private val rect = RectF()

    private fun intensity(mood: Mood, level: Float): Float = when (mood) {
        Mood.CALM -> level / 0.6f
        Mood.UNEASY -> (level - 0.5f) / 0.5f
        Mood.SICK -> (level - 0.8f) / 0.7f
    }.coerceIn(0f, 1f)

    private fun rgba(rgb: Int, a: Float): Int =
        ((a * 255f).roundToInt().coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF)

    private fun opaque(rgb: Int): Int = rgb or (0xFF shl 24)

    private fun mix(a: Int, b: Int, f: Float): Int {
        val ar = (a shr 16) and 0xFF; val br = (b shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF; val bg = (b shr 8) and 0xFF
        val ab = a and 0xFF; val bb = b and 0xFF
        val r = (ar + (br - ar) * f).roundToInt()
        val g = (ag + (bg - ag) * f).roundToInt()
        val bl = (ab + (bb - ab) * f).roundToInt()
        return (r shl 16) or (g shl 8) or bl
    }

    /** Canvas 2D shadowBlur (in box units) to a setShadowLayer radius with the same Gaussian sigma. */
    private fun blurR(b: Float): Float = max(0.05f, (b * 0.5f - 0.5f) / 0.57735f)

    // shaders depend on mood colours and a few animated values only: reuse them instead of allocating per frame
    private val shaders = object : LinkedHashMap<Long, Shader>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Shader>?) = size > 96
    }

    private fun key(kind: Int, a: Float, b: Float, c: Float, d: Float, colors: IntArray, stops: FloatArray?): Long {
        var h = kind.toLong()
        h = h * 1_000_003L + a.toRawBits(); h = h * 1_000_003L + b.toRawBits()
        h = h * 1_000_003L + c.toRawBits(); h = h * 1_000_003L + d.toRawBits()
        for (col in colors) h = h * 1_000_003L + col
        if (stops != null) for (f in stops) h = h * 1_000_003L + f.toRawBits()
        return h
    }

    private fun radial(x: Float, y: Float, r: Float, colors: IntArray, stops: FloatArray?): Shader =
        shaders.getOrPut(key(1, x, y, r, 0f, colors, stops)) { RadialGradient(x, y, max(r, 0.01f), colors, stops, CLAMP) }

    private fun radial2(x: Float, y: Float, r: Float, c0: Int, c1: Int): Shader = radial(x, y, r, intArrayOf(c0, c1), null)

    private fun linear(x0: Float, y0: Float, x1: Float, y1: Float, colors: IntArray, stops: FloatArray? = null): Shader =
        shaders.getOrPut(key(2, x0, y0, x1, y1, colors, stops)) { LinearGradient(x0, y0, x1, y1, colors, stops, CLAMP) }

    private fun shaded(sh: Shader): Paint {
        fill.color = Color.BLACK
        fill.shader = sh
        return fill
    }

    private fun solid(color: Int): Paint {
        fill.shader = null
        fill.color = color
        return fill
    }

    private fun oval(c: Canvas, x: Float, y: Float, rx: Float, ry: Float, p: Paint) {
        rect.set(x - rx, y - ry, x + rx, y + ry)
        c.drawOval(rect, p)
    }

    override fun draw(c: Canvas, cx: Float, cy: Float, size: Float, mood: Mood, level: Float, t: Float) {
        if (size <= 0f) return
        val pal = PALETTES[mood.ordinal]
        val k = intensity(mood, level)
        light = pal.light; mid = mix(pal.mid, pal.strong, k); shade = pal.shade
        deep = pal.deep; bounce = pal.bounce; blush = pal.blush
        val tt = if (animated) t else 0f
        val s = size / 100f

        c.save()
        c.translate(cx - 50f * s, cy - 50f * s)
        c.scale(s, s)

        // ---- posture
        val breath = sin(TAU * tt / 2.4f)
        var hop = 0f
        var sx = 1f
        var sy = 1f
        var tilt = 0f
        var land = 0f
        when (mood) {
            Mood.CALM -> {
                val ph = (tt / 1.2f) % 1f
                val air = max(0f, sin(ph * TAU))
                hop = air * (4.5f + 2f * k)
                land = max(0f, -sin(ph * TAU))
                sy = 1f + 0.05f * air - 0.07f * land + 0.015f * breath
                sx = 1f - 0.03f * air + 0.06f * land - 0.012f * breath
            }
            Mood.UNEASY -> {
                tilt = (4f + 5f * k) * sin(TAU * tt / 1.8f) * PI_F / 180f
                sy = 1f + 0.02f * breath; sx = 1f - 0.016f * breath
            }
            Mood.SICK -> {
                tilt = (4f + 3f * k) * sin(TAU * tt / 3.6f) * PI_F / 180f + 0.04f
                sx = 1.07f + 0.03f * k - 0.01f * breath; sy = 0.89f - 0.03f * k + 0.012f * breath
                land = 0.6f
            }
        }
        curTilt = tilt

        // ---- contact shadow (shrinks while airborne): tight dark core plus a soft wide falloff
        val shK = 1f - hop / 16f
        c.save()
        c.translate(GX, GY + 0.8f)
        c.scale(1f, 0.19f)
        var r = 36f * sx * shK
        c.drawCircle(0f, 0f, r, shaded(radial(0f, 0f, r,
            intArrayOf(rgba(GROUND, 0.42f * shK), rgba(GROUND, 0.2f * shK), rgba(GROUND, 0f)),
            floatArrayOf(0f, 0.5f, 1f))))
        r = 24f * sx * shK
        c.drawCircle(0f, 0f, r, shaded(radial2(0f, 0f, r, rgba(GROUND, 0.3f * shK), rgba(GROUND, 0f))))
        c.restore()

        // ---- body space
        c.save()
        c.translate(GX, GY - hop)
        c.rotate(tilt * DEG)
        c.scale(sx, sy)

        // arms behind the body
        val wave = sin(TAU * tt / 0.7f)
        when (mood) {
            Mood.CALM -> {
                val a = -0.95f + 0.28f * wave
                arm(c, 28f, -34f, 28f + cos(a) * 13f, -34f + sin(a) * 13f, 4.4f, 4.9f, false)
                arm(c, -29f, -25f, -37f, -13f, 4.4f, 4.8f, false)
            }
            Mood.SICK -> {
                val sw = 0.06f * sin(TAU * tt / 3.6f)
                arm(c, -30f, -21f, -41f + sw * 20f, -6f, 4.3f, 4.7f, false)
            }
            Mood.UNEASY -> Unit
        }

        c.drawPath(bodyPath, shaded(radial(-14f, -44f, 68f,
            intArrayOf(opaque(light), opaque(mid), opaque(mid), opaque(shade), opaque(mix(shade, bounce, 0.35f))),
            floatArrayOf(0f, 0.36f, 0.64f, 0.86f, 1f))))

        c.save()
        c.clipPath(bodyPath)
        if (mood == Mood.SICK) {
            // cooler, lighter face area so the swirl eyes pop
            c.drawRect(-40f, -62f, 40f, 2f, shaded(radial(0f, -25f, 24f,
                intArrayOf(rgba(0xE8F2E4, 0.75f), rgba(0xE8F2E4, 0.35f), rgba(0xE8F2E4, 0f)),
                floatArrayOf(0f, 0.6f, 1f))))
        }
        // clay grain
        val grainLight = rgba(WHITE, 0.14f)
        val grainDark = rgba(deep, 0.09f)
        var i = 0
        while (i < GRAIN.size) {
            c.drawCircle(GRAIN[i], GRAIN[i + 1], GRAIN[i + 2], solid(if (i % 2 != 0) grainLight else grainDark))
            i += 3
        }
        // thumb presses, kept away from the face
        press(c, -22f, -43f, 6.8f, 4.4f, -0.9f)
        press(c, 24f, -12f, 6.4f, 4.2f, 0.5f)
        if (mood != Mood.SICK) press(c, 20f, -49f, 5.4f, 3.6f, 0.6f)
        // ambient occlusion at the base
        c.drawRect(-40f, -17f, 40f, 2f,
            shaded(linear(0f, -16f, 0f, 1f, intArrayOf(rgba(deep, 0f), rgba(deep, 0.38f)))))
        // inner occlusion: only the ring's blurred shadow lands inside the clip
        shadowFill.color = opaque(deep)
        shadowFill.setShadowLayer(blurR(6f), 0f, -0.8f, rgba(deep, 0.30f))
        c.drawPath(ringPath, shadowFill)
        // faint cool rim on the far side
        c.drawRect(-40f, -62f, 40f, 2f, shaded(radial(-18f, -46f, 66f,
            intArrayOf(rgba(WHITE, 0f), rgba(WHITE, 0f), rgba(WHITE, 0.16f)),
            floatArrayOf(0f, 0.86f, 1f))))
        // one wide, low-contrast warm sheen
        c.drawRect(-40f, -62f, 40f, 2f, shaded(radial(-13f, -44f, 24f,
            intArrayOf(rgba(SHEEN, 0.30f), rgba(SHEEN, 0.12f), rgba(SHEEN, 0f)),
            floatArrayOf(0f, 0.55f, 1f))))
        c.restore()

        // ---- feet merged into the base, squashing wider on landing
        val fw = 8f * (1f + 0.24f * land)
        val fh = 3.5f * (1f - 0.15f * land)
        val fy = -1.2f
        for (sd in SIDES) {
            val fx = sd * (12f + 1.5f * land)
            tmpPath.reset()
            rect.set(fx - fw, fy - fh, fx + fw, fy + fh)
            tmpPath.addOval(rect, Path.Direction.CW)
            matrix.setRotate(sd * 0.06f * DEG, fx, fy)
            tmpPath.transform(matrix)
            c.drawPath(tmpPath, shaded(radial(fx - 2.5f, fy - 5f, 13f,
                intArrayOf(opaque(mid), opaque(mix(mid, shade, 0.35f)), opaque(shade)),
                floatArrayOf(0f, 0.5f, 1f))))
        }
        // seam where the feet meet the body
        shadowStroke.color = rgba(deep, 0.16f)
        shadowStroke.strokeWidth = 1.1f
        shadowStroke.setShadowLayer(blurR(2.4f), 0f, -0.6f, rgba(deep, 0.45f))
        for (sd in SIDES) {
            val fx = sd * (12f + 1.5f * land)
            val ex = fw * 0.94f
            rect.set(fx - ex, fy + 0.2f - fh, fx + ex, fy + 0.2f + fh)
            c.drawArc(rect, 212.4f, 115.2f, false, shadowStroke)
        }
        // gap between the feet
        oval(c, 0f, 0.4f, 4f, 1.8f, shaded(radial2(0f, 0.4f, 4f, rgba(deep, 0.35f), rgba(deep, 0f))))

        // ---- clay coil on top (swings)
        val coilRot = when (mood) {
            Mood.CALM -> 0.12f * sin(TAU * tt / 1.2f - 0.8f)
            Mood.UNEASY -> 0.22f * sin(TAU * tt / 1.8f - 1.0f)
            Mood.SICK -> 1.05f + 0.05f * sin(TAU * tt / 3.6f)
        }
        c.save()
        c.rotate(coilRot * DEG, 0f, -57f)
        coilPass(c, 0.35f, 0.6f, 1.0f, opaque(shade))
        coilPass(c, 0f, 0f, 0.82f, opaque(mid))
        coilPass(c, -0.5f, -0.6f, 0.4f, rgba(light, 0.35f))
        // fingertip press where it was pinched onto the head
        c.save()
        c.translate(-1.4f, -60.6f)
        c.rotate(-0.6f * DEG)
        oval(c, 0f, 0f, 1.6f, 1.2f, solid(rgba(deep, 0.14f)))
        c.restore()
        c.restore()
        // soft occlusion where the coil joins the head
        oval(c, 0f, -57.6f, 5f, 2.2f, shaded(radial2(0f, -57.6f, 5f, rgba(deep, 0.22f), rgba(deep, 0f))))

        drawFace(c, mood, k, tt)

        // ---- arms in front of the body
        when (mood) {
            Mood.UNEASY -> {
                val j = 0.5f * sin(TAU * tt / 0.3f)
                arm(c, 29.5f, -29f, 17.5f, -17f + j, 4.1f, 4.9f, true)
                arm(c, -29.5f, -29f, -17.5f, -17f - j, 4.1f, 4.9f, true)
            }
            Mood.SICK -> arm(c, 30.5f, -18f, 14.5f, -8f, 4.2f, 5.6f, true)
            Mood.CALM -> Unit
        }

        // ---- sweat drop (desaturated, no outline) at the side of the head
        if (mood == Mood.SICK) {
            val dy = (tt * 5f) % 3f
            c.save()
            c.translate(29f, -43f + dy)
            c.scale(0.95f, 0.95f)
            c.drawPath(dropPath,
                shaded(linear(0f, -6f, 0f, 6.6f, intArrayOf(opaque(0xEEF4F6), opaque(0x9DB6C2)))))
            oval(c, 0.8f, 4.2f, 2.6f, 1.6f, solid(rgba(0x7896A5, 0.35f)))
            oval(c, -1.3f, 1.4f, 0.9f, 1.4f, solid(rgba(WHITE, 0.85f)))
            c.restore()
        }
        c.restore()

        // ---- wobble marks, stronger on the side opposite the tilt
        if (mood == Mood.UNEASY) {
            val side = if (sin(TAU * tt / 1.8f) >= 0f) -1f else 1f
            c.save()
            c.translate(GX, GY - 40f)
            stroke.shader = null
            stroke.strokeWidth = 1.6f
            for (sd in SIDES) {
                stroke.color = rgba(shade, 0.85f * (if (sd == side) 1f else 0.45f))
                tmpPath.reset()
                tmpPath.moveTo(sd * 40f, -6f); tmpPath.quadTo(sd * 43.5f, 0f, sd * 40f, 6f)
                tmpPath.moveTo(sd * 45f, -3.5f); tmpPath.quadTo(sd * 47.2f, 0f, sd * 45f, 3.5f)
                c.drawPath(tmpPath, stroke)
            }
            c.restore()
        }
        c.restore()
    }

    private fun drawFace(c: Canvas, mood: Mood, k: Float, tt: Float) {
        val blinking = mood == Mood.CALM && (tt % 3.3f) > 3.12f
        stroke.shader = null
        if (mood != Mood.SICK) {
            for (sd in SIDES) {
                val x = sd * 18.5f
                oval(c, x, -19.5f, 6.5f, 4.3f, shaded(radial2(x, -19.5f, 6.5f, rgba(blush, 0.55f), rgba(blush, 0f))))
            }
        }
        if (mood == Mood.SICK) {
            // puffed cheeks: lit on top, shadowed below
            for (sd in SIDES) {
                val x = sd * 8.2f
                val y = -14.2f
                oval(c, x, y + 1.6f, 5.4f, 4f,
                    shaded(radial2(x, y + 1.4f, 5.2f, rgba(deep, 0.24f), rgba(deep, 0f))))
                oval(c, x, y - 0.4f, 4.8f, 3.6f, shaded(radial(x - 1f, y - 1.6f, 5f,
                    intArrayOf(rgba(0xFAFDF2, 0.8f), rgba(0xF0F6E2, 0.5f), rgba(0xF0F6E2, 0f)),
                    floatArrayOf(0f, 0.55f, 1f))))
            }
            // swirl eyes on a white backing
            val rot = tt * 2.2f
            stroke.color = opaque(INK)
            stroke.strokeWidth = 0.38f
            for (sd in SIDES) {
                c.save()
                c.translate(sd * 11f, -29f)
                c.rotate(sd * rot * DEG)
                c.scale(5.2f, 5.2f)
                c.drawCircle(0f, 0f, 1.25f, solid(rgba(0xFAFCF6, 0.9f)))
                c.drawPath(spiralPath, stroke)
                c.restore()
            }
        } else if (blinking) {
            stroke.color = opaque(INK)
            stroke.strokeWidth = 1.8f
            for (sd in SIDES) {
                tmpPath.reset()
                tmpPath.moveTo(sd * 11f - 3.8f, -27.5f)
                tmpPath.quadTo(sd * 11f, -25f, sd * 11f + 3.8f, -27.5f)
                c.drawPath(tmpPath, stroke)
            }
        } else {
            val lookX = if (mood == Mood.UNEASY) -0.5f else 0f
            val lookY = if (mood == Mood.UNEASY) -0.6f else 0f
            for (sd in SIDES) bead(c, sd * 11f, -28f, 4.2f, 5.3f, lookX, lookY)
            if (mood == Mood.UNEASY) {
                // watery lower lid
                stroke.color = rgba(WHITE, 0.7f)
                stroke.strokeWidth = 0.9f
                for (sd in SIDES) {
                    rect.set(sd * 11f - 3.4f, -28f - 4.5f, sd * 11f + 3.4f, -28f + 4.5f)
                    c.drawArc(rect, 45f, 90f, false, stroke)
                }
            }
        }

        // brows
        stroke.color = rgba(deep, 0.85f)
        stroke.strokeWidth = 2.1f
        if (mood == Mood.UNEASY) {
            for (sd in SIDES) {
                tmpPath.reset()
                tmpPath.moveTo(sd * 15f, -35.5f)
                tmpPath.quadTo(sd * 11f, -37.5f, sd * 7f, -38.8f)
                c.drawPath(tmpPath, stroke)
            }
        } else if (mood == Mood.SICK) {
            stroke.strokeWidth = 1.8f
            for (sd in SIDES) {
                tmpPath.reset()
                tmpPath.moveTo(sd * 15f, -35.5f)
                tmpPath.quadTo(sd * 11.5f, -37.6f, sd * 7.5f, -37.8f)
                c.drawPath(tmpPath, stroke)
            }
            // nauseous shade lines on the forehead
            c.save()
            c.clipPath(bodyPath)
            stroke.color = Color.BLACK
            stroke.shader = linear(0f, -55f, 0f, -44f,
                intArrayOf(rgba(0x5E7488, 0.45f + 0.15f * k), rgba(0x5E7488, 0f)))
            stroke.strokeWidth = 1.2f
            for (dx in floatArrayOf(-4f, 0f, 4f)) {
                c.drawLine(dx - 6f, -54f + abs(dx) * 0.25f, dx - 6f, -44.5f, stroke)
            }
            stroke.shader = null
            c.restore()
        }

        // mouth
        when (mood) {
            Mood.CALM -> {
                c.drawPath(smilePath, solid(opaque(MOUTH)))
                c.save()
                c.clipPath(smilePath)
                oval(c, 0.4f, -12.9f, 3.8f, 2.7f, solid(opaque(TONGUE)))
                c.restore()
            }
            Mood.UNEASY -> {
                val q = 1f + 0.08f * sin(TAU * tt / 0.45f)
                c.save()
                c.scale(1f / q, q, 0f, -15.5f)
                c.drawPath(oMouthPath, solid(opaque(MOUTH)))
                oval(c, 0f, -13.9f, 1.7f, 1f, solid(rgba(TONGUE, 0.8f)))
                c.restore()
            }
            Mood.SICK -> c.drawPath(puffPath, solid(opaque(MOUTH)))
        }
    }

    /** Capsule (mitten) arm from shoulder to hand; a front arm casts a soft shadow on the body. */
    private fun arm(
        c: Canvas, x0: Float, y0: Float, x1: Float, y1: Float,
        r0: Float, r1: Float, front: Boolean,
    ) {
        val ang = atan2(y1 - y0, x1 - x0)
        val len = hypot(x1 - x0, y1 - y0)
        val th = ang - PI_F / 2f
        c.save()
        c.translate(x0, y0)
        c.rotate(th * DEG)
        armPath.reset()
        rect.set(-r0, -r0, r0, r0)
        armPath.arcTo(rect, 180f, 180f, false)
        rect.set(-r1, len - r1, r1, len + r1)
        armPath.arcTo(rect, 0f, 180f, false)
        armPath.close()
        // light direction (-0.45, -0.6) in world -> local
        val cs = cos(-th)
        val sn = sin(-th)
        val lx = -0.45f * cs + 0.6f * sn
        val ly = -0.45f * sn - 0.6f * cs
        val rr = max(r0, r1)
        if (front) {
            // the shadow offset is screen-space in the design; undo the arm and body rotation
            val a = th + curTilt
            val ca = cos(a)
            val sa = sin(a)
            shadowFill.color = opaque(shade)
            shadowFill.setShadowLayer(blurR(3.2f), 0.6f * ca + 1.4f * sa, -0.6f * sa + 1.4f * ca, rgba(deep, 0.32f))
            c.drawPath(armPath, shadowFill)
        }
        c.drawPath(armPath, shaded(radial(lx * rr, len * 0.55f + ly * rr, len * 0.6f + rr * 1.6f,
            intArrayOf(opaque(mix(light, mid, 0.35f)), opaque(mid), opaque(shade), opaque(mix(shade, bounce, 0.4f))),
            floatArrayOf(0f, 0.4f, 0.82f, 1f))))
        c.restore()
    }

    /** Shallow thumb press: shadow crescent on the wall facing away from the light, lit crescent opposite. */
    private fun press(c: Canvas, x: Float, y: Float, rx: Float, ry: Float, rot: Float) {
        c.save()
        c.translate(x, y)
        c.rotate(rot * DEG)
        val cs = cos(-rot)
        val sn = sin(-rot)
        val lx = -0.6f * cs + 0.8f * sn
        val ly = -0.6f * sn - 0.8f * cs
        oval(c, 0f, 0f, rx, ry, shaded(linear(lx * rx, ly * ry, -lx * rx, -ly * ry,
            intArrayOf(rgba(deep, 0.20f), rgba(deep, 0.07f), rgba(LIP, 0f), rgba(LIP, 0.34f)),
            floatArrayOf(0f, 0.35f, 0.6f, 1f))))
        // raised lip pushed out on the lit outer side
        val hx = lx * rx * 1.05f
        val hy = ly * ry * 1.05f
        oval(c, hx, hy, rx * 0.7f, ry * 0.55f, shaded(radial2(hx, hy, rx * 0.7f, rgba(LIP, 0.16f), rgba(LIP, 0f))))
        c.restore()
    }

    /** Matte claymation eye: dark bead with one small catchlight. */
    private fun bead(c: Canvas, x: Float, y: Float, rx: Float, ry: Float, lookX: Float, lookY: Float) {
        oval(c, x, y, rx, ry, shaded(radial2(x - rx * 0.3f, y - ry * 0.4f, ry * 1.5f,
            opaque(0x3A2F35), opaque(0x1C1418))))
        oval(c, x - rx * 0.32f + lookX, y - ry * 0.38f + lookY, rx * 0.27f, ry * 0.22f, solid(rgba(WHITE, 0.92f)))
    }

    /** One stroke pass of the coil, segment by segment so the width tapers. */
    private fun coilPass(c: Canvas, dx: Float, dy: Float, wf: Float, color: Int) {
        stroke.shader = null
        stroke.color = color
        for (i in 1..COIL_N) {
            val a = (i - 1) * 3
            val b = i * 3
            stroke.strokeWidth = COIL[b + 2] * wf
            c.drawLine(COIL[a] + dx, COIL[a + 1] + dy, COIL[b] + dx, COIL[b + 1] + dy, stroke)
        }
    }
}
