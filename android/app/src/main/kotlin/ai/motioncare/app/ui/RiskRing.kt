package ai.motioncare.app.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * The character's border: a thick ring that fills clockwise from the top with the chance of
 * sickness, green → yellow → red. Every theme and the map widget draw it the same way.
 */
object RiskRing {
    val TRACK = Color.parseColor("#E5E7EB")

    /**
     * The value the [ai.motioncare.app.theme.MoodIconView] being drawn holds (0..1, NaN = no ring).
     * The view sets it right before calling the theme's draw, so themes need no new parameter.
     */
    @JvmStatic var current: Float = Float.NaN

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = TRACK }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val oval = RectF()

    fun color(p: Float) = when { p >= 0.5f -> Ui.RISK_HIGH; p >= 0.25f -> Ui.RISK_MID; else -> Ui.RISK_LOW }

    /** Ring centred on ([cx], [cy]); [radius] is the stroke's centre line. Draws nothing when [p] is NaN. */
    fun draw(c: Canvas, cx: Float, cy: Float, radius: Float, stroke: Float, p: Float) {
        if (p.isNaN()) return
        val v = p.coerceIn(0f, 1f)
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius)
        track.strokeWidth = stroke
        c.drawCircle(cx, cy, radius, track)
        arc.strokeWidth = stroke
        arc.color = color(v)
        // a small dot at 0 % so the ring never looks broken
        c.drawArc(oval, -90f, (360f * v).coerceAtLeast(1f), false, arc)
    }

    /** Same, with the value of the view being drawn ([current]). */
    fun draw(c: Canvas, cx: Float, cy: Float, radius: Float, stroke: Float) = draw(c, cx, cy, radius, stroke, current)
}
