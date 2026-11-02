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
import android.util.Log
import android.view.View

private const val TAG = "MotionCare"

/** Demo panel: index, thresholds × cabin factor, cues and HVAC actions over time, and the event log. */
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
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 36f; color = navy }

    fun reset() {
        points.clear(); log.clear(); invalidate()
    }

    fun add(s: Sample, o: Output) {
        points += Point(s, o)
        if (points.size > 900) points.removeAt(0)
        invalidate()
    }

    fun note(msg: String) {
        Log.i(TAG, "note: $msg")
        log.addFirst(msg)
        while (log.size > 6) log.removeLast()
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(Color.WHITE)
        val w = width.toFloat()
        val h = height.toFloat()
        val split = 0f

        // demo graph
        val left = split + 20f
        val right = w - 20f
        val top = 54f
        val bottom = h * 0.62f
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
        text.textAlign = Paint.Align.LEFT; text.textSize = 32f; text.color = navy
        c.drawText("흔들림 지수 (보라) · 주의/높음 기준선 (점선) · ● 예고  ■ 공조", left, top - 14f, text)

        // event log
        text.color = muted; text.textSize = 30f
        log.forEachIndexed { i, msg -> if (bottom + 50f + i * 40f < h) c.drawText(msg, left, bottom + 50f + i * 40f, text) }
    }
}
