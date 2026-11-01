package ai.motioncare.app

import ai.motioncare.core.Action
import ai.motioncare.core.AlertState
import ai.motioncare.core.Output
import ai.motioncare.core.Sample
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/**
 * Left: the only thing a driver needs (one colour dot + a word).
 * Right: the demo panel — index, thresholds × cabin factor, cues and HVAC actions over time.
 */
class DashboardView(context: Context) : View(context) {
    private data class Point(val s: Sample, val o: Output)

    private val points = ArrayList<Point>()
    private val log = ArrayDeque<String>()
    private val violet = Color.parseColor("#453ECE")
    private val navy = Color.parseColor("#012348")
    private val amber = Color.parseColor("#B45309")
    private val warn = Color.parseColor("#C2410C")
    private val muted = Color.parseColor("#6B6E86")
    private val line = Color.parseColor("#DADAEA")
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 30f; color = navy }

    fun reset() {
        points.clear(); log.clear(); invalidate()
    }

    fun add(s: Sample, o: Output) {
        points += Point(s, o)
        if (points.size > 900) points.removeAt(0)
        invalidate()
    }

    fun note(msg: String) {
        log.addFirst(msg)
        while (log.size > 6) log.removeLast()
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(Color.WHITE)
        val w = width.toFloat()
        val h = height.toFloat()
        val split = w * 0.28f
        val last = points.lastOrNull()

        // driver panel
        val state = last?.o?.state ?: AlertState.NORMAL
        fill.color = when (state) {
            AlertState.NORMAL -> Color.parseColor("#15803D")
            AlertState.CAUTION -> amber
            AlertState.HIGH -> warn
        }
        val r = minOf(split, h) * 0.22f
        c.drawCircle(split / 2, h * 0.36f, r, fill)
        text.textSize = 44f; text.textAlign = Paint.Align.CENTER; text.color = navy
        c.drawText(when (state) { AlertState.NORMAL -> "편안함"; AlertState.CAUTION -> "흔들림 주의"; AlertState.HIGH -> "멀미 위험" }, split / 2, h * 0.36f + r + 60f, text)
        text.textSize = 26f; text.color = muted
        last?.let {
            c.drawText("%.0f km/h · 실내 %.1f°C · %s".format(it.s.speed * 3.6, it.s.cabinTemp, if (it.s.recirc) "내기" else "외기"), split / 2, h * 0.36f + r + 110f, text)
            c.drawText("환경 계수 m = %.2f".format(it.o.m), split / 2, h * 0.36f + r + 150f, text)
        }

        // demo graph
        val left = split + 40f
        val right = w - 30f
        val top = 40f
        val bottom = h * 0.68f
        stroke.color = line; stroke.strokeWidth = 2f; stroke.pathEffect = null
        c.drawRect(left, top, right, bottom, stroke)
        if (points.size > 1) {
            val ymax = maxOf(2.0, points.maxOf { it.o.index } * 1.1)
            val n = points.size
            fun x(i: Int) = left + (right - left) * i / (n - 1).coerceAtLeast(1)
            fun y(v: Double) = (bottom - (bottom - top) * (v.coerceAtMost(ymax) / ymax)).toFloat()
            for ((key, color) in listOf<Pair<(Output) -> Double, Int>>({ o: Output -> o.thetaCaution } to amber, { o: Output -> o.thetaHigh } to warn)) {
                val p = Path()
                points.forEachIndexed { i, pt -> if (i == 0) p.moveTo(x(i), y(key(pt.o))) else p.lineTo(x(i), y(key(pt.o))) }
                stroke.color = color; stroke.strokeWidth = 3f; stroke.pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
                c.drawPath(p, stroke)
            }
            val p = Path()
            points.forEachIndexed { i, pt -> if (i == 0) p.moveTo(x(i), y(pt.o.index)) else p.lineTo(x(i), y(pt.o.index)) }
            stroke.color = violet; stroke.strokeWidth = 5f; stroke.pathEffect = null
            c.drawPath(p, stroke)
            points.forEachIndexed { i, pt ->
                pt.o.actions.forEach { a ->
                    when (a) {
                        Action.CUE_LEFT, Action.CUE_RIGHT -> { fill.color = navy; c.drawCircle(x(i), top + 18f, 9f, fill) }
                        Action.HVAC_FRESH -> { fill.color = warn; c.drawRect(x(i) - 9f, top + 34f, x(i) + 9f, top + 52f, fill) }
                        else -> Unit
                    }
                }
            }
        }
        text.textAlign = Paint.Align.LEFT; text.textSize = 26f; text.color = navy
        c.drawText("멀미 지수 I (보라) · 주의/높음 기준선 × 환경 계수 (점선) · ● 예고  ■ 공조", left, top - 10f, text)

        // event log
        text.color = muted; text.textSize = 26f
        log.forEachIndexed { i, msg -> c.drawText(msg, left, bottom + 50f + i * 36f, text) }
    }
}
