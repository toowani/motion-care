package ai.motioncare.app.theme

import ai.motioncare.app.R
import ai.motioncare.app.ui.RiskRing
import ai.motioncare.app.ui.Ui
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.sin

/**
 * Collaboration example: the original virtual idol "루미 (LUMI)". Shows how the icon slot could
 * host a licensed entertainment IP later. The character is original (no real person, group,
 * VTuber or existing character); the three portraits are AI renders (Higgsfield) shipped as
 * res/drawable-nodpi/lumi_{calm,uneasy,sick}.webp, one per mood, each on a stage light in the
 * mood's colour (green / yellow / red).
 *
 * Unlike the other themes this one draws bitmaps, so it needs resources once ([attach]);
 * until then it falls back to a plain disc in the mood colour.
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

    private const val TAU = 6.2831855f
    private const val RING_R = 46.5f
    private const val RING_W = 5f
    /** The portrait's circle fills its square; crop a little inside it so idle motion never shows the edge. */
    private const val CROP = 0.04f
    private val ART = intArrayOf(R.drawable.lumi_calm, R.drawable.lumi_uneasy, R.drawable.lumi_sick)
    private val MOOD_COLOR = intArrayOf(Ui.RISK_LOW, Ui.RISK_MID, Ui.RISK_HIGH)

    /** Application resources only (not a Context), so the object holds nothing that can leak. */
    private var res: Resources? = null
    private val art = arrayOfNulls<Bitmap>(3)

    /** Lets the theme load its pictures; cheap to call more than once. */
    fun attach(context: Context) {
        if (res == null) res = context.applicationContext.resources
    }

    private fun picture(mood: Mood): Bitmap? = art[mood.ordinal] ?: res?.let { r ->
        BitmapFactory.decodeResource(r, ART[mood.ordinal])?.also { art[mood.ordinal] = it }
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.4f }
    private val clip = Path()
    private val src = Rect()
    private val dst = RectF()

    override fun draw(c: Canvas, cx: Float, cy: Float, size: Float, mood: Mood, level: Float, t: Float) {
        if (size <= 0f) return
        val s = size / 100f
        c.save()
        c.translate(cx - size / 2f, cy - size / 2f)
        c.scale(s, s)
        // with the risk ring on, the portrait sits just inside it; otherwise a thin rim in the mood colour
        val ring = RiskRing.current
        val r = if (ring.isNaN()) 46f else RING_R - RING_W / 2f - 0.8f
        c.save()
        clip.reset()
        clip.addCircle(50f, 50f, r, Path.Direction.CW)
        c.clipPath(clip)
        val bmp = picture(mood)
        if (bmp == null) {
            fill.color = MOOD_COLOR[mood.ordinal]
            c.drawCircle(50f, 50f, r, fill)
        } else {
            // idle motion: a slow breath; when sick, a gentle sway that grows with the level
            val breath = 1.01f + 0.012f * sin(t * TAU / 3.2f)
            val sway = if (mood == Mood.SICK) sin(t * TAU / 4f) * (1f + (level - 1f).coerceIn(0f, 0.5f) * 2f) else 0f
            c.rotate(sway, 50f, 92f)
            c.scale(breath, breath, 50f, 62f)
            val inset = (bmp.width * CROP).toInt()
            src.set(inset, inset, bmp.width - inset, bmp.height - inset)
            dst.set(50f - r, 50f - r, 50f + r, 50f + r)
            c.drawBitmap(bmp, src, dst, paint)
        }
        c.restore()
        if (ring.isNaN()) {
            rim.color = MOOD_COLOR[mood.ordinal]
            c.drawCircle(50f, 50f, 46.8f, rim)
        } else {
            RiskRing.draw(c, 50f, 50f, RING_R, RING_W, ring)
        }
        c.restore()
    }
}
