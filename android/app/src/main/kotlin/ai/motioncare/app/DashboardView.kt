package ai.motioncare.app

import ai.motioncare.app.ui.Ui.dpf
import ai.motioncare.core.Action
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

/**
 * Analysis panel: either the shake index with its thresholds × cabin factor, or the sickness
 * probability (one line per child in family mode). Cue and fresh-air markers on both; event log below.
 */
class DashboardView(context: Context) : View(context) {
    enum class Mode { INDEX, RISK }

    private data class Point(val s: Sample, val o: Output, val r: TripService.Risk)

    private val points = ArrayList<Point>()
    private val log = ArrayDeque<String>()
    private val violet = Color.parseColor("#453ECE")
    private val navy = Color.parseColor("#012348")
    private val amber = Color.parseColor("#B45309")
    private val warn = Color.parseColor("#C2410C")
    private val muted = Color.parseColor("#6B6E86")
    private val line = Color.parseColor("#DADAEA")
    /** One colour per child, in profile order. */
    private val childColors = intArrayOf(violet, Color.parseColor("#DB2777"), Color.parseColor("#0284C7"), Color.parseColor("#65A30D"))
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navy }
    private val bigText = Paint(text).apply { textSize = context.dpf(24f); isFakeBoldText = true; textAlign = Paint.Align.LEFT }
    private val linePath = Path()
    private val dash = DashPathEffect(floatArrayOf(14f, 10f), 0f)

    var mode: Mode = lastMode
        set(v) { field = v; lastMode = v; invalidate() }

    fun reset() {
        points.clear(); log.clear(); invalidate()
    }

    fun add(s: Sample, o: Output, r: TripService.Risk) {
        points += Point(s, o, r)
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
        val label = context.dpf(16f)
        val left = context.dpf(12f) + if (mode == Mode.RISK) context.dpf(48f) else 0f
        val right = w - context.dpf(if (mode == Mode.RISK) 110f else 12f)
        val top = label * 2.4f
        val bottom = h * 0.70f
        stroke.color = line; stroke.strokeWidth = 2f; stroke.pathEffect = null
        c.drawRect(left, top, right, bottom, stroke)
        val n = points.size
        fun x(i: Int) = left + (right - left) * i / (n - 1).coerceAtLeast(1)

        text.textSize = label; text.isFakeBoldText = true; text.textAlign = Paint.Align.LEFT
        if (mode == Mode.INDEX) drawIndex(c, left, right, top, bottom, ::x) else drawRisk(c, left, right, top, bottom, ::x)

        // event markers: turn cue (dot) and fresh air (square), on both graphs
        points.forEachIndexed { i, pt ->
            pt.o.actions.forEach { a ->
                when (a) {
                    Action.CUE_LEFT, Action.CUE_RIGHT -> { fill.color = navy; c.drawCircle(x(i), top + 18f, 9f, fill) }
                    Action.HVAC_FRESH -> { fill.color = warn; c.drawRect(x(i) - 9f, top + 34f, x(i) + 9f, top + 52f, fill) }
                    else -> Unit
                }
            }
        }
        // marker legend (top-right)
        text.textAlign = Paint.Align.RIGHT; text.color = muted; text.textSize = label
        val ly = top - label * 0.8f
        val airW = text.measureText("바람")
        c.drawText("바람", w - context.dpf(12f), ly, text)
        fill.color = warn
        val sq = label * 0.35f
        val airX = w - context.dpf(12f) - airW - sq - context.dpf(6f)
        c.drawRect(airX - sq, ly - label * 0.35f - sq, airX + sq, ly - label * 0.35f + sq, fill)
        val cueEnd = airX - sq - context.dpf(18f)
        val cueW = text.measureText("회전 예고")
        c.drawText("회전 예고", cueEnd, ly, text)
        fill.color = navy
        c.drawCircle(cueEnd - cueW - sq - context.dpf(6f), ly - label * 0.35f, sq, fill)

        // event log
        text.textAlign = Paint.Align.LEFT; text.isFakeBoldText = false; text.color = muted; text.textSize = label
        val step = label * 1.45f
        log.forEachIndexed { i, msg -> val y = bottom + step * (i + 1.5f); if (y < h) c.drawText(msg, context.dpf(12f), y, text) }
    }

    private fun drawIndex(c: Canvas, left: Float, right: Float, top: Float, bottom: Float, x: (Int) -> Float) {
        val label = text.textSize
        legend(c, left, top - label * 0.8f, listOf(Triple("흔들림", violet, false), Triple("주의선", amber, true), Triple("높음선", warn, true)))
        if (points.size < 2) return
        val ymax = maxOf(2.0, points.maxOf { it.o.index } * 1.1)
        fun y(v: Double) = (bottom - (bottom - top) * (v.coerceAtMost(ymax) / ymax)).toFloat()
        for ((key, color) in listOf<Pair<(Output) -> Double, Int>>({ o: Output -> o.thetaCaution } to amber, { o: Output -> o.thetaHigh } to warn)) {
            plot(c, color, 3f, dash) { i -> x(i) to y(key(points[i].o)) }
        }
        plot(c, violet, 5f, null) { i -> x(i) to y(points[i].o.index) }
    }

    private fun drawRisk(c: Canvas, left: Float, right: Float, top: Float, bottom: Float, x: (Int) -> Float) {
        val label = text.textSize
        fun y(p: Double) = (bottom - (bottom - top) * p.coerceIn(0.0, 1.0)).toFloat()
        // 0 / 50 / 100 % axis; 50 % is where the car steps in
        text.textAlign = Paint.Align.RIGHT; text.color = muted
        for (p in listOf(0.0, 1.0)) c.drawText("${(p * 100).toInt()}%", left - context.dpf(6f), y(p) + label / 3, text)
        stroke.color = warn; stroke.strokeWidth = 3f; stroke.pathEffect = dash
        c.drawLine(left, y(0.5), right, y(0.5), stroke)
        text.color = warn
        c.drawText("50%", left - context.dpf(6f), y(0.5) + label / 3, text)
        text.textAlign = Paint.Align.LEFT
        c.drawText("알림", right + context.dpf(8f), y(0.5) + label / 3, text)

        val names = points.lastOrNull()?.r?.children?.map { it.first }.orEmpty()
        val series = if (names.isEmpty()) listOf(Triple("멀미 가능성", violet, -1))
            else names.mapIndexed { k, name -> Triple(name, childColors[k % childColors.size], k) }
        legend(c, left, top - label * 0.8f, series.map { Triple(it.first, it.second, false) })
        if (points.size < 2) return
        val big = bigText
        for ((name, color, k) in series) {
            fun p(i: Int): Double {
                val r = points[i].r
                return if (k < 0) r.p else r.children.firstOrNull { it.first == name }?.second ?: r.p
            }
            plot(c, color, 5f, null) { i -> x(i) to y(p(i)) }
            // current value at the end of each line, large enough to read at a glance
            val v = p(points.size - 1)
            big.color = color
            if (series.size == 1 || v >= 0.02) c.drawText("${ai.motioncare.app.ui.Ui.pct(v)}%", right + context.dpf(8f), y(v) + big.textSize / 3, big)
        }
    }

    private fun plot(c: Canvas, color: Int, width: Float, effect: DashPathEffect?, at: (Int) -> Pair<Float, Float>) {
        val p = linePath
        p.rewind()
        for (i in points.indices) { val (px, py) = at(i); if (i == 0) p.moveTo(px, py) else p.lineTo(px, py) }
        stroke.color = color; stroke.strokeWidth = width; stroke.pathEffect = effect
        c.drawPath(p, stroke)
    }

    /** Colour swatch + 1-word label per series, left to right. */
    private fun legend(c: Canvas, x0: Float, baseline: Float, items: List<Triple<String, Int, Boolean>>) {
        var x = x0
        val sw = context.dpf(22f)
        text.textAlign = Paint.Align.LEFT
        for ((name, color, dashed) in items) {
            stroke.color = color; stroke.strokeWidth = 5f; stroke.pathEffect = if (dashed) dash else null
            val cy = baseline - text.textSize * 0.35f
            c.drawLine(x, cy, x + sw, cy, stroke)
            x += sw + context.dpf(6f)
            text.color = color
            c.drawText(name, x, baseline, text)
            x += text.measureText(name) + context.dpf(18f)
        }
    }

    companion object {
        /** The chosen graph survives tab switches and screen re-creation while the app runs. */
        private var lastMode = Mode.INDEX
    }
}
