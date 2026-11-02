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
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Collaboration example: the original virtual idol "루미 (LUMI)" on a small neon stage.
 * Shows how the icon slot could host a licensed entertainment IP later. The character is
 * fully original (no real person, group, VTuber or existing character).
 *
 * Bust-up, 3/4 view: milk-tea blonde high ponytail with a mint-to-pink inner-colour streak,
 * see-through bangs, black sequin cropped jacket with hot-pink accents, headset boom mic.
 * Designed in a fixed 0..100 box and scaled by size / 100; paths are parsed once.
 */
object IdolTheme : MoodTheme {
    override val id: String = "idol"
    override val title: String = "가상 아이돌 콜라보"
    override val subtitle: String = "콜라보 예시 · 오리지널 가상 아이돌 루미"
    override val animated: Boolean = true

    override fun label(mood: Mood): String = when (mood) {
        Mood.CALM -> "신나게"
        Mood.UNEASY -> "걱정해"
        Mood.SICK -> "쉬어 가요"
    }

    override fun cueLine(left: Boolean): String =
        if (left) "루미가 먼저 알려 줄게요. 곧 왼쪽으로 돌아요!"
        else "루미가 먼저 알려 줄게요. 곧 오른쪽으로 돌아요!"

    // ---------------------------------------------------------------- math helpers
    private const val TAU = 6.2831855f
    private const val PI_F = 3.1415927f
    private fun deg(rad: Float): Float = rad * 57.29578f
    private fun lerp(a: Float, b: Float, k: Float): Float = a + (b - a) * k
    private fun hex(s: String): Int = Color.parseColor(s)
    private fun rgba(r: Int, g: Int, b: Int, a: Float): Int =
        Color.argb((a.coerceIn(0f, 1f) * 255f).roundToInt(), r, g, b)

    // ---------------------------------------------------------------- palette
    private val OL = hex("#3A2038")
    private val IL = rgba(90, 50, 70, 0.55f)
    private val SKIN = hex("#FFEFE7")
    private val SKIN_SHADE = hex("#F4C9BB")
    private val SKIN_SICK = hex("#FAEFEC")
    private val HAIR = hex("#FBDDA8")
    private val HAIR_MID = hex("#F2C48A")
    private val HAIR_ROOT = hex("#DFA06C")
    private val HAIR_SHADE = hex("#D08F63")
    private val HAIR_LINE = rgba(150, 88, 58, 0.55f)
    private val MINT = hex("#6FE3CF")
    private val STREAK_PINK = hex("#FF8FC4")
    private val JACKET = hex("#262338")
    private val JACKET_HI = hex("#4A4566")
    private val ACCENT = hex("#FF3D8B")
    private val ACCENT_HI = hex("#FF8DBE")
    private val SILVER = hex("#E8ECF6")
    private val GOLD = hex("#FFD25A")
    private val GOLD_DEEP = hex("#D99A22")
    private val MOUTH = hex("#D43F6E")
    private val TONGUE = hex("#FF8FA8")
    private val TOWEL_STRIPE = hex("#6FB2EE")
    private val TOWEL_EDGE = hex("#A9CDEE")
    private val WHITE = Color.WHITE
    private val BROW = hex("#8A5238")
    private val C_24103A = hex("#24103A")
    private val C_2C4A8C = hex("#2C4A8C")
    private val C_2F7FBD = hex("#2F7FBD")
    private val C_4F78A8 = hex("#4F78A8")
    private val C_6A55B8 = hex("#6A55B8")
    private val C_6A5A8E = hex("#6A5A8E")
    private val C_6FAEE6 = hex("#6FAEE6")
    private val C_7FE3FF = hex("#7FE3FF")
    private val C_8DB9E6 = hex("#8DB9E6")
    private val C_B8235F = hex("#B8235F")
    private val C_BDEBFF = hex("#BDEBFF")
    private val C_E3D9E2 = hex("#E3D9E2")
    private val C_FF2E8E = hex("#FF2E8E")
    private val C_FF5AA8 = hex("#FF5AA8")
    private val C_FF9AD5 = hex("#FF9AD5")
    private val C_FFB3C8 = hex("#FFB3C8")
    private val C_FFD24A = hex("#FFD24A")
    private val C_FFE2D8 = hex("#FFE2D8")
    private val C_FFF6C8 = hex("#FFF6C8")
    private val HEADSET = hex("#1E1628")

    /** Stage colours per mood: centre, mid, edge, beam rgb, ring1, ring2, rim light. */
    private class Stage(
        val c0: Int, val c1: Int, val c2: Int, val beam: Int,
        val ring1: Int, val ring2: Int, val rim: Int,
    )

    private val STAGES = arrayOf(
        Stage(hex("#FFE6F4"), hex("#FF6DB8"), hex("#5A1E9E"), rgba(255, 240, 250, 1f),
            hex("#FFD6F0"), hex("#FF4FA7"), hex("#FF7CC8")),
        Stage(hex("#EEE6FF"), hex("#AE95E6"), hex("#45367E"), rgba(240, 232, 255, 1f),
            hex("#E6DAFF"), hex("#9C83E0"), hex("#B89BFF")),
        Stage(hex("#E2F1FF"), hex("#86B6EA"), hex("#2C4A8C"), rgba(230, 244, 255, 1f),
            hex("#D8ECFF"), hex("#6E9EE0"), hex("#7EC4FF")),
    )

    // ---------------------------------------------------------------- static paths (0..100 box)
    private fun p(d: String): Path = PathParser.createPathFromPathData(d)

