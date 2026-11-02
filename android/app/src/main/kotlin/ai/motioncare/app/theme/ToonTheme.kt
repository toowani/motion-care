package ai.motioncare.app.theme

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import androidx.core.graphics.PathParser
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * 2D cel-shaded cartoon: 부릉이, a squat chibi car seen head-on, headlights for eyes and the grille
 * for a mouth, on a round stage with a road running to the horizon.
 * Signature traits: springy pink antenna ball, heart hood badge, notched bumper, chrome grille slat.
 * CALM: sky blue, sparkly eyes, big smile, hops with squash and stretch, speed streaks.
 * UNEASY: yellow, worry brows and drooping lids, glancing eyes, wavy mouth, rocking, sweat drops.
 * SICK: mint on a lavender stage, spiral eyes, tongue out, slow sway, smoke puffs, two dizzy stars.
 *
 * Designed in a 0..100 box and scaled by size / 100. Paths are parsed once; gradients are built
 * once (per mood where they differ) in fixed local frames and placed with translate().
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
    private const val TAU = 6.2831855f
    private const val PI = 3.1415927f
    private const val RAD2DEG = 57.29578f
    private const val LW = 2.4f // outline width in design units
    private const val ER = 7.5f // eye (headlight lens) radius

    private val INK = 0xFF25233F.toInt()
    private val WHITE = Color.WHITE

    private class Pal(
        val hi: Int, val base: Int, val mid: Int, val shade: Int, val deep: Int,
        val skyTop: Int, val skyBot: Int, val ground: Int, val road: Int,
        val shadow: Int, val streak: Int, val iris: Int, val irisHi: Int,
    )

    private val PALS = arrayOf(
        Pal(
            0xFFD2F3FF.toInt(), 0xFF5CC6F6.toInt(), 0xFF3AAEEC.toInt(), 0xFF2388D2.toInt(), 0xFF1A6FB4.toInt(),
            0xFFE6F6FF.toInt(), 0xFFCBEAFF.toInt(), 0xFFCDEBC9.toInt(), 0xFFEEF4FB.toInt(),
            0x4D1A5AA0, 0xFF3AAEEC.toInt(), 0xFF2B4C9A.toInt(), 0xFF59A9F0.toInt(),
        ),
        Pal(
            0xFFFFF6C9.toInt(), 0xFFFFD54D.toInt(), 0xFFFFC234.toInt(), 0xFFEE9E1B.toInt(), 0xFFC9780C.toInt(),
            0xFFFFF9E8.toInt(), 0xFFFFEEC6.toInt(), 0xFFF1E6B8.toInt(), 0xFFFBF6EA.toInt(),
            0x4DAA6E14, 0xFFF2B23A.toInt(), 0xFF5A3A1E.toInt(), 0xFFB8813A.toInt(),
        ),
        // clean mint body on a greyish-lavender stage so the car pops instead of melting into green
        Pal(
            0xFFE4FBEF.toInt(), 0xFF8BE0B2.toInt(), 0xFF62CC98.toInt(), 0xFF3FAE7C.toInt(), 0xFF2C8C61.toInt(),
            0xFFF3F1F8.toInt(), 0xFFDFDBEB.toInt(), 0xFFD3CEE2.toInt(), 0xFFECE9F3.toInt(),
            0x47504678, 0xFFB7B0CC.toInt(), 0xFF3E5A2A.toInt(), 0xFF7FA65A.toInt(),
        ),
    )

    // ---- shapes (0..100 box), frontal and symmetric about x = 50; parsed once ----
    private fun p(d: String): Path = PathParser.createPathFromPathData(d)

    private class Shapes {
        val body = p("M20 80C13 80 10.5 75.5 10.8 69L11.4 58C11.8 52 15 48.4 21 47.4" +
            "C22.6 37 29 30.4 50 30.4C71 30.4 77.4 37 79 47.4" +
            "C85 48.4 88.2 52 88.6 58L89.2 69C89.5 75.5 87 80 80 80Z")
        val glass = p("M28.6 45.8C29.8 39 34.8 35.2 50 35.2C65.2 35.2 70.2 39 71.4 45.8C62 47.1 38 47.1 28.6 45.8Z")
        val glassStreak1 = p("M36.5 45.6L43.6 35.6L49 35.4L41.8 46Z")
        val glassStreak2 = p("M45.4 46.2L52.6 35.5L55.2 35.6L48 46.3Z")
        val dash = p("M33 46.3C40 44.4 60 44.4 67 46.3")
        val sideShade = p("M100 40L80.6 47.8C83 54 83.6 66 82 82L100 82Z")
        val bellyShade = p("M0 72.6C20 76.6 80 76.6 100 72.6L100 100L0 100Z")
        val roofRim = p("M27.8 40.6C30.6 33.6 37.6 32 50 32C58 32 63.6 32.6 67 34.4C61 33.6 56 33.5 50 33.5C38.6 33.5 31.6 35.2 27.8 40.6Z")
        val rimL = p("M15.6 55C14.8 59 14.6 66 15 71.6")
        val hoodLine = p("M21.2 47.6C36 50 64 50 78.8 47.6")
        val mirrorL = p("M16.8 49.4C13.8 45.8 7.8 46 6.6 49.6C5.6 52.8 8.8 54.8 12.8 54.4C14.8 54.2 16 53.2 16.8 51.8Z")
        val mirrorR = p("M83.2 49.4C86.2 45.8 92.2 46 93.4 49.6C94.4 52.8 91.2 54.8 87.2 54.4C85.2 54.2 84 53.2 83.2 51.8Z")
        val mirrorHiL = p("M8.8 49.4C9.6 48 11.6 47.6 13.2 48.4")
        val mirrorHiR = p("M86.8 48.4C88.4 47.6 90.4 48 91.2 49.4")
        val bumper = p("M18 76.4L44.6 76.4C46.4 76.4 47 78 50 78C53 78 53.6 76.4 55.4 76.4L82 76.4C87.2 76.4 87.8 83.6 82.4 83.6L17.6 83.6C12.2 83.6 12.8 76.4 18 76.4Z")
        val mouthSmile = p("M40.4 64.4C46 65.8 54 65.8 59.6 64.4C60 70.8 55.8 74.2 50 74.2C44.2 74.2 40 70.8 40.4 64.4Z")
        val tongueSmile = p("M44.4 72.6C46.2 70 48.8 69.8 50 71.2C51.2 69.8 53.8 70 55.6 72.6C53.8 74.2 52.2 74.6 50 74.6C47.8 74.6 46.2 74.2 44.4 72.6Z")
        val mouthWavy = p("M41.2 69.6Q43.6 66.8 46 69.4T50.8 69.4T55.6 69.4T59.6 68.8")
        val mouthSick = p("M41 67.4Q43.3 66.2 45.6 67.4T50.2 67.4T54.8 67.4T59.4 67.4C60.2 71.6 56 74.2 50.2 74.2C44.4 74.2 40.2 71.6 41 67.4Z")
        val tongueSick = p("M52.6 71C52.8 69.2 59.6 69 59.8 71C60.2 74 59.8 76.8 56.2 76.8C52.6 76.8 52.2 74 52.6 71Z")
        val tongueSickLine = p("M56.2 71.2L56.2 74.6")
        val browL = p("M21.6 51.4Q29.4 50.6 36.8 46")
        val browR = p("M78.4 51.4Q70.6 50.6 63.2 46")
        val gloom = p("M42 37.4L42 43M50 36.6L50 43.6M58 37.4L58 43")
        val sweat = p("M0 -7.2C2.8 -3 4.8 0 4.8 2.6C4.8 5.4 2.6 7.2 0 7.2C-2.6 7.2 -4.8 5.4 -4.8 2.6C-4.8 0 -2.8 -3 0 -7.2Z")
        val spark = p("M0 -5C0.8 -1.6 1.6 -0.8 5 0C1.6 0.8 0.8 1.6 0 5C-0.8 1.6 -1.6 0.8 -5 0C-1.6 -0.8 -0.8 -1.6 0 -5Z")
        val star5 = p("M0 -5L1.5 -1.7L5 -1.5L2.3 0.9L3.1 4.4L0 2.5L-3.1 4.4L-2.3 0.9L-5 -1.5L-1.5 -1.7Z")
        val heart = p("M0 1.9C-1.9 0.6 -3 -0.4 -3 -1.6C-3 -2.6 -2.2 -3.2 -1.4 -3.2C-0.8 -3.2 -0.3 -2.9 0 -2.4" +
            "C0.3 -2.9 0.8 -3.2 1.4 -3.2C2.2 -3.2 3 -2.6 3 -1.6C3 -0.4 1.9 0.6 0 1.9Z")
        val road = p("M41 58L59 58L114 100L-14 100Z")
        val nervousL = p("M9 28Q12 31 10 34")
        val nervousR = p("M91 28Q88 31 90 34")

        val disc = Path().apply { addCircle(50f, 50f, 48f, Path.Direction.CW) }
        val tyre = Path().apply { addOval(-6.4f, -7f, 6.4f, 7f, Path.Direction.CW) }
        val lens = Path().apply {
            val r = ER
            val q = ER * 0.552f
            moveTo(-r, 0f)
            cubicTo(-r, -q, -q, -r, 0f, -r)
            cubicTo(q, -r, r, -q, r, 0f)
            cubicTo(r, q, q, r, 0f, r)
            cubicTo(-q, r, -r, q, -r, 0f)
            close()
        }
        // spiral eyes depend only on the side; the spin is a canvas rotation
        val spiralL = spiral(-1f)
        val spiralR = spiral(1f)

        private fun spiral(side: Float) = Path().apply {
            for (i in 0..44) {
                val a = i / 44f * TAU * 2.1f
                val rr = ER * 0.08f + ER * 0.7f * (i / 44f)
                val px = cos(a) * rr * side
                val py = sin(a) * rr
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
        }
    }

    private val S by lazy { Shapes() }

    private val EYE_X = floatArrayOf(29.6f, 70.4f)
    private const val EYE_Y = 62.2f
    private val TYRE_X = floatArrayOf(22.4f, 77.6f)

    // speed streak rows: y, direction, phase
    private val STREAKS = floatArrayOf(
        24f, -1f, 0f, 37f, -1f, 0.5f, 21f, 1f, 0.25f,
        34f, 1f, 0.75f, 50f, -1f, 0.6f, 49f, 1f, 0.1f,
    )

    // ---- preallocated drawing objects ----
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isDither = true }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val work = Path()
    private val rect = RectF()

    // ---- cached shaders ----
    private val skyShaders = arrayOfNulls<Shader>(3)
    private val bodyShaders = arrayOfNulls<Shader>(3)
    private val irisShaders = arrayOfNulls<Shader>(3)

    private val bezelShader by lazy {
        lin(0f, -ER - 2f, 0f, ER + 2f, intArrayOf(WHITE, 0xFFD9E1EC.toInt(), 0xFFA9B5C8.toInt()), floatArrayOf(0f, 0.55f, 1f))
    }
    private val lensShader by lazy {
        radial(
            -ER * 0.3f, -ER * 0.35f, ER * 0.1f, 0f, 0f, ER,
            intArrayOf(WHITE, 0xFFFFFDF2.toInt(), 0xFFF1E9C8.toInt()), floatArrayOf(0f, 0.7f, 1f),
            floatArrayOf(0f, 0.58f, 0.88f),
        )
    }
    private val rimShader by lazy {
        lin(0f, -7f, 0f, 7f, intArrayOf(WHITE, 0xFFC9D2E0.toInt(), 0xFF8E9AB0.toInt()), floatArrayOf(0f, 0.6f, 1f))
    }
    private val ballShader by lazy {
        radial(
            -1.2f, -1.4f, 0.3f, 0f, 0f, 4f,
            intArrayOf(WHITE, 0xFFFF8FB1.toInt(), 0xFFEE4F7B.toInt()), floatArrayOf(0f, 0.35f, 1f),
            floatArrayOf(0f, 0.3f, 0.92f),
        )
    }
    private val glassShader by lazy {
        lin(0f, 35f, 0f, 46f, intArrayOf(0xFFF6FCFF.toInt(), 0xFFB9D5EC.toInt()), floatArrayOf(0f, 1f))
    }
    private val bumperShader by lazy {
        lin(0f, 76f, 0f, 83.6f, intArrayOf(WHITE, 0xFFD6DEEA.toInt(), 0xFF9FAABF.toInt()), floatArrayOf(0f, 0.5f, 1f))
    }
    private val sweatShader by lazy {
        lin(0f, -7f, 0f, 7f, intArrayOf(0xFFDDF4FF.toInt(), 0xFF6CC4F5.toInt()), floatArrayOf(0f, 1f))
    }

    private fun lin(x0: Float, y0: Float, x1: Float, y1: Float, colors: IntArray, stops: FloatArray): Shader =
        LinearGradient(x0, y0, x1, y1, colors, stops, Shader.TileMode.CLAMP)

    /** Two-point radial gradient like canvas createRadialGradient. Before API 31 there is no
     *  two-point form, so fall back to one centre halfway to the focus with adjusted stops. */
    private fun radial(
        fx: Float, fy: Float, r0: Float, x1: Float, y1: Float, r1: Float,
        colors: IntArray, stops: FloatArray, fallbackStops: FloatArray,
    ): Shader {
        if (Build.VERSION.SDK_INT >= 31) {
            val packed = LongArray(colors.size) { Color.pack(colors[it]) }
            return RadialGradient(fx, fy, r0, x1, y1, r1, packed, stops, Shader.TileMode.CLAMP)
        }
        val mx = (fx + x1) / 2f
        val my = (fy + y1) / 2f
        val dx = x1 - mx
        val dy = y1 - my
        val r = r1 + kotlin.math.sqrt(dx * dx + dy * dy)
        return RadialGradient(mx, my, r, colors, fallbackStops, Shader.TileMode.CLAMP)
    }

    private fun sky(m: Int, pal: Pal): Shader =
        skyShaders[m] ?: lin(0f, 2f, 0f, 98f, intArrayOf(pal.skyTop, pal.skyBot), floatArrayOf(0f, 1f))
            .also { skyShaders[m] = it }

    private fun bodyGrad(m: Int, pal: Pal): Shader =
        bodyShaders[m] ?: lin(0f, 30f, 0f, 80f, intArrayOf(pal.hi, pal.base, pal.mid), floatArrayOf(0f, 0.2f, 1f))
            .also { bodyShaders[m] = it }

    private fun irisGrad(m: Int, pal: Pal, ir: Float): Shader =
        irisShaders[m] ?: lin(0f, -ir, 0f, ir, intArrayOf(INK, pal.iris, pal.irisHi), floatArrayOf(0f, 0.45f, 1f))
            .also { irisShaders[m] = it }

    // ---- paint helpers (globalAlpha multiplies the colour's own alpha, as in canvas 2D) ----
    private fun solid(color: Int, a: Float = 1f): Paint {
        fill.shader = null
        fill.color = color
        if (a < 1f) fill.alpha = (Color.alpha(color) * a + 0.5f).toInt().coerceIn(0, 255)
        return fill
    }

    private fun shaded(sh: Shader, a: Float = 1f): Paint {
        fill.shader = sh
        fill.color = Color.BLACK
        fill.alpha = (255f * a + 0.5f).toInt().coerceIn(0, 255)
        return fill
    }

    private fun pen(color: Int, w: Float, a: Float = 1f): Paint {
        stroke.shader = null
        stroke.color = color
        if (a < 1f) stroke.alpha = (Color.alpha(color) * a + 0.5f).toInt().coerceIn(0, 255)
        stroke.strokeWidth = w
        stroke.strokeCap = Paint.Cap.ROUND
        stroke.strokeJoin = Paint.Join.ROUND
        return stroke
    }

    private fun ink(w: Float = LW): Paint = pen(INK, w)

    private fun oval(c: Canvas, x: Float, y: Float, rx: Float, ry: Float, paint: Paint) {
        rect.set(x - rx, y - ry, x + rx, y + ry)
        c.drawOval(rect, paint)
    }

    private fun ovalRot(c: Canvas, x: Float, y: Float, rx: Float, ry: Float, rotRad: Float, paint: Paint) {
        c.save()
        c.translate(x, y)
        c.rotate(rotRad * RAD2DEG)
        oval(c, 0f, 0f, rx, ry, paint)
        c.restore()
    }

    // ---- per-frame eye state (reused, no allocation) ----
    private var gazeX = 0f
    private var gazeY = 0f
    private var lidTop = 0f
    private var lidBottom = 0f
    private var lidTilt = 0f
    private var spin = 0f
    private var blinking = false

    override fun draw(c: Canvas, cx: Float, cy: Float, size: Float, mood: Mood, level: Float, t: Float) {
        if (size <= 1f) return
        val m = mood.ordinal
        val pal = PALS[m]
        val sh = S
        val k = when (mood) {
            Mood.CALM -> clamp01(level / 0.6f)
            Mood.UNEASY -> clamp01((level - 0.5f) / 0.5f)
            Mood.SICK -> clamp01((level - 0.8f) / 0.7f)
        }

        c.save()
        c.translate(cx - size / 2f, cy - size / 2f)
        c.scale(size / 100f, size / 100f)

        // motion: hop cycle with squash on landing, stretch on take-off / fall
        var bob = 0f
        var rot = 0f // radians
        var sx = 1f
        var sy = 1f
        var shakeX = 0f
        val speed: Float
        val wheel: Float
        var hop = 0f
        val hopU = posMod(t / 1.05f, 1f)
        when (mood) {
            Mood.CALM -> {
                hop = sin(PI * hopU)
                val contact = max(0f, cos(TAU * hopU)).pow(6)
                val stretch = 0.06f * abs(cos(PI * hopU)) * (1f - contact)
                bob = -4.2f * hop
                sy = 1f - 0.11f * contact + stretch
                sx = 1f + 0.08f * contact - stretch * 0.7f
                speed = 0.9f
                wheel = t * 3.2f
            }
            Mood.UNEASY -> {
                rot = (3f + 3f * k) * sin(TAU * t / 1.4f) * PI / 180f
                shakeX = 0.35f * sin(TAU * t * 7f) * (0.4f + k)
                bob = -0.6f * (0.5f + 0.5f * sin(TAU * t / 0.7f))
                speed = 0.45f
                wheel = t * 1.8f
            }
            Mood.SICK -> {
                rot = (4f + 3f * k) * sin(TAU * t / 3.2f + 0.8f) * PI / 180f
                sy = 0.965f
                sx = 1.025f
                bob = 0.6f + 0.8f * sin(TAU * t / 3.2f)
                speed = 0.18f
                wheel = t * 0.9f
            }
        }

        backdrop(c, m, pal, t, speed)

        // crisp contact shadow, shrinks while airborne
        c.save()
        c.clipPath(sh.disc)
        oval(c, 50f, 90f, 37f * (1f - 0.12f * hop), 3.4f * (1f - 0.12f * hop), solid(pal.shadow))
        c.restore()

        if (mood == Mood.SICK) drawSmoke(c, t)

        // tyres stay planted on the road (body rides the suspension above them)
        c.save()
        c.translate(shakeX, 0f)
        drawTyre(c, TYRE_X[0], wheel, -1f)
        drawTyre(c, TYRE_X[1], wheel, 1f)
        c.restore()

        // body transform: pivot at ground under the car
        c.save()
        c.translate(50f + shakeX, 84f + bob)
        c.rotate(rot * RAD2DEG)
        c.scale(sx, sy)
        c.translate(-50f, -84f)

        // antenna with springy pink ball
        val sway = when (mood) {
            Mood.CALM -> -cos(PI * hopU * 2f) * 5f
            Mood.UNEASY -> sin(TAU * t / 1.4f - 0.6f) * 7f
            Mood.SICK -> sin(TAU * t / 3.2f) * 9f + 6f
        }
        val bx = 63f + sway
        val by = 15.5f
        work.rewind()
        work.moveTo(60.5f, 31f)
        work.quadTo(61.5f + sway * 0.3f, 22f, bx, by + 3f)
        c.drawPath(work, ink(1.9f))
        c.save()
        c.translate(bx, by)
        c.drawCircle(0f, 0f, 4f, shaded(ballShader))
        c.drawCircle(0f, 0f, 4f, ink(1.9f))
        c.restore()

        // mirrors
        solid(pal.mid)
        c.drawPath(sh.mirrorL, fill)
        c.drawPath(sh.mirrorR, fill)
        ink()
        c.drawPath(sh.mirrorL, stroke)
        c.drawPath(sh.mirrorR, stroke)
        pen(WHITE, 1.3f, 0.85f)
        c.drawPath(sh.mirrorHiL, stroke)
        c.drawPath(sh.mirrorHiR, stroke)

        // body with shade band on the right, rim light on the left
        c.drawPath(sh.body, shaded(bodyGrad(m, pal)))
        c.save()
        c.clipPath(sh.body)
        c.drawPath(sh.sideShade, solid(pal.shade))
        c.drawPath(sh.bellyShade, solid(pal.shade, 0.85f))
        c.drawPath(sh.roofRim, solid(WHITE, 0.8f))
        c.drawPath(sh.rimL, pen(WHITE, 1.6f, 0.75f))
        c.restore()
        c.drawPath(sh.body, ink())
        c.drawPath(sh.hoodLine, pen(pal.deep, 1.2f))

        // windshield with a dashboard line
        c.drawPath(sh.glass, shaded(glassShader))
        c.save()
        c.clipPath(sh.glass)
        c.drawPath(sh.glassStreak1, solid(WHITE, 0.9f))
        c.drawPath(sh.glassStreak2, solid(WHITE, 0.7f))
        c.drawPath(sh.dash, pen(0xFF8FAECB.toInt(), 1.2f))
        if (mood == Mood.SICK) c.drawPath(sh.gloom, pen(pal.deep, 1.6f, 0.7f))
        c.restore()
        c.drawPath(sh.glass, ink(2f))

        // cheeks on the fenders outside the eyes
        val cheek = if (mood == Mood.SICK) 0x73968CC8 else 0x8CFF6E96.toInt()
        oval(c, 18.2f, 72.4f, 4.2f, 2.6f, solid(cheek))
        oval(c, 81.8f, 72.4f, 4.2f, 2.6f, fill)

        // heart hood badge
        c.save()
        c.translate(50f, 55.2f)
        c.scale(1.3f, 1.3f)
        c.drawPath(sh.heart, solid(0xFFFF6F96.toInt()))
        c.drawPath(sh.heart, ink(1.25f))
        c.drawCircle(-1.4f, -1.9f, 0.6f, solid(WHITE, 0.85f))
        c.restore()

        // eyes
        val phase = posMod(t, 3.4f)
        val blink = if (phase > 3.18f) sin((phase - 3.18f) / 0.22f * PI) else 0f
        when (mood) {
            Mood.CALM -> {
                gazeX = 0f; gazeY = -0.15f; lidTop = blink; lidBottom = 0.16f * (1f - blink); lidTilt = 0f; spin = 0f
            }
            Mood.UNEASY -> {
                val g = sin(TAU * t / 2.8f)
                val sd = if (g > 0f) min(1f, g * 3f) else max(-1f, g * 3f)
                gazeX = sd * 0.85f; gazeY = -0.05f; lidTop = max(blink, 0.3f); lidBottom = 0f; lidTilt = 0.26f; spin = 0f
            }
            Mood.SICK -> {
                gazeX = 0f; gazeY = 0f; lidTop = max(blink, 0.36f); lidBottom = 0f; lidTilt = 0.16f; spin = t * 3f
            }
        }
        blinking = blink > 0.01f && blink >= lidTop - 0.001f
        for (i in 0 until 2) {
            c.save()
            c.translate(EYE_X[i], EYE_Y)
            drawEye(c, mood, m, pal, if (i == 0) -1f else 1f)
            c.restore()
        }

        // heavy worry brows over the bezels
        if (mood == Mood.UNEASY) {
            ink(3f)
            c.drawPath(sh.browL, stroke)
            c.drawPath(sh.browR, stroke)
        }

        // bumper with the chin notch
        c.drawPath(sh.bumper, shaded(bumperShader))
        c.drawPath(sh.bumper, ink())

        // grille mouth
        when (mood) {
            Mood.CALM -> {
                c.drawPath(sh.mouthSmile, solid(0xFF4A2240.toInt()))
                c.save()
                c.clipPath(sh.mouthSmile)
                c.drawPath(sh.tongueSmile, solid(0xFFFF7FA0.toInt()))
                c.drawRect(38f, 63f, 62f, 67.4f, solid(0xFFE9EEF6.toInt())) // chrome slat = upper teeth
                c.drawRect(38f, 67f, 62f, 67.6f, solid(0xFFB9C3D3.toInt()))
                c.restore()
                c.drawPath(sh.mouthSmile, ink())
            }
            Mood.UNEASY -> c.drawPath(sh.mouthWavy, ink(2.4f))
            Mood.SICK -> {
                c.drawPath(sh.mouthSick, solid(0xFF3E2238.toInt()))
                c.save()
                c.clipPath(sh.mouthSick)
                oval(c, 48.4f, 73.8f, 6f, 2.6f, solid(0xFFC24D74.toInt())) // throat
                c.restore()
                c.drawPath(sh.mouthSick, ink())
                c.save()
                c.rotate(sin(TAU * t / 1.6f) * 0.1f * RAD2DEG, 56.2f, 70.4f)
                c.drawPath(sh.tongueSick, solid(0xFFFF86A6.toInt()))
                c.drawPath(sh.tongueSick, ink(1.9f))
                c.drawPath(sh.tongueSickLine, pen(0xFFD9557A.toInt(), 1.1f))
                c.restore()
            }
        }
        c.restore() // body transform

        // mood effects in screen space
        when (mood) {
            Mood.CALM -> {
                for (i in 0 until 2) {
                    val x = if (i == 0) 19f else 83f
                    val y = if (i == 0) 24f else 28f
                    val s = if (i == 0) 1.05f else 0.85f
                    val ph = if (i == 0) 0f else 0.5f
                    val a = 0.5f + 0.5f * sin(TAU * (t / 1.4f + ph))
                    val sc = s * (0.75f + 0.25f * a)
                    c.save()
                    c.translate(x, y)
                    c.scale(sc, sc)
                    c.drawPath(sh.spark, solid(0xFFFFD84A.toInt()))
                    c.drawPath(sh.spark, ink(1.4f / s))
                    c.restore()
                }
            }
            Mood.UNEASY -> {
                for (i in 0 until 2) {
                    val x = if (i == 0) 83f else 16f
                    val y = if (i == 0) 38f else 41f
                    val s = if (i == 0) 1f else 0.72f
                    val ph = if (i == 0) 0f else 0.45f
                    val drip = posMod(t / 1.8f + ph, 1f)
                    val a = 1f - max(0f, drip - 0.7f) / 0.3f
                    c.save()
                    c.translate(x, y + drip * 5f)
                    c.scale(s, s)
                    c.drawPath(sh.sweat, shaded(sweatShader, a))
                    c.drawPath(sh.sweat, pen(INK, 1.8f / s, a))
                    ovalRot(c, -1.6f, 2.6f, 1f, 1.8f, 0.3f, solid(WHITE, a))
                    c.restore()
                }
                ink(1.8f)
                c.drawPath(sh.nervousL, stroke)
                c.drawPath(sh.nervousR, stroke)
            }
            Mood.SICK -> {
                // two big dizzy stars orbiting above the roof
                for (i in 0 until 2) {
                    val a = t * 2.2f + i * PI
                    val x = 47f + cos(a) * 21f
                    val y = 20f + sin(a) * 4.5f
                    val s = 1.05f + 0.2f * sin(a)
                    c.save()
                    c.translate(x, y)
                    c.scale(s, s)
                    c.drawPath(sh.star5, solid(0xFFFFD84A.toInt()))
                    c.drawPath(sh.star5, ink(1.4f / s))
                    c.restore()
                }
            }
        }
        c.restore()
    }

    // ---------------------------------------------------------------- scenery

    /** Round stage with a wide road running to the horizon. */
    private fun backdrop(c: Canvas, m: Int, pal: Pal, t: Float, speed: Float) {
        val sh = S
        c.save()
        c.drawPath(sh.disc, shaded(sky(m, pal)))
        c.clipPath(sh.disc)
        c.drawRect(0f, 58f, 100f, 100f, solid(pal.ground))
        c.drawPath(sh.road, solid(pal.road))
        c.drawRect(0f, 57.4f, 100f, 58.6f, solid(WHITE, 0.7f))

        // solid edge lines + centre dashes flowing toward the viewer
        val edge = pen(pal.streak, 1.6f, 0.75f)
        edge.strokeCap = Paint.Cap.BUTT
        c.drawLine(42.6f, 58f, -6f, 100f, edge)
        c.drawLine(57.4f, 58f, 106f, 100f, edge)
        for (i in 0 until 3) {
            val u = posMod(i / 3f + t * speed, 1f)
            val z0 = u * u
            val z1 = min(1f, (u + 0.14f) * (u + 0.14f))
            val y0 = 58f + 42f * z0
            val y1 = 58f + 42f * z1
            val w0 = 0.4f + 2.2f * z0
            val w1 = 0.4f + 2.2f * z1
            work.rewind()
            work.moveTo(50f - w0, y0)
            work.lineTo(50f + w0, y0)
            work.lineTo(50f + w1, y1)
            work.lineTo(50f - w1, y1)
            work.close()
            c.drawPath(work, solid(pal.streak, 0.35f + 0.5f * u))
        }

        // bold speed streaks: coloured body, white core, flying outward past the car
        if (speed > 0.5f) {
            var j = 0
            while (j < STREAKS.size) {
                val y = STREAKS[j]
                val s = STREAKS[j + 1]
                val ph = STREAKS[j + 2]
                j += 3
                val u = posMod(ph + t * 1.4f, 1f)
                val a = 30f + u * 14f
                val len = 8f + 6f * sin(u * PI)
                val x0 = 50f + s * a
                val x1 = 50f + s * (a + len)
                val alpha = clamp01(sin(u * PI) * 1.6f)
                c.drawLine(x0, y, x1, y, pen(pal.streak, 3.4f, alpha))
                c.drawLine(x0 + s * 1.2f, y, x1 - s * 0.6f, y, pen(WHITE, 1.4f, alpha))
            }
        }
        c.restore()
    }

    private fun drawSmoke(c: Canvas, t: Float) {
        for (i in 0 until 2) {
            val u = posMod(t / 2.6f + i / 2f, 1f)
            val x = 19f - u * 9f
            val y = 44f - u * 26f
            val r = 3.9f + u * 6.4f
            val ga = if (u < 0.75f) 1f else (1f - u) / 0.25f
            val x2 = x + r * 0.95f
            val y2 = y + r * 0.35f
            val r2 = r * 0.68f
            val x3 = x - r * 0.85f
            val y3 = y + r * 0.4f
            val r3 = r * 0.58f
            solid(INK, ga)
            c.drawCircle(x, y, r + 1.3f, fill)
            c.drawCircle(x2, y2, r2 + 1.3f, fill)
            c.drawCircle(x3, y3, r3 + 1.3f, fill)
            solid(0xFFF2F5F3.toInt(), ga)
            c.drawCircle(x, y, r, fill)
            c.drawCircle(x2, y2, r2, fill)
            c.drawCircle(x3, y3, r3, fill)
            oval(c, x + r * 0.2f, y + r * 0.62f, r * 1.1f, r * 0.3f, solid(0xFFC8CFD6.toInt(), ga))
            c.drawCircle(x - r * 0.3f, y - r * 0.35f, r * 0.32f, solid(WHITE, ga))
        }
    }

    // ---------------------------------------------------------------- car parts

    /** Rounded tyre with a sidewall / rim sliver on its outer side and tread scrolling down. */
    private fun drawTyre(c: Canvas, x: Float, wheel: Float, side: Float) {
        val rx = 6.4f
        val ry = 7f
        c.save()
        c.translate(x, 83f)
        c.drawPath(S.tyre, solid(0xFF3A3A55.toInt()))
        c.save()
        c.clipPath(S.tyre)
        val groove = pen(0xFF2A2942.toInt(), 1.1f)
        groove.strokeCap = Paint.Cap.BUTT
        for (i in 0 until 4) {
            val yy = -ry + posMod(i * 3.6f + wheel * 3.6f, 14.4f)
            c.drawLine(-rx, yy, rx, yy, groove)
        }
        oval(c, side * rx * 0.95f, 0f, rx * 0.5f, ry * 0.95f, solid(0xFF5A5A7A.toInt()))
        oval(c, side * rx * 1.08f, 0f, rx * 0.36f, ry * 0.7f, shaded(rimShader))
        oval(c, side * rx * 0.92f, ry * 0.32f, 0.7f, 1.3f, solid(WHITE, 0.9f))
        c.restore()
        c.drawPath(S.tyre, ink())
        c.restore()
    }

    /** Headlight eye drawn around (0,0); [side] is -1 for the left eye, 1 for the right. */
    private fun drawEye(c: Canvas, mood: Mood, m: Int, pal: Pal, side: Float) {
        val r = ER
        val sh = S
        c.drawCircle(0f, 0f, r + 2.1f, shaded(bezelShader))
        c.drawCircle(0f, 0f, r + 2.1f, ink())
        c.drawPath(sh.lens, shaded(lensShader))

        c.save()
        c.clipPath(sh.lens)
        if (mood == Mood.SICK) {
            c.save()
            c.rotate(spin * side * RAD2DEG)
            c.drawPath(if (side < 0f) sh.spiralL else sh.spiralR, ink(1.9f))
            c.restore()
        } else {
            val gx = gazeX * r * 0.3f
            val gy = gazeY * r * 0.24f
            val ir = r * (if (mood == Mood.UNEASY) 0.56f else 0.72f)
            c.save()
            c.translate(gx, gy)
            c.drawCircle(0f, 0f, ir, shaded(irisGrad(m, pal, ir)))
            c.drawCircle(0f, 0f, ir * 0.5f, solid(INK))
            ovalRot(c, -ir * 0.36f, -ir * 0.4f, ir * 0.38f, ir * 0.31f, -0.5f, solid(WHITE))
            c.drawCircle(ir * 0.42f, ir * 0.42f, ir * 0.16f, fill)
            if (mood == Mood.CALM) {
                c.save()
                c.translate(ir * 0.4f, -ir * 0.52f)
                c.scale(0.42f, 0.42f)
                c.drawPath(sh.spark, fill)
                c.restore()
            }
            c.restore()
        }

        // upper lid (body colour) with a curved lashed edge; outer corner droops
        val top = lidTop
        if (top > 0.01f) {
            c.save()
            c.rotate(lidTilt * side * RAD2DEG)
            val yT = -r - 1f + (1.3f * r + 1f) * top
            val sag = r * (0.34f - 0.22f * max(0f, top - 0.6f) / 0.4f)
            work.rewind()
            work.moveTo(-r - 3f, -r - 4f)
            work.lineTo(r + 3f, -r - 4f)
            work.lineTo(r + 3f, yT)
            work.cubicTo(r * 0.5f, yT + sag * 1.33f, -r * 0.5f, yT + sag * 1.33f, -r - 3f, yT)
            work.close()
            c.drawPath(work, solid(pal.base))
            // darker crease above the lash for volume
            work.rewind()
            work.moveTo(-r * 0.8f, yT - 1.6f)
            work.cubicTo(-r * 0.4f, yT + sag - 1.4f, r * 0.4f, yT + sag - 1.4f, r * 0.8f, yT - 1.6f)
            c.drawPath(work, pen(pal.deep, 1.1f, 0.35f))
            if (blinking) { // lower lid rises to meet it
                val yB = r + 1f - (0.7f * r + 1f) * top
                c.drawRect(-r - 3f, yB, r + 3f, r + 4f, solid(pal.base))
            }
            work.rewind()
            work.moveTo(-r - 3f, yT)
            work.cubicTo(-r * 0.5f, yT + sag * 1.33f, r * 0.5f, yT + sag * 1.33f, r + 3f, yT)
            c.drawPath(work, ink(2.2f))
            if (!blinking) { // lash flick at the outer corner
                val ox = -side * r * 0.92f
                val oy = yT + sag * 0.35f
                c.drawLine(ox, oy, ox - side * 2.2f, oy + 1.2f, ink(1.7f))
            }
            c.restore()
        }
        if (lidBottom > 0.01f) { // happy cheek-lid pushing up from below
            val h = r * 2f * lidBottom
            rect.set(-r * 1.25f, r + h * 0.55f - h, r * 1.25f, r + h * 0.55f + h)
            c.drawOval(rect, solid(pal.base))
            c.drawArc(rect, 1.12f * 180f, 0.76f * 180f, false, ink(1.8f))
        }
        ovalRot(c, -r * 0.45f, -r * 0.55f, r * 0.42f, r * 0.18f, -0.7f, solid(WHITE, 0.55f))
        c.restore() // lens clip
        c.drawPath(sh.lens, ink(1.6f))
    }

    // ---------------------------------------------------------------- helpers

    private fun posMod(v: Float, m: Float): Float {
        val r = v % m
        return if (r < 0f) r + m else r
    }

    private fun clamp01(v: Float) = if (v < 0f) 0f else if (v > 1f) 1f else v
}
