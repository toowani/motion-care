package ai.motioncare.app.ui

import ai.motioncare.app.ui.Ui.dpf
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View

/** Semicircle gauge: sickness probability as a big % with a level word, coloured by risk. */
class RiskGauge(context: Context) : View(context) {
    private var p = 0.0
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = Ui.LINE }
    private val arc = Paint(track).apply { color = Ui.RISK_LOW }
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.MUTED; strokeWidth = context.dpf(2f) }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private val word = Paint(big)
    private val oval = RectF()

    fun set(value: Double) {
        p = if (value.isNaN()) 0.0 else value.coerceIn(0.0, 1.0)
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        // the arc hangs from the top (right under the card title); height left below it goes to bigger text
        val sw = minOf(w, h * 1.4f) * 0.1f
        val r = minOf(w / 2f - sw, (h - sw) / 1.45f)
        val cx = w / 2f; val cy = sw * 0.9f + r
        val spare = (h - (sw * 0.9f + r * 1.45f)).coerceAtLeast(0f)
        val grow = (1f + spare / r * 0.35f).coerceAtMost(1.2f)
        oval.set(cx - r, cy - r, cx + r, cy + r)
        track.strokeWidth = sw; arc.strokeWidth = sw
        c.drawArc(oval, 180f, 180f, false, track)
        val col = color(p)
        arc.color = col
        c.drawArc(oval, 180f, (180f * p.toFloat()).coerceAtLeast(2f), false, arc)
        // 50 % (alert) mark at the top of the arc
        c.drawLine(cx, cy - r - sw * 0.9f, cx, cy - r + sw * 0.9f, tick)
        big.color = ink(p)
        big.textSize = r * 0.62f * grow
        // the number stays inside the arc ("100%" included)
        val tw = big.measureText("${Ui.pct(p)}%")
        if (tw > r * 1.45f) big.textSize *= r * 1.45f / tw
        c.drawText("${Ui.pct(p)}%", cx, cy - r * 0.12f + (grow - 1f) * r * 0.5f, big)
        word.color = ink(p)
        word.textSize = r * 0.28f * grow
        c.drawText(label(p), cx, cy + r * 0.34f * grow + (grow - 1f) * r * 0.5f, word)
    }

    companion object {
        val AMBER: Int = Ui.RISK_MID
        /** Fill colour: < 25 % green, 25–50 % yellow, ≥ 50 % red. */
        fun color(p: Double) = when { p >= 0.5 -> Ui.RISK_HIGH; p >= 0.25 -> Ui.RISK_MID; else -> Ui.RISK_LOW }
        /** The same band as text on white (yellow darkened for contrast). */
        fun ink(p: Double) = when { p >= 0.5 -> Ui.RISK_HIGH; p >= 0.25 -> Ui.RISK_MID_INK; else -> Ui.RISK_LOW }
        // its own words: 편안·주의·위험 belong to the face (the shake now), this is the chance of getting sick
        fun label(p: Double) = when { p >= 0.5 -> "높음"; p >= 0.25 -> "보통"; else -> "낮음" }
    }
}
