package ai.motioncare.app.theme

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.view.View
import ai.motioncare.app.ui.RiskRing

/** Draws the chosen [MoodTheme]; animates every frame while the theme is animated and visible. */
class MoodIconView(context: Context) : View(context) {
    private val start = SystemClock.uptimeMillis()

    init { IdolTheme.attach(context) }
    private var shownLevel = 0f
    private var shownRing = Float.NaN

    var theme: MoodTheme = CleanTheme
        set(value) { field = value; invalidate() }
    var mood: Mood = Mood.CALM
        set(value) { field = value; invalidate() }
    var level: Float = 0f
        set(value) { field = if (value.isNaN()) 0f else value.coerceIn(0f, 1.5f); invalidate() }
    /** Chance of sickness 0..1 for the ring around the character ([ai.motioncare.app.ui.RiskRing]); NaN hides it. */
    var ring: Float = Float.NaN
        set(value) { field = if (value.isNaN()) value else value.coerceIn(0f, 1f); invalidate() }

    override fun onDraw(c: Canvas) {
        // ease toward the new level so the face changes smoothly, not in 1-second steps
        shownLevel += (level - shownLevel) * 0.15f
        shownRing = when {
            ring.isNaN() -> Float.NaN
            shownRing.isNaN() -> ring
            else -> shownRing + (ring - shownRing) * 0.15f
        }
        val t = (SystemClock.uptimeMillis() - start) / 1000f
        val size = minOf(width, height).toFloat()
        RiskRing.current = shownRing
        RiskRing.mood = mood
        try { theme.draw(c, width / 2f, height / 2f, size, mood, shownLevel, t) } finally { RiskRing.current = Float.NaN; RiskRing.mood = null }
        val ringMoving = !ring.isNaN() && kotlin.math.abs(ring - shownRing) > 0.002f
        if (theme.animated || kotlin.math.abs(level - shownLevel) > 0.01f || ringMoving) postInvalidateOnAnimation()
    }
}
