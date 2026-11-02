package ai.motioncare.app.theme

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.view.View

/** Draws the chosen [MoodTheme]; animates every frame while the theme is animated and visible. */
class MoodIconView(context: Context) : View(context) {
    private val start = SystemClock.uptimeMillis()
    private var shownLevel = 0f

    var theme: MoodTheme = CleanTheme
        set(value) { field = value; invalidate() }
    var mood: Mood = Mood.CALM
        set(value) { field = value; invalidate() }
    var level: Float = 0f
        set(value) { field = if (value.isNaN()) 0f else value.coerceIn(0f, 1.5f); invalidate() }

    override fun onDraw(c: Canvas) {
        // ease toward the new level so the face changes smoothly, not in 1-second steps
        shownLevel += (level - shownLevel) * 0.15f
        val t = (SystemClock.uptimeMillis() - start) / 1000f
        val size = minOf(width, height).toFloat()
        theme.draw(c, width / 2f, height / 2f, size, mood, shownLevel, t)
        if (theme.animated || kotlin.math.abs(level - shownLevel) > 0.01f) postInvalidateOnAnimation()
    }
}