    private val PONY by lazy { p("M37 21.5 C31 13.4 20.4 13.8 16.6 21.4 C13.6 27.6 17 33.6 14.6 41.4 C12.4 48.6 16.6 54.6 13.6 62.4 C11.6 67.8 12.8 74.4 17.4 80.4 C17.4 75.8 19.2 72.6 20.4 68.4 C22 62.6 19.8 57 22 50.4 C24 44.2 22.2 37.6 24.8 31.6 C26.8 27.4 31 25.2 36.4 26.6 Z") }
    private val PONY_STREAK by lazy { p("M24.8 31.6 C22.2 37.6 24 44.2 22 50.4 C19.8 57 22 62.6 20.4 68.4 C19.2 72.6 17.4 75.8 17.4 80.4 C15.6 76 15.4 71.4 17 67 C18.6 62 16.4 56.6 18.4 50.4 C20.4 44 18.4 37.6 21.4 31.6 C22.6 29.4 24 28.6 25.6 28.4 Z") }
    private val PONY_SHADE by lazy { p("M36.4 26.6 C31 25.2 26.8 27.4 24.8 31.6 C22.2 37.6 24 44.2 22 50.4 C20 56.4 21.6 61.6 20.8 66.4 C26 60 25 52 27 46 C29 39 30 31 36.4 26.6 Z") }
    private val HAIR_BACK by lazy { p("M28.4 45 C27.2 29.4 36.6 18.2 50.6 18.2 C64.6 18.2 74 29.4 72.4 45 C72 50 71 54.4 69.6 58 L31.2 58 C29.6 54.4 28.6 50 28.4 45 Z") }
    private val HAIR_BACK_R by lazy { p("M66 46 C70.4 51 71.6 57 70.8 62.6 C70.4 66.4 71.6 69.8 74 72.4 C69.6 73 66.2 70.8 64.4 67 Z") }
    private val HAIR_BACK_R_SHADE by lazy { p("M64 46 L67 46 C68.6 54 68.4 62 71 72 L64 72 Z") }
    private val FACE by lazy { p("M30.6 42 C30.3 50 32.9 56.4 37.6 61.3 C42 65.6 47.5 68.2 52 68.2 C55.6 68.2 59.6 65 63.4 60.4 C67.2 55.8 69.4 50 69.4 42 C69.4 29.8 61.2 22.6 50 22.6 C38.8 22.6 30.6 29.8 30.6 42 Z") }
    private val JAW_SHADE_R by lazy { p("M69.4 44 C69.4 51 67.4 56.4 63.4 60.4 C59.6 65 55.6 68.2 52 68.2 C56 66.4 59.6 62.6 62.4 58.4 C65.4 53.6 66.8 49 67 44 Z") }
    private val NECK by lazy { p("M44.6 58 L55.6 58 L56.8 78 L43.4 78 Z") }
    private val NECK_SHADE by lazy { p("M44.4 61 C47 65.6 49.6 68.6 52 68.6 C54 68.6 55 66.8 55.7 64.6 L56 72.4 C52.6 74.4 47.4 74 44.2 71.6 Z") }
    private val CHEST by lazy { p("M15 104 C15 90 23 82.6 35 80.2 C39.4 79.3 42.4 77.8 43.5 75.6 L56.6 75.6 C57.7 77.8 60.6 79.3 65 80.2 C77 82.6 85 90 85 104 Z") }
    private val INNER_TOP by lazy { p("M30.5 89.4 C40 87.6 60 87.6 69.5 89.4 L72 104 L28 104 Z") }
    private val JACKET_L by lazy { p("M39.6 80.2 C33 80.6 25.6 80.8 19.8 81.2 C17.6 81.4 16.4 82.6 15.8 84.4 C14.4 89.4 14 96 14 104 L32.6 104 C35.4 97.6 37.6 90 39.6 80.2 Z") }
    private val JACKET_R by lazy { p("M60.4 80.2 C67 80.6 74.4 80.8 80.2 81.2 C82.4 81.4 83.6 82.6 84.2 84.4 C85.6 89.4 86 96 86 104 L67.4 104 C64.6 97.6 62.4 90 60.4 80.2 Z") }
    private val PIPE_L by lazy { p("M32.6 104 C35.4 97.6 37.6 90 39.6 80.2 M18.6 81.6 L31 80.8") }
    private val PIPE_R by lazy { p("M67.4 104 C64.6 97.6 62.4 90 60.4 80.2 M81.4 81.6 L69 80.8") }
    private val BANGS_TOP by lazy { p("M28.6 43 C27.4 27.4 37 18.6 50.6 18.6 C64.2 18.6 73.8 27.6 72 43 C70.2 36 66.4 31.8 60.4 31 C54.4 30.2 45.6 30.2 39.8 31 C34 31.8 30.2 35.8 28.6 43 Z") }
    private val HAIR_SHADE_TOP by lazy { p("M60 19.6 C67.4 22.4 72.8 30 72 43 C70.2 36 66.4 31.8 60.4 31 L58 31 C62 27.4 62.6 23 60 19.6 Z") }
    private val LOCK_L by lazy { p("M32 36 C29 42 28.4 48 30 53 C31.4 57.4 30.4 62 27.8 66.6 C31.6 64.2 33.6 60 33.2 55.4 C32.8 50.6 31.6 46 34 39 Z") }
    private val LOCK_R by lazy { p("M68 36 C71.2 42 71.8 48 70.2 53 C68.8 57.4 69.8 62 72.4 66.4 C68.6 64 66.6 60 67 55.4 C67.4 50.6 68.6 46 66.4 39 Z") }
    private val RIBBON_L by lazy { p("M0 0 C-2.2 -3.6 -6.6 -4.6 -7.4 -2 C-8 0.2 -5 2.2 0 0 Z") }
    private val RIBBON_R by lazy { p("M0 0 C2.2 -3.4 6.2 -4 6.8 -1.6 C7.2 0.4 4.6 2 0 0 Z") }
    private val RIBBON_TAIL by lazy { p("M-0.6 0.4 C-1.8 3.6 -3.4 6.6 -5.8 9 L-3.4 9.2 L-2.6 11.2 C-0.8 8 0 4.4 0.6 0.6 Z") }
    private val EYE_WHITE by lazy { p("M-5.5 0.8 C-5.6 -3.8 -2.6 -5.8 0.6 -5.8 C3.9 -5.8 5.9 -3.4 5.9 0.2 C5.9 4.2 3.1 6.6 0.1 6.6 C-3.1 6.6 -5.4 4.4 -5.5 0.8 Z") }
    private val LASH_TOP by lazy { p("M-6.1 0.6 C-6.1 -4.4 -2.8 -6.6 0.6 -6.5 C3.8 -6.4 6.2 -4.8 7.4 -2.4 L8.8 -3.6 C7.6 -6.8 4.6 -8.4 0.6 -8.3 C-3.8 -8.2 -6.9 -5.4 -6.9 -0.4 Z") }
    private val LASH_FLICK by lazy { p("M6.2 -4.6 L10 -6.8 L7.8 -3 Z") }
    private val WINK_LASH by lazy { p("M-5.6 1.4 Q0.2 -5.6 6.2 0.4 L7.8 -1.8 L6.9 1.6 Q0.2 -3.4 -5.6 1.4 Z") }
    private val HEART by lazy { p("M0 3.2 C-1.2 2.2 -4 0.4 -4 -1.6 C-4 -3.2 -2.6 -4 -1.5 -4 C-0.8 -4 -0.3 -3.6 0 -3 C0.3 -3.6 0.8 -4 1.5 -4 C2.6 -4 4 -3.2 4 -1.6 C4 0.4 1.2 2.2 0 3.2 Z") }
    private val STAR4 by lazy { p("M0 -1 Q0.16 -0.16 1 0 Q0.16 0.16 0 1 Q-0.16 0.16 -1 0 Q-0.16 -0.16 0 -1 Z") }
    private val STAR5 by lazy { p("M0 -1 L0.24 -0.33 L0.95 -0.31 L0.38 0.12 L0.59 0.81 L0 0.4 L-0.59 0.81 L-0.38 0.12 L-0.95 -0.31 L-0.24 -0.33 Z") }
    private val DROP by lazy { p("M0 -4 C1 -2.2 2.6 -0.6 2.6 1.2 C2.6 2.8 1.4 3.9 0 3.9 C-1.4 3.9 -2.6 2.8 -2.6 1.2 C-2.6 -0.6 -1 -2.2 0 -4 Z") }
    private val GEM by lazy { p("M0 -1.6 L1.1 -0.2 L0 1.9 L-1.1 -0.2 Z") }
    private val MITTEN by lazy { p("M-3.4 4.6 C-4.2 0.6 -4.4 -4.4 -3.4 -8 C-2.6 -10.8 2.4 -11 3.2 -8.2 C4 -5 4.2 0.4 3.6 4.6 Z") }
    private val TOWEL_BACK by lazy { p("M55.6 76.4 C62 75.2 70.4 76.6 77 80 C80 81.6 81.4 83.6 81 85.8 L73 87.6 C70 84.6 64 82.8 58 82.6 Z") }
    private val TOWEL_FRONT by lazy { p("M57.6 79.6 C62 79.4 65.8 81.2 67.6 84.6 L69.2 91.6 L59.6 92.6 C58.6 88.4 57.8 84 57.6 79.6 Z") }
    private val PATCH by lazy { p("M-7.4 -2.6 Q-7.4 -3.8 -6 -3.8 L6 -3.8 Q7.4 -3.8 7.4 -2.6 L7.4 2.6 Q7.4 3.8 6 3.8 L-6 3.8 Q-7.4 3.8 -7.4 2.6 Z") }
    private val SNOW_ARM by lazy { p("M-2.2 0 L2.2 0 M1.4 0 L2 -0.6 M1.4 0 L2 0.6 M-1.4 0 L-2 -0.6 M-1.4 0 L-2 0.6") }
    private val ZZ1 by lazy { p("M0 0 L4.6 0 L0 5 L4.6 5") }
    private val MOUTH_CALM by lazy { p("M47.8 59.8 Q52.4 61.2 56.6 59.4 Q55.8 65 52.2 65.2 Q48.6 65 47.8 59.8 Z") }
    // see-through bangs: separate tapered strands
    private val STRANDS: Array<Path> by lazy {
        arrayOf(
            "M31.9 30 Q28.9 37.8 30.6 45.6 Q32.4 37.8 39.1 30 Z",
            "M37.6 29 Q35.9 36.3 37.4 43.6 Q39.1 36.3 44 29 Z",
            "M43 28.4 Q42.6 35.8 43.8 43.2 Q45.8 35.8 49.4 28.4 Z",
            "M48.9 28.4 Q50.4 36.1 50.6 43.8 Q53.5 36.1 55.1 28.4 Z",
            "M54.4 28.8 Q58.5 35.8 59.4 42.8 Q61.5 35.8 60.4 28.8 Z",
            "M59.6 29.6 Q64.6 36.7 66 43.8 Q67.6 36.7 65.6 29.6 Z",
            "M64.6 31.6 Q68.5 39.1 70 46.6 Q71.1 39.1 69.8 31.6 Z",
        ).map { p(it) }.toTypedArray()
    }
    private val CLIP_CIRCLE by lazy { Path().apply { addCircle(50f, 50f, 46f, Path.Direction.CW) } }
    // dizzy swirl: chained half-circles with shrinking radius
    private val SWIRL by lazy {
        Path().apply {
            var r = 4.4f
            var cx = 0f
            moveTo(r, 0f)
            for (i in 0 until 5) {
                arcTo(RectF(cx - r, -r, cx + r, r), if (i % 2 == 1) 180f else 0f, 180f, false)
                r -= 0.85f
                cx = if (i % 2 == 1) 0f else -0.85f
            }
        }
    }

