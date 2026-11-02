package ai.motioncare.app.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.text.LineBreakConfig
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Shared look: large touch targets (≥ 64 dp) and high-contrast text for an in-car screen. */
object Ui {
    val BG = Color.parseColor("#F4F6FA")
    val CARD = Color.WHITE
    val INK = Color.parseColor("#1F2340")
    val MUTED = Color.parseColor("#6B6E86")
    val LINE = Color.parseColor("#E3E5EE")
    val PRIMARY = Color.parseColor("#0F766E")
    val PRIMARY_SOFT = Color.parseColor("#E1F5EE")
    val ALERT = Color.parseColor("#C2410C")
    val ALERT_SOFT = Color.parseColor("#FDEBDD")

    /** Car screens are wide and seen from arm's length: everything is drawn a third larger than phone sizes. */
    const val SCALE = 1.35f

    fun Context.dp(v: Int) = (v * resources.displayMetrics.density * SCALE).toInt()
    fun Context.dpf(v: Float) = v * resources.displayMetrics.density * SCALE

    fun rounded(color: Int, radius: Float, stroke: Int = 0, strokeColor: Int = 0) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius
        if (stroke > 0) setStroke(stroke, strokeColor)
    }

    fun Context.text(s: String, sp: Float, color: Int = INK, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = sp * SCALE; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        phraseBreaks()
    }

    /** Korean wraps at phrase boundaries, never mid-word or leaving a lone syllable (API 33+). */
    fun TextView.phraseBreaks() {
        if (Build.VERSION.SDK_INT >= 33) lineBreakWordStyle = LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE
    }

    /** Pill button. [filled] for the main action on a screen. */
    fun Context.pill(label: String, filled: Boolean = false, color: Int = PRIMARY, onClick: () -> Unit) = TextView(this).apply {
        text = label; textSize = 18f * SCALE; gravity = Gravity.CENTER
        typeface = Typeface.DEFAULT_BOLD
        phraseBreaks()
        setTextColor(if (filled) Color.WHITE else color)
        background = rounded(if (filled) color else CARD, dpf(28f), dp(2), color)
        minHeight = dp(56); minWidth = dp(96)
        setPadding(dp(22), dp(10), dp(22), dp(10))
        isClickable = true; isFocusable = true
        setOnClickListener { onClick() }
    }

    fun Context.card(vertical: Boolean = true) = LinearLayout(this).apply {
        orientation = if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        background = rounded(CARD, dpf(20f))
        setPadding(dp(20), dp(18), dp(20), dp(18))
        elevation = dpf(1f)
    }

    fun lp(w: Int, h: Int, weight: Float = 0f, margin: Int = 0) = LinearLayout.LayoutParams(w, h, weight).apply { setMargins(margin, margin, margin, margin) }

    fun View.gone(hide: Boolean) { visibility = if (hide) View.GONE else View.VISIBLE }
}
