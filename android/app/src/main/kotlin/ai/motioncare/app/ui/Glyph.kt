package ai.motioncare.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/**
 * Simple line pictograms for the glanceable home screen (drawn, no bitmaps). Each kind is drawn
 * in a unit box, so it scales with the view; [color] tints strokes and fills.
 */
class Glyph(context: Context, kind: Kind, color: Int) : View(context) {
    enum class Kind { CLOCK, TURN, WIND, WINDOW, PHONE, FOOD, FACE_CALM, FACE_UNEASY, FACE_SICK }

    var kind: Kind = kind
        set(value) { field = value; invalidate() }
    var color: Int = color
        set(value) { field = value; invalidate() }

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val box = RectF()

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        if (s <= 0f) return
        c.save()
        c.translate((width - s) / 2f, (height - s) / 2f)
        c.scale(s, s)
        stroke.color = color; stroke.strokeWidth = 0.085f
        fill.color = color
        when (kind) {
            Kind.CLOCK -> clock(c)
            Kind.TURN -> turn(c)
            Kind.WIND -> wind(c)
            Kind.WINDOW -> window(c)
            Kind.PHONE -> phone(c)
            Kind.FOOD -> food(c)
            Kind.FACE_CALM, Kind.FACE_UNEASY, Kind.FACE_SICK -> face(c)
        }
        c.restore()
    }

    private fun clock(c: Canvas) {
        c.drawCircle(0.5f, 0.5f, 0.4f, stroke)
        c.drawLine(0.5f, 0.5f, 0.5f, 0.26f, stroke)
        c.drawLine(0.5f, 0.5f, 0.68f, 0.6f, stroke)
    }

    /** Arrow that goes up and bends right, like a navigation turn. */
    private fun turn(c: Canvas) {
        path.reset()
        path.moveTo(0.3f, 0.9f); path.lineTo(0.3f, 0.5f)
        path.quadTo(0.3f, 0.3f, 0.5f, 0.3f); path.lineTo(0.72f, 0.3f)
        c.drawPath(path, stroke)
        path.reset()
        path.moveTo(0.62f, 0.14f); path.lineTo(0.84f, 0.3f); path.lineTo(0.62f, 0.46f); path.close()
        c.drawPath(path, fill)
    }

    private fun wind(c: Canvas) {
        path.reset()
        path.moveTo(0.1f, 0.36f); path.lineTo(0.62f, 0.36f)
        path.cubicTo(0.86f, 0.36f, 0.86f, 0.08f, 0.66f, 0.12f)
        c.drawPath(path, stroke)
        path.reset()
        path.moveTo(0.1f, 0.56f); path.lineTo(0.78f, 0.56f)
        c.drawPath(path, stroke)
        path.reset()
        path.moveTo(0.1f, 0.76f); path.lineTo(0.52f, 0.76f)
        path.cubicTo(0.74f, 0.76f, 0.74f, 0.98f, 0.56f, 0.94f)
        c.drawPath(path, stroke)
    }

    /** Window frame with far-away hills: "look out at the distance". */
    private fun window(c: Canvas) {
        box.set(0.1f, 0.14f, 0.9f, 0.86f)
        c.drawRoundRect(box, 0.08f, 0.08f, stroke)
        path.reset()
        path.moveTo(0.16f, 0.78f); path.lineTo(0.38f, 0.5f); path.lineTo(0.52f, 0.66f)
        path.lineTo(0.66f, 0.46f); path.lineTo(0.84f, 0.78f); path.close()
        c.drawPath(path, fill)
        c.drawCircle(0.72f, 0.3f, 0.07f, fill)
    }

    /** Phone with a short-time mark: "keep screens short". */
    private fun phone(c: Canvas) {
        box.set(0.26f, 0.08f, 0.74f, 0.92f)
        c.drawRoundRect(box, 0.09f, 0.09f, stroke)
        c.drawLine(0.44f, 0.8f, 0.56f, 0.8f, stroke)
        c.drawCircle(0.5f, 0.44f, 0.13f, stroke)
        c.drawLine(0.5f, 0.44f, 0.5f, 0.36f, stroke)
    }

    /** Bowl with steam: "eat lightly". */
    private fun food(c: Canvas) {
        path.reset()
        path.moveTo(0.1f, 0.52f); path.lineTo(0.9f, 0.52f)
        path.quadTo(0.88f, 0.88f, 0.5f, 0.88f); path.quadTo(0.12f, 0.88f, 0.1f, 0.52f); path.close()
        c.drawPath(path, fill)
        for (x in floatArrayOf(0.36f, 0.5f, 0.64f)) {
            path.reset()
            path.moveTo(x, 0.42f); path.quadTo(x - 0.06f, 0.3f, x, 0.22f); path.quadTo(x + 0.06f, 0.14f, x, 0.08f)
            c.drawPath(path, stroke)
        }
    }

    private fun face(c: Canvas) {
        c.drawCircle(0.5f, 0.5f, 0.44f, fill)
        stroke.color = android.graphics.Color.WHITE
        fill.color = android.graphics.Color.WHITE
        c.drawCircle(0.35f, 0.42f, 0.055f, fill)
        c.drawCircle(0.65f, 0.42f, 0.055f, fill)
        path.reset()
        when (kind) {
            Kind.FACE_CALM -> { path.moveTo(0.32f, 0.6f); path.quadTo(0.5f, 0.78f, 0.68f, 0.6f) }
            Kind.FACE_UNEASY -> { path.moveTo(0.34f, 0.66f); path.lineTo(0.66f, 0.66f) }
            else -> { path.moveTo(0.32f, 0.72f); path.quadTo(0.41f, 0.62f, 0.5f, 0.7f); path.quadTo(0.59f, 0.78f, 0.68f, 0.66f) }
        }
        c.drawPath(path, stroke)
    }
}
