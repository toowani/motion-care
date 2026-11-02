package ai.motioncare.app.ui

import ai.motioncare.app.DashboardView
import ai.motioncare.app.theme.Mood
import ai.motioncare.app.theme.MoodIconView
import ai.motioncare.app.theme.MoodTheme
import ai.motioncare.app.ui.Ui.card
import ai.motioncare.app.ui.Ui.dp
import ai.motioncare.app.ui.Ui.dpf
import ai.motioncare.app.ui.Ui.gone
import ai.motioncare.app.ui.Ui.lp
import ai.motioncare.app.ui.Ui.pill
import ai.motioncare.app.ui.Ui.text
import ai.motioncare.core.Output
import ai.motioncare.core.Sample
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Home screen. Left: what everyone in the car needs at a glance (themed mood icon, one line of
 * what the car is doing, three facts). Right: family bars and, for demos, the index graph.
 * Bottom: demo controls (scenario replay / live signals), hidden when the demo panel is off.
 */
class HomeView(context: Context, private val callbacks: Callbacks) : LinearLayout(context) {
    interface Callbacks {
        fun onFamilyToggle()
        fun onReportSick()
        fun onSettings()
        fun onScenario(file: String)
        fun onLive()
        fun onStopTrip()
    }

    val dashboard = DashboardView(context)
    private val icon = MoodIconView(context)
    private val moodLabel = context.text("", 34f, bold = true).apply { gravity = Gravity.CENTER }
    private val advice = context.text("", 20f, Ui.MUTED).apply { gravity = Gravity.CENTER; setLineSpacing(0f, 1.15f) }
    private val chipSpeed = chip("속도", "—")
    private val chipAir = chip("실내", "—")
    private val chipTurn = chip("다음 회전", "—")
    private val themeName = context.text("", 15f, Ui.MUTED)
    private val familyChip: TextView
    private val familyBars = FamilyBars(context)
    private val familyCard: LinearLayout
    private val graphCard: LinearLayout
    private val summary = context.text("", 21f, Ui.INK).apply { setLineSpacing(0f, 1.35f) }
    private val summaryCard: LinearLayout
    private val demoBar: HorizontalScrollView
    private val promptText = context.text("", 24f, bold = true).apply { gravity = Gravity.CENTER }
    private val promptButtons = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER }
    private val promptBox = LinearLayout(context).apply {
        orientation = VERTICAL; gravity = Gravity.CENTER
        background = Ui.rounded(Ui.PRIMARY_SOFT, context.dpf(18f), context.dp(2), Ui.PRIMARY)
        setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(12))
        addView(promptText, lp(MATCH_PARENT, WRAP_CONTENT))
        addView(promptButtons, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(10) })
        visibility = GONE
    }

    private var familyKey = ""
    private var actionText: String? = null
    private var actionUntil = 0L
    private var mood = Mood.CALM
    private var theme: MoodTheme? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(Ui.BG)
        val pad = context.dp(20)
        setPadding(pad, context.dp(14), pad, context.dp(10))

        // top bar
        val top = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val titles = LinearLayout(context).apply {
            orientation = VERTICAL
            addView(context.text("멀미 케어", 26f, bold = true))
            addView(themeName)
        }
        top.addView(titles, lp(0, WRAP_CONTENT, 1f))
        familyChip = context.pill("가족 모드 끔") { callbacks.onFamilyToggle() }
        top.addView(familyChip, lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(12) })
        top.addView(context.pill("멀미 났어", filled = true, color = Ui.ALERT) { callbacks.onReportSick() },
            lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(12) })
        top.addView(context.pill("설정") { callbacks.onSettings() }, lp(WRAP_CONTENT, WRAP_CONTENT))
        addView(top, lp(MATCH_PARENT, WRAP_CONTENT))

        // body
        val body = LinearLayout(context).apply { orientation = HORIZONTAL }
        val left = context.card().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(icon, lp(MATCH_PARENT, 0, 1f))
            addView(moodLabel, lp(MATCH_PARENT, WRAP_CONTENT))
            addView(advice, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(6) })
            addView(promptBox, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(10) })
            val chips = LinearLayout(context).apply { orientation = HORIZONTAL }
            listOf(chipSpeed, chipAir, chipTurn).forEach { chips.addView(it, lp(0, WRAP_CONTENT, 1f).apply { setMargins(context.dp(4), 0, context.dp(4), 0) }) }
            addView(chips, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(14) })
        }
        body.addView(left, lp(0, MATCH_PARENT, 0.4f).apply { setMargins(0, context.dp(14), context.dp(14), context.dp(10)) })

        val right = LinearLayout(context).apply { orientation = VERTICAL }
        familyCard = context.card().apply {
            addView(context.text("가족 · 아이별 멀미 가능성", 17f, Ui.MUTED, bold = true))
            addView(familyBars, lp(MATCH_PARENT, context.dp(96)).apply { topMargin = context.dp(6) })
        }
        right.addView(familyCard, lp(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = context.dp(14) })
        graphCard = context.card().apply {
            setPadding(context.dp(8), context.dp(10), context.dp(8), context.dp(8))
            addView(dashboard, lp(MATCH_PARENT, MATCH_PARENT))
        }
        right.addView(graphCard, lp(MATCH_PARENT, 0, 1f))
        summaryCard = context.card().apply {
            addView(context.text("이번 주행", 17f, Ui.MUTED, bold = true))
            addView(summary, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(8) })
        }
        right.addView(summaryCard, lp(MATCH_PARENT, 0, 1f))
        body.addView(right, lp(0, MATCH_PARENT, 0.6f).apply { setMargins(0, context.dp(14), 0, context.dp(10)) })
        addView(body, lp(MATCH_PARENT, 0, 1f))

        // demo controls
        val demo = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        demo.addView(context.text("시연", 16f, Ui.MUTED, bold = true), lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(12) })
        listOf(
            "굽은 길 · 거칠게" to "B_winding_rough.csv",
            "굽은 길 · 부드럽게" to "B_winding_smooth.csv",
            "도심 · 거칠게" to "A_city_rough.csv",
            "가족 귀성" to "D_family.csv",
        ).forEach { (label, file) ->
            demo.addView(context.pill(label) { callbacks.onScenario(file) }, lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(10) })
        }
        demo.addView(context.pill("실차 신호", filled = true) { callbacks.onLive() }, lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(10) })
        demo.addView(context.pill("정지", color = Ui.INK) { callbacks.onStopTrip() }, lp(WRAP_CONTENT, WRAP_CONTENT))
        demoBar = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false; addView(demo) }
        addView(demoBar, lp(MATCH_PARENT, WRAP_CONTENT))

        showMood(Mood.CALM, 0f)
        updateSummary(0, 0, 0, 0f)
    }

    private fun chip(title: String, value: String) = LinearLayout(context).apply {
        orientation = VERTICAL; gravity = Gravity.CENTER
        background = Ui.rounded(Ui.BG, context.dpf(14f))
        setPadding(context.dp(8), context.dp(10), context.dp(8), context.dp(10))
        addView(context.text(title, 15f, Ui.MUTED).apply { gravity = Gravity.CENTER })
        addView(context.text(value, 19f, bold = true).apply { gravity = Gravity.CENTER; tag = "value" })
    }

    private fun LinearLayout.setChip(value: String) { findViewWithTag<TextView>("value").text = value }

    fun setTheme(t: MoodTheme) {
        theme = t
        icon.theme = t
        themeName.text = "아이콘: ${t.title}"
        moodLabel.text = labelFor(t, mood)
    }

    /** Every theme shows the same level word first, so a parent can read the risk in any theme. */
    private fun labelFor(t: MoodTheme, m: Mood): String {
        val word = when (m) { Mood.CALM -> "편안"; Mood.UNEASY -> "주의"; Mood.SICK -> "위험" }
        val own = t.label(m)
        return if (own.contains(word)) own else "$word · $own"
    }

    /** Clears the mood, chips and bars when a trip ends or the signal source changes. */
    fun resetTrip() {
        actionUntil = 0L
        showMood(Mood.CALM, 0f)
        listOf(chipSpeed, chipAir, chipTurn).forEach { it.setChip("—") }
        familyBars.set(familyBars.names().map { it to 0.0 })
        hidePrompt()
    }

    /**
     * A question with large answer buttons inside the home screen (no system dialog while driving).
     * [answers] are (label, action) pairs; the first is drawn filled.
     */
    fun prompt(question: String, answers: List<Pair<String, () -> Unit>>) {
        promptText.text = question
        promptButtons.removeAllViews()
        answers.forEachIndexed { i, (label, action) ->
            promptButtons.addView(context.pill(label, filled = i == 0) { hidePrompt(); action() },
                lp(0, WRAP_CONTENT, 1f).apply { setMargins(context.dp(5), 0, context.dp(5), 0) })
        }
        promptBox.visibility = VISIBLE
    }

    fun hidePrompt() { promptBox.visibility = GONE }

    fun setDemoPanel(on: Boolean) {
        demoBar.gone(!on)
        graphCard.gone(!on)
        summaryCard.gone(on)
    }

    fun setFamily(on: Boolean, names: List<String>) {
        familyChip.text = if (on) "가족 모드 · " + names.joinToString(", ") else "가족 모드 끔"
        familyChip.setTextColor(if (on) Color.WHITE else Ui.PRIMARY)
        familyChip.background = Ui.rounded(if (on) Ui.PRIMARY else Ui.CARD, context.dpf(28f), context.dp(2), Ui.PRIMARY)
        familyCard.gone(!on)
        // only reset the bars when the family itself changed, not on every settings change mid-trip
        val key = "$on:" + names.joinToString(",")
        if (key != familyKey) { familyKey = key; familyBars.set(names.map { it to 0.0 }) }
    }

    fun setFamilyProbabilities(values: List<Pair<String, Double>>) = familyBars.set(values)

    /** Shows what the car just did for a few seconds instead of the generic advice. */
    fun showAction(msg: String) {
        actionText = msg
        actionUntil = SystemClock.uptimeMillis() + 8000
        advice.text = msg
        advice.setTextColor(Ui.PRIMARY)
    }

    fun update(s: Sample, o: Output, nextTurnM: Double?) {
        val level = if (o.thetaHigh > 0) (o.index / o.thetaHigh).toFloat() else 0f
        showMood(Mood.of(o.state), level)
        chipSpeed.setChip("%.0f km/h".format(s.speed * 3.6))
        chipAir.setChip("%.1f° · %s".format(s.cabinTemp, if (s.recirc) "내기" else "외기"))
        chipTurn.setChip(nextTurnM?.takeIf { it >= 0 }?.let { if (it >= 1000) "%.1f km".format(it / 1000) else "%.0f m".format(it) } ?: "—")
    }

    private fun showMood(m: Mood, level: Float) {
        mood = m
        icon.mood = m
        icon.level = level
        theme?.let { moodLabel.text = labelFor(it, m) }
        moodLabel.setTextColor(when (m) { Mood.CALM -> Ui.PRIMARY; Mood.UNEASY -> Color.parseColor("#B45309"); Mood.SICK -> Ui.ALERT })
        if (SystemClock.uptimeMillis() > actionUntil) {
            advice.setTextColor(Ui.MUTED)
            advice.text = when (m) {
                Mood.CALM -> "편안하게 가고 있어요"
                Mood.UNEASY -> "흔들림이 늘고 있어요\n창밖 먼 곳을 보면 좋아요"
                Mood.SICK -> "멀미가 나기 쉬워요\n바람을 넣고 쉬어 갈 곳을 찾을게요"
            }
        }
    }

    fun updateSummary(minutes: Int, cues: Int, airs: Int, peak: Float) {
        val peakText = when { peak >= 1f -> "높음"; peak >= 0.6f -> "주의"; else -> "편안" }
        summary.text = "주행 ${minutes}분 · 가장 흔들린 순간: $peakText\n회전 예고 ${cues}번 · 공기 바꾸기 ${airs}번\n\n" +
            "멀미 줄이는 습관\n· 창밖 먼 곳 보기 · 화면은 짧게\n· 바람 쐬기 · 출발 전 가볍게 먹기"
    }

    fun note(msg: String) = dashboard.note(msg)

    /** One row per child: name, probability bar, word. */
    private class FamilyBars(context: Context) : View(context) {
        private val AMBER = Color.parseColor("#B45309")
        private var rows: List<Pair<String, Double>> = emptyList()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = context.dpf(18f); color = Ui.INK; isFakeBoldText = true }
        private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = context.dpf(18f); textAlign = Paint.Align.RIGHT; isFakeBoldText = true }
        private val r = context.dpf(8f)

        fun set(values: List<Pair<String, Double>>) { rows = values; invalidate() }
        fun names() = rows.map { it.first }

        override fun onDraw(c: Canvas) {
            if (rows.isEmpty()) return
            val rowH = height / rows.size.toFloat()
            val nameW = context.dpf(80f)
            val wordW = context.dpf(64f)
            rows.forEachIndexed { i, (name, p) ->
                val cy = rowH * i + rowH / 2
                c.drawText(name, 0f, cy + text.textSize / 3, text)
                val x0 = nameW; val x1 = width - wordW
                paint.color = Ui.LINE
                c.drawRoundRect(x0, cy - r, x1, cy + r, r, r, paint)
                paint.color = when { p >= 0.5 -> Ui.ALERT; p >= 0.25 -> AMBER; else -> Ui.PRIMARY }
                c.drawRoundRect(x0, cy - r, x0 + (x1 - x0) * p.toFloat().coerceIn(0.03f, 1f), cy + r, r, r, paint)
                small.color = paint.color
                c.drawText(when { p >= 0.5 -> "위험"; p >= 0.25 -> "주의"; else -> "편안" }, width.toFloat(), cy + small.textSize / 3, small)
            }
        }
    }
}
