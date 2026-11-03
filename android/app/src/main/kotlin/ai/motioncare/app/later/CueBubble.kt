package ai.motioncare.app.later

import ai.motioncare.app.ui.ManeuverView
import ai.motioncare.app.ui.RiskGauge
import ai.motioncare.app.ui.Ui
import ai.motioncare.app.ui.Ui.dp
import ai.motioncare.app.ui.Ui.dpf
import ai.motioncare.app.ui.Ui.gone
import ai.motioncare.app.ui.Ui.text
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import kotlin.math.roundToInt

private const val CUE_SHOW_MS = 4000L

/**
 * Parked: the map's turn cue bubble (big arrow + distance, under the character). [show] when a
 * cue fires, [follow] with every sample's distance to the turn; it hides once the turn is behind.
 */
class CueBubble(context: Context) : LinearLayout(context) {
    private val arrow = ManeuverView(context)
    private val dist = context.text("", 30f, Color.WHITE, bold = true)
    private val hide = Runnable { cueAt = -1.0; animate().alpha(0f).setDuration(250).withEndAction { gone(true) }.start() }
    private var cueAt = -1.0
    private var lastTurnM = -1.0

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = Ui.rounded(Color.parseColor("#111317"), context.dpf(16f), context.dp(3), RiskGauge.AMBER)
        elevation = context.dpf(6f)
        setPadding(context.dp(8), context.dp(6), context.dp(14), context.dp(6))
        addView(arrow, LayoutParams(context.dp(56), context.dp(56)))
        dist.maxLines = 1
        addView(dist, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = context.dp(4) })
        gone(true)
    }

    fun show(left: Boolean) {
        arrow.kind = if (left) ManeuverView.Kind.LEFT else ManeuverView.Kind.RIGHT
        cueAt = lastTurnM
        dist.text = cueText(lastTurnM)
        removeCallbacks(hide)
        animate().cancel()
        if (visibility != VISIBLE) { alpha = 0f; translationY = -context.dpf(10f) }
        gone(false)
        animate().alpha(1f).translationY(0f).setDuration(220).start()
        postDelayed(hide, CUE_SHOW_MS)
    }

    /** Counts the cue down with each sample; a growing distance means the turn is behind us. */
    fun follow(turnM: Double) {
        val prev = lastTurnM
        lastTurnM = turnM
        if (visibility != VISIBLE || cueAt < 0) return
        if (turnM < 0 || (prev >= 0 && turnM > prev + 5)) { removeCallbacks(hide); hide.run(); return }
        dist.text = cueText(turnM)
    }

    fun reset() {
        removeCallbacks(hide)
        gone(true)
        cueAt = -1.0
        lastTurnM = -1.0
    }

    private fun cueText(m: Double) = if (m in 0.0..30.0 || m < 0) "곧" else "${(m / 10).roundToInt() * 10}m"
}