    // ---------------------------------------------------------------- shaders (unit space, size-independent)
    private fun lin(x0: Float, y0: Float, x1: Float, y1: Float, colors: IntArray, pos: FloatArray?): Shader =
        LinearGradient(x0, y0, x1, y1, colors, pos, Shader.TileMode.CLAMP)

    private fun rad(x: Float, y: Float, r: Float, colors: IntArray, pos: FloatArray?): Shader =
        RadialGradient(x, y, r, colors, pos, Shader.TileMode.CLAMP)

    private val hairGrad by lazy {
        lin(0f, 18f, 0f, 82f, intArrayOf(HAIR_ROOT, HAIR_MID, HAIR, hex("#FFE6CF")), floatArrayOf(0f, 0.22f, 0.45f, 1f))
    }
    private val ponyGrad by lazy {
        lin(14f, 16f, 22f, 80f, intArrayOf(HAIR_MID, HAIR, hex("#FFD9C4")), floatArrayOf(0f, 0.4f, 1f))
    }
    private val streakGrad by lazy { lin(0f, 30f, 0f, 80f, intArrayOf(MINT, STREAK_PINK), null) }
    private val innerTopGrad by lazy { lin(0f, 88f, 0f, 100f, intArrayOf(ACCENT, hex("#B8237A")), null) }
    private val jacketGrad by lazy {
        lin(0f, 80f, 0f, 100f, intArrayOf(JACKET_HI, JACKET, hex("#15131F")), floatArrayOf(0f, 0.35f, 1f))
    }
    private fun sheen(side: Int): Shader = lin(
        50f + side * 18f, 82f, 50f + side * 30f, 100f,
        intArrayOf(rgba(255, 120, 200, 0f), rgba(255, 140, 210, 0.45f), rgba(120, 230, 255, 0.45f), rgba(120, 230, 255, 0f)),
        floatArrayOf(0f, 0.4f, 0.6f, 1f),
    )
    private val sheenL by lazy { sheen(-1) }
    private val sheenR by lazy { sheen(1) }
    private val ribbonGrad by lazy { lin(0f, -4f, 0f, 4f, intArrayOf(ACCENT_HI, ACCENT), null) }
    private val starGrad by lazy { lin(0f, -1f, 0f, 1f, intArrayOf(hex("#FFF6C2"), GOLD), null) }
    private val gemGrad by lazy {
        lin(-1f, -1.6f, 1f, 1.9f, intArrayOf(WHITE, hex("#BDEBFF"), hex("#FF9BD0")), floatArrayOf(0f, 0.5f, 1f))
    }
    private val earGrad by lazy { lin(0f, 45f, 0f, 55f, intArrayOf(hex("#4A3D5E"), HEADSET), null) }
    private val dropGrad by lazy { lin(0f, -4f, 0f, 4f, intArrayOf(hex("#E2F6FF"), hex("#5DB8F2")), null) }
    private val patchGrad by lazy {
        lin(0f, -3.8f, 0f, 3.8f, intArrayOf(rgba(240, 250, 255, 0.92f), rgba(196, 228, 252, 0.88f)), null)
    }
    private val towelGrad by lazy { lin(0f, 76f, 0f, 92f, intArrayOf(WHITE, hex("#E3F0FC")), null) }
    private val heartGrad by lazy { lin(0f, -4f, 0f, 3f, intArrayOf(hex("#FF9ACD"), hex("#FF1F84")), null) }
    private val lidShade by lazy { lin(0f, -6f, 0f, -1.5f, intArrayOf(rgba(70, 30, 70, 0.4f), rgba(70, 30, 70, 0f)), null) }
    private val irisUneasy by lazy {
        lin(0f, -5f, 0f, 6f, intArrayOf(hex("#2A1A4E"), hex("#6448B4"), hex("#BDA2FF"), hex("#E8DCFF")),
            floatArrayOf(0f, 0.5f, 0.9f, 1f))
    }
    private val irisBright by lazy {
        lin(0f, -5f, 0f, 6f, intArrayOf(hex("#3A1650"), hex("#A0399E"), hex("#FF86C8"), hex("#FFD0EC")),
            floatArrayOf(0f, 0.45f, 0.85f, 1f))
    }
    private val keyLight by lazy {
        rad(40f, 23f, 14f, intArrayOf(rgba(255, 250, 235, 0.55f), rgba(255, 250, 235, 0f)), null)
    }
    // full-alpha gradients whose strength is set through the paint alpha (stops share one rgb)
    private val blushL by lazy { rad(38.6f, 57.2f, 5f, intArrayOf(rgba(255, 110, 150, 1f), rgba(255, 110, 150, 0f)), null) }
    private val blushR by lazy { rad(62.6f, 57.2f, 4.2f, intArrayOf(rgba(255, 110, 150, 1f), rgba(255, 110, 150, 0f)), null) }
    private val tintL by lazy { rad(41.2f, 54.4f, 5.4f, intArrayOf(rgba(110, 190, 185, 1f), rgba(110, 190, 185, 0f)), null) }
    private val tintR by lazy { rad(60.4f, 54.4f, 4.6f, intArrayOf(rgba(110, 190, 185, 1f), rgba(110, 190, 185, 0f)), null) }
    private val floorGlow by lazy { rad(50f, 104f, 46f, intArrayOf(WHITE, rgba(255, 255, 255, 0f)), null) }

    private val bgCache = arrayOfNulls<Shader>(3)
    private val beamCache = arrayOfNulls<Shader>(3)
    private val ringCache = arrayOfNulls<Shader>(3)

    private fun bgShader(m: Mood): Shader = bgCache[m.ordinal] ?: STAGES[m.ordinal].let { st ->
        rad(50f, 30f, 62f, intArrayOf(st.c0, st.c1, st.c2), floatArrayOf(0f, 0.38f, 1f))
    }.also { bgCache[m.ordinal] = it }

    private fun beamShader(m: Mood): Shader = beamCache[m.ordinal] ?: STAGES[m.ordinal].let { st ->
        lin(0f, 0f, 0f, 100f, intArrayOf(st.beam, st.beam and 0x00FFFFFF), floatArrayOf(0f, 0.75f))
    }.also { beamCache[m.ordinal] = it }

    private fun ringShader(m: Mood): Shader = ringCache[m.ordinal] ?: STAGES[m.ordinal].let { st ->
        lin(0f, 0f, 100f, 100f, intArrayOf(st.ring1, st.ring2, st.ring1), floatArrayOf(0f, 0.5f, 1f))
    }.also { ringCache[m.ordinal] = it }

    // ---------------------------------------------------------------- preallocated drawing objects
    private val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    // glow passes (setShadowLayer), solid colour only
    private val shf = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shs = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val tmpPath = Path()
    private val mtx = Matrix()
    private val rect = RectF()
    private val bokehColors = IntArray(3)
    private val bokehStops = floatArrayOf(0f, 0.7f, 1f)

    /** Global alpha, like CanvasRenderingContext2D.globalAlpha (applied by the paint helpers). */
    private var ga = 1f

    private fun ac(color: Int): Int {
        if (ga >= 1f) return color
        val a = (Color.alpha(color) * ga.coerceAtLeast(0f)).roundToInt()
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    private fun fillC(color: Int): Paint { fp.shader = null; fp.color = ac(color); return fp }
    private fun fillS(sh: Shader, alpha: Float = 1f): Paint {
        fp.shader = sh
        fp.color = Color.argb(((alpha * ga).coerceIn(0f, 1f) * 255f).roundToInt(), 0, 0, 0)
        return fp
    }
    private fun strokeC(color: Int, w: Float): Paint { sp.shader = null; sp.color = ac(color); sp.strokeWidth = w; return sp }
    private fun strokeS(sh: Shader, w: Float): Paint { sp.shader = sh; sp.color = ac(WHITE); sp.strokeWidth = w; return sp }

    /** Android blur radius for a canvas shadowBlur of [blur] units (sigma = blur / 2). */
    private fun shadowR(blur: Float): Float = max(0.01f, (blur * 0.5f - 0.5f) / 0.57735f)

    private fun oval(x: Float, y: Float, rx: Float, ry: Float): RectF { rect.set(x - rx, y - ry, x + rx, y + ry); return rect }
    private fun ovalR(c: Canvas, x: Float, y: Float, rx: Float, ry: Float, rotRad: Float, paint: Paint) {
        c.save(); c.rotate(deg(rotRad), x, y); c.drawOval(oval(x, y, rx, ry), paint); c.restore()
    }
    private fun quad(c: Canvas, x0: Float, y0: Float, qx: Float, qy: Float, x1: Float, y1: Float, paint: Paint) {
        path.reset(); path.moveTo(x0, y0); path.quadTo(qx, qy, x1, y1); c.drawPath(path, paint)
    }
    private fun rim(c: Canvas, d: Path, color: Int, w: Float, x0: Float, x1: Float) {
        c.save(); c.clipPath(d); c.clipRect(x0, 0f, x1, 110f); c.drawPath(d, strokeC(color, w)); c.restore()
    }
    private fun sparkle(c: Canvas, x: Float, y: Float, r: Float, color: Int, alpha: Float) {
        val g0 = ga
        ga *= alpha
        c.save(); c.translate(x, y); c.scale(r, r); c.drawPath(STAR4, fillC(color)); c.restore()
        ga = g0
    }
    private fun capsule(c: Canvas, x0: Float, y0: Float, x1: Float, y1: Float, w: Float, fill: Int, line: Int, lw: Float) {
        c.drawLine(x0, y0, x1, y1, strokeC(line, w + lw * 2f))
        c.drawLine(x0, y0, x1, y1, strokeC(fill, w))
    }

    // ================================================================== draw
    override fun draw(c: Canvas, cx: Float, cy: Float, size: Float, mood: Mood, level: Float, t: Float) {
        if (size <= 0f) return
        val s = size / 100f
        val k = when (mood) {
            Mood.CALM -> (level / 0.5f).coerceIn(0f, 1f)
            Mood.UNEASY -> ((level - 0.4f) / 0.6f).coerceIn(0f, 1f)
            Mood.SICK -> ((level - 1f) / 0.5f).coerceIn(0f, 1f)
        }
        ga = 1f
        c.save()
        c.translate(cx - size / 2f, cy - size / 2f)
        c.scale(s, s)
        drawStage(c, mood, k, t)
        c.save()
        c.clipPath(CLIP_CIRCLE)
        val sway = if (mood == Mood.SICK) sin(t * TAU / 4f) * (0.8f + 0.8f * k) else sin(t * TAU / 3f) * 1.4f
        c.rotate(sway, 50f, 104f)
        if (mood == Mood.SICK) c.translate(0f, 1f + 2f * k)
        drawLumi(c, mood, k, t)
        c.restore()
        drawRim(c, mood)
        drawFront(c, mood, k, t)
        c.restore()
        ga = 1f
    }

    // ---------------------------------------------------------------- stage
    private val BOKEH = floatArrayOf(18f, 30f, 5f, 82f, 26f, 4f, 14f, 62f, 3.5f, 86f, 58f, 5.5f,
        26f, 14f, 2.5f, 74f, 12f, 3f, 90f, 40f, 2.4f, 10f, 44f, 2.6f)
    private val CONF_POS = floatArrayOf(16f, 22f, 20f, 30f, 9f, -30f, 70f, 8f, 40f, 86f, 34f, 10f,
        12f, 50f, -20f, 88f, 70f, 60f, 22f, 76f, 30f, 62f, 4f, -50f)
    private val CONF_COL by lazy {
        intArrayOf(C_FFD24A, WHITE, C_7FE3FF, WHITE, C_FF9AD5, C_FFD24A, C_7FE3FF, C_FF9AD5)
    }

    private fun drawStage(c: Canvas, mood: Mood, k: Float, t: Float) {
        val dim = when (mood) {
            Mood.UNEASY -> 0.1f + 0.45f * k
            Mood.SICK -> 0.25f + 0.4f * k
            Mood.CALM -> 0f
        }
        c.save()
        c.clipPath(CLIP_CIRCLE)
        c.drawRect(0f, 0f, 100f, 100f, fillS(bgShader(mood)))
        val sw = sin(t * TAU / 5f) * 7f
        val beamA = (if (mood == Mood.CALM) 0.55f else 0.4f) * (1f - dim)
        for (i in 0..2) {
            val ang = when (i) { 0 -> -28f + sw; 1 -> -sw * 0.6f; else -> 28f + sw }
            val spread = if (i == 1) 28f else 22f
            c.save()
            c.translate(50f + (i - 1) * 18f, -6f)
            c.rotate(ang)
            path.reset()
            path.moveTo(-2f, 0f); path.lineTo(2f, 0f)
            path.lineTo(spread / 2f, 100f); path.lineTo(-spread / 2f, 100f); path.close()
            c.drawPath(path, fillS(beamShader(mood), beamA))
            c.restore()
        }
        for (i in 0 until 8) {
            val x = BOKEH[i * 3]; val y = BOKEH[i * 3 + 1]; val r = BOKEH[i * 3 + 2]
            val a = (0.28f + 0.18f * sin(t * TAU / 2.6f + i * 1.7f)) * (1f - dim)
            bokehColors[0] = rgba(255, 255, 255, a + 0.2f)
            bokehColors[1] = rgba(255, 255, 255, a)
            bokehColors[2] = rgba(255, 255, 255, 0f)
            c.drawOval(oval(x, y, r, r), fillS(rad(x, y, r, bokehColors, bokehStops)))
        }
        c.drawRect(0f, 60f, 100f, 100f, fillS(floorGlow, 0.5f * (1f - dim)))
        if (mood != Mood.SICK) {
            val n = if (mood == Mood.CALM) 8 else 3
            for (i in 0 until n) {
                val x = CONF_POS[i * 3]; val y = CONF_POS[i * 3 + 1]; val r0 = CONF_POS[i * 3 + 2]
                val fy = (y + t * 3f + i * 7f) % 90f + 4f
                c.save()
                c.translate(x, fy)
                c.rotate(r0 + t * 90f * (if (i % 2 == 1) 1f else -1f))
                ga = 0.9f * (1f - dim)
                c.drawRect(-1.3f, -0.6f, 1.3f, 0.6f, fillC(CONF_COL[i]))
                ga = 1f
                c.restore()
            }
        }
        // dimming veil (level-driven)
        if (dim > 0f) {
            val veil = if (mood == Mood.SICK) rgba(20, 34, 80, dim * 0.55f) else rgba(40, 24, 80, dim * 0.55f)
            c.drawRect(0f, 0f, 100f, 100f, fillC(veil))
        }
        c.restore()
    }

    private fun drawRim(c: Canvas, mood: Mood) {
        val st = STAGES[mood.ordinal]
        shs.color = st.ring2
        shs.strokeWidth = 2.4f
        shs.setShadowLayer(shadowR(3f), 0f, 0f, st.ring2)
        c.drawCircle(50f, 50f, 46.4f, shs)
        c.drawCircle(50f, 50f, 46.4f, strokeS(ringShader(mood), 2.4f))
    }

    // ---------------------------------------------------------------- character
    private fun headTilt(mood: Mood, k: Float, t: Float): Float = when (mood) {
        Mood.SICK -> -(4f + 11f * k)
        Mood.UNEASY -> 3f + 5f * k
        Mood.CALM -> 4f + sin(t * TAU / 3f) * 1.2f
    }

    private const val BY = -3.5f

    private fun drawLumi(c: Canvas, mood: Mood, k: Float, t: Float) {
        val sick = mood == Mood.SICK
        val uneasy = mood == Mood.UNEASY
        val calm = mood == Mood.CALM
        val rimCol = STAGES[mood.ordinal].rim
        val tilt = headTilt(mood, k, t)
        val swing = sin(t * TAU / 3f + 0.6f) * 1.5f

        // ===== outline pass: thick stroke of the silhouette only (fills below cover the inner half)
        val ol = strokeC(OL, 2f)
        c.save(); c.rotate(tilt, 50f, 68f); c.rotate(swing, 35f, 22f); c.drawPath(PONY, ol); c.restore()
        c.save(); c.translate(0f, BY); c.drawPath(JACKET_L, ol); c.drawPath(JACKET_R, ol); c.drawPath(CHEST, ol); c.restore()
        c.save(); c.rotate(tilt, 50f, 68f)
        c.drawPath(HAIR_BACK, ol); c.drawPath(HAIR_BACK_R, ol); c.drawPath(FACE, ol)
        c.drawPath(BANGS_TOP, ol); c.drawPath(LOCK_L, ol); c.drawPath(LOCK_R, ol)
        c.restore()

        // ===== ponytail (behind everything)
        c.save(); c.rotate(tilt, 50f, 68f); c.rotate(swing, 35f, 22f)
        c.drawPath(PONY, fillS(ponyGrad))
        c.save(); c.clipPath(PONY)
        c.drawPath(PONY_SHADE, fillC(HAIR_SHADE))
        c.drawPath(PONY_STREAK, fillS(streakGrad))
        val hl = strokeC(HAIR_LINE, 0.4f)
        quad(c, 30f, 18f, 20f, 26f, 19f, 40f, hl)
        quad(c, 26f, 17f, 17f, 30f, 17.4f, 52f, hl)
        quad(c, 22f, 30f, 19.6f, 46f, 16f, 64f, hl)
        quad(c, 19.4f, 22.4f, 23f, 17.4f, 29f, 18.4f, strokeC(rgba(255, 255, 255, 0.8f), 1.2f))  // shine on the bulb
        c.restore()
        rim(c, PONY, rimCol, 2.2f, 0f, 19f)
        c.restore()

        // ===== neck (cast shadow under the jaw)
        c.drawPath(NECK, fillC(if (sick) SKIN_SICK else SKIN))
        c.save(); c.clipPath(NECK); c.rotate(tilt * 0.6f, 50f, 68f); c.drawPath(NECK_SHADE, fillC(SKIN_SHADE)); c.restore()

        // ===== body: chest skin, sequin top, cropped jacket (raised by BY for a short neck)
        c.save(); c.translate(0f, BY)
        c.drawPath(CHEST, fillC(if (sick) SKIN_SICK else SKIN))
        c.save(); c.clipPath(CHEST)
        val cb = strokeC(SKIN_SHADE, 0.6f)  // collarbones
        quad(c, 38.6f, 83.2f, 43.4f, 84.6f, 47.2f, 83.6f, cb)
        quad(c, 61.4f, 83.2f, 56.6f, 84.6f, 52.8f, 83.6f, cb)
        c.restore()
        c.drawPath(INNER_TOP, fillS(innerTopGrad))
        val rh = fillC(rgba(255, 255, 255, 0.9f))  // rhinestone neckline
        for (i in 0..16) {
            val x = 32f + 2.2f * i
            c.drawOval(oval(x, 89f - 1.3f * sin((x - 30.5f) / 39f * PI_F), 0.55f, 0.55f), rh)
        }
        for (side in intArrayOf(-1, 1)) {
            val d = if (side < 0) JACKET_L else JACKET_R
            c.drawPath(d, fillS(jacketGrad))
            c.save(); c.clipPath(d)
            c.drawRect(0f, 78f, 100f, 108f, fillS(if (side < 0) sheenL else sheenR))  // holographic sheen band
            for (i in 0 until 9) {
                val sx = SEQ[i * 2]
                val x = if (side < 0) sx else 100f - sx
                val tw = 0.5f + 0.5f * sin(t * TAU / 1.4f + i * 2.1f + side)
                c.drawOval(oval(x, SEQ[i * 2 + 1], 0.65f, 0.65f), fillC(rgba(232, 236, 246, 0.35f + 0.55f * tw)))
            }
            c.drawPath(if (side < 0) PIPE_L else PIPE_R, strokeC(ACCENT, 1.4f))  // hot-pink piping
            c.restore()
        }
        rim(c, JACKET_R, rimCol, 2.0f, 70f, 100f)
        rim(c, JACKET_L, rimCol, 1.4f, 0f, 18f)
        // necklace: thin silver chain + star pendant
        quad(c, 44.4f, 77.4f, 50f, 83.6f, 55.8f, 77.4f, strokeC(SILVER, 0.45f))
        c.save(); c.translate(50.1f, 81.6f); c.scale(1.3f, 1.3f); c.drawPath(STAR5, fillC(WHITE)); c.restore()
        c.restore()

        // ===== SICK towel over the right shoulder
        if (sick) { c.save(); c.translate(0f, BY); drawTowel(c, k); c.restore() }

        // ===== head
        c.save(); c.rotate(tilt, 50f, 68f)
        c.drawPath(HAIR_BACK, fillS(hairGrad))
        c.save(); c.clipPath(HAIR_BACK); c.drawRect(28f, 44f, 74f, 60f, fillC(HAIR_SHADE)); c.restore()
        c.drawPath(HAIR_BACK_R, fillS(hairGrad))
        c.save(); c.clipPath(HAIR_BACK_R); c.drawPath(HAIR_BACK_R_SHADE, fillC(HAIR_SHADE)); c.restore()
        rim(c, HAIR_BACK_R, rimCol, 2.0f, 66f, 80f)

        // far ear peeks out
        ovalR(c, 69.3f, 49.4f, 1.8f, 3.2f, 0.15f, fillC(SKIN))
        ovalR(c, 69.3f, 49.4f, 1.8f, 3.2f, 0.15f, strokeC(IL, 0.4f))

        // face
        c.drawPath(FACE, fillC(if (sick) SKIN_SICK else SKIN))
        c.save(); c.clipPath(FACE)
        c.drawPath(JAW_SHADE_R, fillC(if (sick) rgba(200, 200, 220, 0.55f) else rgba(244, 201, 187, 0.75f)))
        // cast shadow of the bangs: same strands shifted down/right, plus the top mass
        c.save(); c.translate(0.7f, 1.8f)
        val bs = fillC(if (sick) C_E3D9E2 else SKIN_SHADE)
        c.drawPath(BANGS_TOP, bs)
        for (d in STRANDS) c.drawPath(d, bs)
        c.restore()
        if (sick) {  // blue-green tint under the eyes, skin stays warm
            val ta = 0.32f + 0.25f * k
            c.drawOval(oval(41.2f, 54.4f, 5.4f, 2.6f), fillS(tintL, ta))
            c.drawOval(oval(60.4f, 54.4f, 4.6f, 2.6f), fillS(tintR, ta))
        }
        c.restore()
        c.drawPath(FACE, strokeC(rgba(120, 60, 60, 0.6f), 0.5f))

        // blush
        val blushA = if (calm) 0.6f + 0.2f * k else if (uneasy) 0.4f else 0.18f
        for (i in 0..1) {
            val x = if (i == 0) 38.6f else 62.6f
            val w = if (i == 0) 5f else 4.2f
            c.drawOval(oval(x, 57.2f, w, 3f), fillS(if (i == 0) blushL else blushR, blushA))
            if (!sick) c.drawLine(x - 1.6f, 56.4f, x - 0.8f, 55.8f, strokeC(rgba(255, 255, 255, 0.8f * blushA), 0.5f))
        }
        drawEyes(c, mood, k, t, blinkAmount(t))
        // nose (3/4: small shade on the far side + highlight)
        quad(c, 54.6f, 54.2f, 55.3f, 56f, 54.2f, 56.8f, strokeC(rgba(205, 125, 115, 0.85f), 0.55f))
        c.drawOval(oval(53.4f, 54.8f, 0.4f, 0.7f), fillC(rgba(255, 255, 255, 0.9f)))
        drawMouth(c, mood, k)

        // brows go UNDER the bangs: full here, then re-drawn at 45% over the strands
        drawBrows(c, mood, k)
        // face-framing locks
        for (d in arrayOf(LOCK_L, LOCK_R)) {
            c.drawPath(d, fillS(hairGrad))
            c.drawPath(d, strokeC(HAIR_LINE, 0.35f))
        }
        c.save(); c.clipPath(LOCK_R); c.translate(-1.6f, 0.6f)  // inner-colour peek
        ga = 0.55f; c.drawPath(LOCK_R, fillC(MINT)); ga = 1f
        c.restore()
        rim(c, LOCK_R, rimCol, 1.4f, 69f, 80f)
        // bangs: top mass + see-through strands
        c.drawPath(BANGS_TOP, fillS(hairGrad))
        for (d in STRANDS) c.drawPath(d, fillS(hairGrad))
        // cel shadow on the hair (away from the key light) + key light + root shade on strands
        c.save(); c.clipPath(BANGS_TOP); c.drawPath(HAIR_SHADE_TOP, fillC(HAIR_SHADE)); c.restore()
        c.save(); c.clipPath(BANGS_TOP); c.drawRect(26f, 16f, 56f, 36f, fillS(keyLight)); c.restore()
        val strandLine = rgba(120, 64, 50, 0.7f)
        for (i in STRANDS.indices) {
            val d = STRANDS[i]
            c.save(); c.clipPath(d); c.translate(1.3f, 0f)
            ga = 0.85f; c.drawPath(d, fillC(if (i >= 4) HAIR_SHADE else HAIR_MID)); ga = 1f
            c.restore()
            c.drawPath(d, strokeC(strandLine, 0.45f))
        }
        ga = 0.45f; drawBrows(c, mood, k); ga = 1f
        rim(c, BANGS_TOP, rimCol, 2.2f, 64f, 80f)
        rim(c, HAIR_BACK, rimCol, 2.2f, 66f, 80f)
        // angel-ring shine
        val ring = strokeC(rgba(255, 255, 255, 0.9f), 1.5f)
        oval(50.6f, 30f, 18f, 9.5f)
        c.drawArc(rect, 200f, 18f, false, ring)
        c.drawArc(rect, 226f, 32f, false, ring)
        c.drawArc(rect, 266f, 30f, false, ring)
        c.drawArc(rect, 304f, 22f, false, ring)
        // hair strand lines on the top mass
        val tl = strokeC(HAIR_LINE, 0.4f)
        quad(c, 50f, 19.5f, 48f, 24f, 46.4f, 29f, tl)
        quad(c, 42f, 21f, 38.6f, 25f, 36.8f, 30f, tl)
        quad(c, 58f, 21f, 61f, 25f, 62.6f, 30f, tl)

        if (uneasy && k > 0.3f) {  // worry lines appear and darken as level rises
            c.save(); c.clipPath(FACE)
            val wl = strokeC(rgba(120, 100, 200, 0.9f * (k - 0.3f)), 0.6f)
            for (x in floatArrayOf(39.6f, 41.6f, 43.6f)) c.drawLine(x, 43.4f, x, 45.6f, wl)
            c.restore()
        }
        if (sick) {  // gloom lines + flat cooling sheet on the forehead
            c.save(); c.clipPath(FACE)
            val gl = strokeC(rgba(110, 140, 210, 0.35f + 0.45f * k), 0.6f)
            for (x in floatArrayOf(39.6f, 41.6f, 43.6f)) c.drawLine(x, 43.2f, x, 45.4f, gl)
            c.restore()
            drawPatch(c)
        }
        // ribbon at the ponytail tie
        c.save(); c.translate(34.6f, 22.6f); c.rotate(deg(-0.5f + swing * 0.02f))
        c.drawPath(RIBBON_TAIL, fillS(ribbonGrad)); c.drawPath(RIBBON_TAIL, strokeC(OL, 0.45f))
        c.drawPath(RIBBON_L, fillS(ribbonGrad)); c.drawPath(RIBBON_R, fillS(ribbonGrad))
        c.drawPath(RIBBON_L, strokeC(OL, 0.5f)); c.drawPath(RIBBON_R, strokeC(OL, 0.5f))
        c.drawOval(oval(0f, 0f, 1.4f, 1.6f), fillC(ACCENT))
        c.drawOval(oval(0f, 0f, 1.4f, 1.6f), strokeC(OL, 0.45f))
        c.restore()
        // signature "road-star" clip
        c.save(); c.translate(64.4f, 26.4f); c.rotate(deg(0.3f))
        mtx.setScale(3.1f, 3.1f); STAR5.transform(mtx, tmpPath)
        shf.color = GOLD
        shf.setShadowLayer(shadowR(1.6f), 0f, 0f, rgba(255, 240, 180, 0.9f))
        c.drawPath(tmpPath, shf)
        c.scale(3.1f, 3.1f)
        c.drawPath(STAR5, fillS(starGrad))
        c.drawPath(STAR5, strokeC(GOLD_DEEP, 0.12f))
        path.reset()
        path.moveTo(-0.05f, -0.5f); path.lineTo(0f, -0.22f)
        path.moveTo(0f, -0.02f); path.lineTo(0.03f, 0.26f)
        c.drawPath(path, strokeC(WHITE, 0.13f))  // road lane dashes
        c.restore()

        // crystal drop earrings on both sides + headset on the near ear
        drawEarring(c, 30.4f, 55.2f, t, 0f)
        drawEarring(c, 69.4f, 52.6f, t, 1.3f)
        c.drawOval(oval(30.4f, 49.6f, 3.3f, 4.4f), fillS(earGrad))
        c.drawOval(oval(30.4f, 49.6f, 3.3f, 4.4f), strokeC(OL, 0.5f))
        c.drawOval(oval(30.3f, 49.6f, 1.8f, 2.7f), strokeC(ACCENT, 0.9f))
        quad(c, 31.2f, 53.6f, 33f, 61.6f, 44.6f, 62.2f, strokeC(HEADSET, 1.2f))
        c.drawOval(oval(45.6f, 62.1f, 1.8f, 1.5f), fillC(HEADSET))
        c.drawOval(oval(45.9f, 62.1f, 0.8f, 0.8f), fillC(ACCENT))

        // sweat drops (count, size and slide driven by level)
        if (uneasy) {
            drawSweat(c, 30.4f, 39f, 0.75f + 0.45f * k, 0, t)
            if (k > 0.35f) drawSweat(c, 26.6f, 46.4f, 0.45f + 0.4f * k, 1, t)
        } else if (sick) {
            drawSweat(c, 67.4f, 39f, 0.55f + 0.25f * k, 0, t)
        }
        c.restore()  // head

        if (calm) drawFingerHeart(c, t)
        if (uneasy) drawCheekHand(c, tilt)
    }

    private val SEQ = floatArrayOf(22f, 86f, 27f, 90f, 20f, 94f, 31f, 85f, 25f, 98f, 33f, 93f, 18f, 89f, 29f, 96.5f, 35f, 88.5f)

    private fun drawSweat(c: Canvas, x: Float, y: Float, r: Float, i: Int, t: Float) {
        val dy = ((t * 0.7f + i * 0.5f) % 1f) * 1.6f
        c.save(); c.translate(x, y + dy); c.scale(r, r)
        c.drawPath(DROP, fillS(dropGrad))
        c.drawPath(DROP, strokeC(C_2F7FBD, 0.9f))
        c.drawOval(oval(-0.9f, 1f, 0.7f, 1.1f), fillC(WHITE))
        c.restore()
    }

    private fun drawEarring(c: Canvas, x: Float, y: Float, t: Float, ph: Float) {
        c.drawLine(x, y, x + 0.1f, y + 2.4f, strokeC(SILVER, 0.4f))
        val sw = sin(t * TAU / 1.5f + ph) * 0.15f
        c.save(); c.translate(x + 0.1f, y + 4.2f); c.rotate(deg(sw))
        mtx.setScale(1.25f, 1.25f); GEM.transform(mtx, tmpPath)
        shf.color = C_BDEBFF
        shf.setShadowLayer(shadowR(2.2f), 0f, 0f, WHITE)
        c.drawPath(tmpPath, shf)
        c.scale(1.25f, 1.25f)
        c.drawPath(GEM, fillS(gemGrad))
        c.drawPath(GEM, strokeC(rgba(60, 40, 80, 0.6f), 0.25f))
        c.restore()
        ga = 0.5f + 0.5f * sin(t * TAU / 1.1f + ph * 2f)
        sparkle(c, x + 1.4f, y + 3f, 1.2f, WHITE, 1f)
        ga = 1f
    }

    private fun drawPatch(c: Canvas) {
        c.save(); c.translate(51f, 37.4f); c.scale(1f, 0.96f)
        c.drawPath(PATCH, fillS(patchGrad))
        c.drawPath(PATCH, strokeC(rgba(90, 150, 210, 0.85f), 0.45f))
        c.drawRect(-6f, -3f, 1f, -2.1f, fillC(rgba(255, 255, 255, 0.85f)))  // gloss strip
        val sn = strokeC(C_6FAEE6, 0.45f)  // printed snowflake
        for (i in 0..2) {
            c.save(); c.rotate(i * 60f); c.drawPath(SNOW_ARM, sn); c.restore()
        }
        c.restore()
    }

    private fun drawTowel(c: Canvas, k: Float) {
        c.save(); c.translate(0f, -k * 1.2f)
        for (front in booleanArrayOf(false, true)) {
            val d = if (front) TOWEL_FRONT else TOWEL_BACK
            c.drawPath(d, fillS(towelGrad))
            c.save(); c.clipPath(d)
            // woven stripes run along the towel
            if (front) {
                c.drawLine(57f, 88.6f, 70f, 87.6f, strokeC(TOWEL_STRIPE, 2.0f))
                c.drawLine(57f, 90.4f, 70f, 89.4f, strokeC(TOWEL_STRIPE, 0.6f))
            } else {
                quad(c, 60f, 79.4f, 70f, 79.4f, 78.6f, 84.6f, strokeC(TOWEL_STRIPE, 2.0f))
            }
            // terry texture
            val terry = fillC(rgba(170, 200, 232, 0.28f))
            var y = 77f
            while (y < 92f) {
                var x = 56f + (y * 7f) % 2f
                while (x < 82f) { c.drawOval(oval(x, y, 0.35f, 0.35f), terry); x += 2f }
                y += 1.6f
            }
            c.restore()
            c.drawPath(d, strokeC(TOWEL_EDGE, 0.9f))  // terry edge
            c.drawPath(d, strokeC(C_4F78A8, 0.45f))
        }
        // fringe at the hanging end
        val fr = strokeC(C_8DB9E6, 0.55f)
        for (i in 0..6) {
            val x = lerp(59.8f, 69f, i / 6f)
            val y = lerp(92.6f, 91.6f, i / 6f)
            c.drawLine(x, y, x + 0.2f, y + 2.2f, fr)
        }
        c.restore()
    }

    /** One quick blink every 3.6 s, around t = 2.95 (0.12 s long). */
    private fun blinkAmount(t: Float): Float {
        val p = t % 3.6f
        return if (p > 2.89f && p < 3.01f) 1f - abs(p - 2.95f) / 0.06f else 0f
    }

    // ---------------------------------------------------------------- eyes (3/4 view)
    private const val EYE_Y = 49.2f
    private val EYE_X = floatArrayOf(41.2f, 60.4f)
    private val EYE_FX = floatArrayOf(1f, 0.86f)

    private fun drawEyes(c: Canvas, mood: Mood, k: Float, t: Float, blink: Float) {
        for (e in 0..1) {
            val side = if (e == 0) -1f else 1f
            c.save(); c.translate(EYE_X[e], EYE_Y); c.scale(EYE_FX[e], 1f)
            if (mood == Mood.CALM && e == 1) { c.scale(side, 1f); drawWink(c); c.restore(); continue }
            if (mood == Mood.SICK) { c.scale(side, 1f); drawDizzy(c, t, side, k); c.restore(); continue }
            val open = 1f - blink
            if (open < 0.2f) { c.scale(side, 1f); drawClosed(c); c.restore(); continue }
            c.save(); c.scale(side, 1f)
            c.translate(0f, 6f); c.scale(1f, open); c.translate(0f, -6f)
            c.drawPath(EYE_WHITE, fillC(WHITE))
            c.save(); c.clipPath(EYE_WHITE)
            drawIris(c, mood, k)
            c.drawRect(-7f, -7f, 7f, -1.5f, fillS(lidShade))
            c.restore()
            c.drawPath(LASH_TOP, fillC(OL))
            c.drawPath(LASH_FLICK, fillC(OL))
            quad(c, -4.4f, -7.6f, 0.6f, -10.2f, 6f, -7.4f, strokeC(rgba(58, 32, 56, 0.45f), 0.4f))  // double-lid crease
            quad(c, 1.6f, 6.5f, 4.6f, 5.6f, 5.7f, 3.4f, strokeC(OL, 0.5f))
            c.restore()
            c.translate(0f, 6f); c.scale(1f, open); c.translate(0f, -6f)
            val wob = if (mood == Mood.UNEASY) sin(t * TAU * 1.5f) * 0.3f * (0.5f + k) else 0f
            ovalR(c, -1.6f + wob, -1.7f, 1.9f, 2.4f, -0.3f, fillC(WHITE))
            c.drawOval(oval(1.9f, 2.9f, 0.95f, 0.95f), fillC(WHITE))
            ga = 0.6f + 0.4f * sin(t * TAU / 1.8f + side)
            sparkle(c, 2.2f, -2.5f, 1.3f, WHITE, 1f)
            ga = 1f
            c.restore()
        }
    }

    private fun drawIris(c: Canvas, mood: Mood, k: Float) {
        val u = mood == Mood.UNEASY
        val ry = if (u) 5.0f - 0.6f * k else 5.4f
        val rx = if (u) 4.0f - 0.5f * k else 4.4f
        c.drawOval(oval(0.1f, 0.9f, rx, ry), fillS(if (u) irisUneasy else irisBright))
        c.drawOval(oval(0.1f, 0.9f, rx, ry), strokeC(rgba(42, 19, 64, 0.8f), 0.45f))
        c.drawOval(oval(0.1f, 0.4f, rx * 0.48f, ry * 0.52f), fillC(C_24103A))
        c.drawArc(oval(0.1f, 0.9f, rx * 0.72f, ry * 0.72f), 45f, 90f, false, strokeC(rgba(255, 230, 250, 0.6f), 0.7f))
    }

    private fun drawDizzy(c: Canvas, t: Float, side: Float, k: Float) {
        c.drawOval(oval(0.2f, 1f, 5.4f, 5.2f), fillC(rgba(140, 150, 230, 0.2f)))
        c.save(); c.translate(0.2f, 0.8f); c.rotate(deg((t * 2.2f * side) % TAU))
        c.drawPath(SWIRL, strokeC(C_6A55B8, 1.0f))
        c.restore()
        val droop = 1.2f * k
        quad(c, -5.6f, -3.2f + droop, 0.4f, -6.4f + droop, 6.4f, -2.6f + droop * 0.6f, strokeC(OL, 1.3f))
        c.drawLine(6.2f, -2.8f + droop * 0.6f, 8f, -1.8f + droop * 0.6f, strokeC(OL, 0.7f))
    }

    private fun drawWink(c: Canvas) {
        c.drawPath(WINK_LASH, fillC(OL))
        c.drawLine(6.6f, 0.8f, 8.6f, 1.8f, strokeC(OL, 0.6f))
    }

    private fun drawClosed(c: Canvas) {
        quad(c, -5.4f, 2f, 0.2f, 5.4f, 6f, 1.4f, strokeC(OL, 1.3f))
    }

    /** Brows sit just under the bang tips; the inner end lifts with level in UNEASY / SICK. */
    private fun drawBrows(c: Canvas, mood: Mood, k: Float) {
        val br = strokeC(BROW, if (mood == Mood.CALM) 1.0f else 1.2f)
        for (e in 0..1) {
            val side = if (e == 0) -1f else 1f
            c.save(); c.translate(EYE_X[e], 41.2f); c.scale(EYE_FX[e] * side, 1f)
            // inner end at -x (towards the nose), outer end at +x
            val yi: Float
            val yc: Float
            val yo: Float
            when (mood) {
                Mood.CALM -> { yi = 0.2f; yc = -1.6f; yo = 0.4f }
                Mood.UNEASY -> { val lift = 1.0f + 2.6f * k; yi = -lift; yc = -lift * 0.55f; yo = 1.2f }
                Mood.SICK -> { val lift = 0.6f + 1.6f * k; yi = -lift; yc = -0.2f; yo = 1.6f }
            }
            quad(c, -3.8f, yi, 0.4f, yc, 4.4f, yo, br)
            c.restore()
        }
    }

    private fun drawMouth(c: Canvas, mood: Mood, k: Float) {
        when (mood) {
            Mood.CALM -> {
                c.drawPath(MOUTH_CALM, fillC(MOUTH))
                c.save(); c.clipPath(MOUTH_CALM)
                c.drawOval(oval(52.4f, 65.4f, 3.2f, 2.2f), fillC(TONGUE))
                c.drawRect(47f, 59f, 57f, 60.3f, fillC(WHITE))
                c.restore()
                c.drawPath(MOUTH_CALM, strokeC(OL, 0.5f))
                ovalR(c, 54.2f, 63.8f, 0.7f, 0.4f, -0.3f, fillC(rgba(255, 255, 255, 0.85f)))
            }
            Mood.UNEASY -> {
                val a = 0.6f + 1.0f * k  // wave amplitude grows with level
                path.reset()
                path.moveTo(48.6f, 62f)
                path.quadTo(50.1f, 62f - a, 51.6f, 62f)
                path.quadTo(53.1f, 62f + a, 54.6f, 62f)
                c.drawPath(path, strokeC(OL, 0.8f))
                c.drawOval(oval(51.8f, 63.4f, 1.6f, 0.6f), fillC(rgba(240, 116, 148, 0.45f)))  // lower-lip gloss
            }
            Mood.SICK -> {
                val a = 0.8f + 0.8f * k
                path.reset()
                path.moveTo(48.2f, 62.2f)
                for (i in 0 until 4) {
                    path.quadTo(48.9f + i * 1.5f, 62.2f + (if (i % 2 == 1) a else -a), 49.6f + i * 1.5f, 62.2f)
                }
                c.drawPath(path, strokeC(C_6A5A8E, 0.85f))
            }
        }
    }

    // ---------------------------------------------------------------- hands
    private fun drawFingerHeart(c: Canvas, t: Float) {
        // raised arm: black sequin sleeve with pink cuff
        capsule(c, 90f, 108f, 76.6f, 76f, 10.5f, JACKET, OL, 1.0f)
        sp.strokeCap = Paint.Cap.BUTT
        c.drawLine(72.6f, 76.6f, 81.4f, 79.8f, strokeC(ACCENT, 2.2f))
        sp.strokeCap = Paint.Cap.ROUND
        val sq = fillC(rgba(232, 236, 246, 0.8f))
        c.drawOval(oval(82f, 88f, 0.6f, 0.6f), sq)
        c.drawOval(oval(85f, 94f, 0.6f, 0.6f), sq)
        c.drawOval(oval(80.6f, 83.4f, 0.6f, 0.6f), sq)
        // fist at cheek height
        c.save(); c.translate(74.8f, 68.4f); c.rotate(deg(0.12f)); c.scale(1.18f, 1.18f)
        capsule(c, 0f, 0.4f, 0f, 2.2f, 7.6f, SKIN, OL, 0.7f)
        val kn = strokeC(IL, 0.4f)
        c.drawLine(-3.6f, 0.2f, -0.6f, 0.2f, kn)
        c.drawLine(-3.6f, 2.2f, -0.6f, 2.2f, kn)
        // crossed index + thumb: the finger heart (bold, contrasting outline)
        val fo = C_B8235F
        capsule(c, -2.6f, -2.6f, 0.9f, -9.8f, 2.9f, SKIN, fo, 0.75f)
        capsule(c, 2.8f, -3.0f, -0.5f, -10.0f, 3.0f, C_FFE2D8, fo, 0.75f)
        c.drawOval(oval(-0.4f, -9.9f, 0.8f, 0.6f), fillC(C_FFB3C8))  // nail tint
        c.restore()
        // heart pops out of the fingers
        val ph = sin(t * TAU / 1.2f)
        val sc = 1.55f * (1f + 0.1f * ph)
        val hx = 81f
        val hy = 52f - 0.8f * ph
        mtx.setScale(sc, sc); mtx.postRotate(deg(0.22f)); mtx.postTranslate(hx, hy)
        HEART.transform(mtx, tmpPath)
        shf.color = C_FF5AA8
        shf.setShadowLayer(shadowR(3f), 0f, 0f, C_FF2E8E)
        c.drawPath(tmpPath, shf)
        c.save(); c.translate(hx, hy); c.rotate(deg(0.22f)); c.scale(sc, sc)
        c.drawPath(HEART, fillS(heartGrad))
        c.drawPath(HEART, strokeC(WHITE, 0.55f))
        ovalR(c, -1.8f, -2f, 0.9f, 0.6f, -0.5f, fillC(rgba(255, 255, 255, 0.85f)))
        c.restore()
        sparkle(c, 76.6f, 55.6f, 1.4f, WHITE, 0.6f + 0.4f * ph)
    }

    private fun drawCheekHand(c: Canvas, tilt: Float) {
        capsule(c, 90f, 108f, 73f, 74f, 10.5f, JACKET, OL, 1.0f)
        sp.strokeCap = Paint.Cap.BUTT
        c.drawLine(69.6f, 75f, 78f, 78.4f, strokeC(ACCENT, 2.2f))
        sp.strokeCap = Paint.Cap.ROUND
        // flat, closed-finger hand pressed to the far cheek (follows the head tilt)
        c.save(); c.rotate(tilt, 50f, 68f)
        c.save(); c.translate(67.6f, 62.4f); c.rotate(deg(-0.28f))
        c.drawPath(MITTEN, strokeC(OL, 1.6f))
        c.drawPath(MITTEN, fillC(SKIN))
        c.save(); c.clipPath(MITTEN); c.drawRect(1.4f, -12f, 5.4f, 6f, fillC(rgba(244, 201, 187, 0.8f))); c.restore()
        val fl = strokeC(IL, 0.4f)
        for (x in floatArrayOf(-1.6f, 0.2f, 1.9f)) c.drawLine(x, -9.4f + abs(x) * 0.3f, x * 0.9f, -4.6f, fl)
        val nail = fillC(rgba(255, 170, 195, 0.8f))
        for (x in floatArrayOf(-1.7f, 0.1f, 1.8f)) c.drawOval(oval(x * 0.98f, -9.2f + abs(x) * 0.3f, 0.6f, 0.45f), nail)
        c.restore()
        c.restore()
    }

    // ---------------------------------------------------------------- foreground effects
    private val FRONT_SPARKS = floatArrayOf(16f, 20f, 3.2f, 0f, 86f, 22f, 2.6f, 1.3f, 12f, 70f, 2.2f, 2.1f, 89f, 66f, 2.2f, 0.7f)
    private val SPARK_COL by lazy { C_FFF6C8 }
    private val ZZ_GLOW by lazy { C_2C4A8C }

    private fun drawFront(c: Canvas, mood: Mood, k: Float, t: Float) {
        when (mood) {
            Mood.CALM -> {
                shf.setShadowLayer(shadowR(2f), 0f, 0f, WHITE)
                for (i in 0 until 4) {
                    val x = FRONT_SPARKS[i * 4]; val y = FRONT_SPARKS[i * 4 + 1]
                    val r = FRONT_SPARKS[i * 4 + 2]; val ph = FRONT_SPARKS[i * 4 + 3]
                    val tw = 0.55f + 0.45f * sin(t * TAU / 1.6f + ph * 2f)
                    val rr = r * (0.7f + 0.3f * tw)
                    mtx.setScale(rr, rr); mtx.postTranslate(x, y)
                    STAR4.transform(mtx, tmpPath)
                    shf.color = rgba(Color.red(SPARK_COL), Color.green(SPARK_COL), Color.blue(SPARK_COL), 0.5f + 0.5f * tw)
                    c.drawPath(tmpPath, shf)
                }
            }
            Mood.UNEASY -> {
                val tw = 0.5f + 0.5f * sin(t * TAU / 2.4f)
                sparkle(c, 84f, 22f, 2.2f, WHITE, (0.4f + 0.4f * tw) * (1f - 0.6f * k))
                sparkle(c, 15f, 64f, 1.8f, WHITE, (0.4f + 0.4f * (1f - tw)) * (1f - 0.6f * k))
            }
            Mood.SICK -> {
                val p = (t / 2.4f) % 1f
                val n = if (k > 0.5f) 3 else 2
                shs.setShadowLayer(shadowR(2f), 0f, 0f, ZZ_GLOW)
                for (i in 0 until n) {
                    val q = (p + i / n.toFloat()) % 1f
                    val a = 0.3f + 0.7f * sin(q * PI_F)
                    val sc = 0.8f + 0.45f * i + 0.3f * k
                    mtx.setScale(sc, sc)
                    mtx.postTranslate(74f + q * 6f + i * 1.5f, 28f - q * (10f + 8f * k) - 4f * k)
                    ZZ1.transform(mtx, tmpPath)
                    shs.color = rgba(255, 255, 255, a)
                    shs.strokeWidth = 1.4f * sc
                    c.drawPath(tmpPath, shs)
                }
            }
        }
    }
}
