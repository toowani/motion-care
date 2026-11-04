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
import android.graphics.Color
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Home screen, built for a glance (people in a car do not read): big numbers, pictograms and
 * colour instead of sentences. Two tabs: 지도 (navigation-style map with the character widget,
 * the default) and 분석 (risk gauge, trip tiles, habits and the graphs). The narrow pane beside
 * the Pleos navigation shows only the face card. Bottom: demo controls, hidden when the demo panel is off.
 * Family mode and turn cues are parked in [ai.motioncare.app.later].
 */
private const val COMPACT_BELOW_DP = 900
private const val TAB_MAP = 0
private const val TAB_ANALYSIS = 1

class HomeView(context: Context, private val callbacks: Callbacks) : LinearLayout(context) {
    interface Callbacks {
        fun onRelief()
        fun onSettings()
        fun onScenario(file: String)
        fun onLive()
        fun onStopTrip()
        /** The app became (or stopped being) the narrow pane beside the navigation. */
        fun onCompactChanged(compact: Boolean) {}
    }

    val dashboard = DashboardView(context)
    private val icon = MoodIconView(context)
    private val moodLabel = context.text("", 34f, bold = true).apply {
        gravity = Gravity.CENTER; maxLines = 1
        // long theme words shrink instead of wrapping
        setAutoSizeTextTypeUniformWithConfiguration((22 * Ui.SCALE).toInt(), (34 * Ui.SCALE).toInt(), 1, TypedValue.COMPLEX_UNIT_SP)
    }
    private val advice = context.text("", 22f, Ui.MUTED, bold = true).apply { gravity = Gravity.CENTER; setLineSpacing(0f, 1.1f) }
    private val chipSpeed = chip("속도 km/h", "—")
    private val chipAir = chip("실내", "—")
    /** Compact pane: the chance of sickness as one big coloured pill. */
    private val riskPill = context.text("", 26f, Color.WHITE, bold = true).apply {
        gravity = Gravity.CENTER; maxLines = 1
        setAutoSizeTextTypeUniformWithConfiguration((16 * Ui.SCALE).toInt(), (26 * Ui.SCALE).toInt(), 1, TypedValue.COMPLEX_UNIT_SP)
        setPadding(context.dp(16), context.dp(6), context.dp(16), context.dp(6))
    }
    private var lastRisk = 0.0
    private val demoPills = mutableMapOf<String?, TextView>()
    private val tabs: LinearLayout
    private val body: LinearLayout
    private val rightHost = FrameLayout(context)
    private val mapFrame = FrameLayout(context)
    private val leftCard: LinearLayout
    private var demoOn = true
    /** One app pane next to the navigation (Pleos lets an app take one of two panes): character only. */
    private var compact = false
    val isCompact get() = compact
    private val gauge = RiskGauge(context)
    private val statTime = statTile(Glyph.Kind.CLOCK, "주행 시간")
    private val statAir = statTile(Glyph.Kind.WIND, "공기 바꿈")
    private val statRelief = statTile(Glyph.Kind.RELIEF, "완화 버튼")
    private val statPeak = statTile(Glyph.Kind.FACE_CALM, "최고 흔들림")
    private val tabMap: TextView
    private val tabAnalysis: TextView
    private val map = MapPanel(context)
    private val mapCard: LinearLayout
    private val analysis: LinearLayout
    private var tab = TAB_MAP
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

    /** Wide screen: what the car just did, as one dark pill over the top of the tab for a few seconds. */
    private val banner = context.text("", 24f, Color.WHITE, bold = true).apply {
        gravity = Gravity.CENTER; maxLines = 1
        background = Ui.rounded(Color.parseColor("#111317"), context.dpf(26f), context.dp(3), Ui.RELIEF)
        setPadding(context.dp(26), context.dp(12), context.dp(26), context.dp(12))
        elevation = context.dpf(8f)
        visibility = GONE
    }
    private val hideBanner = Runnable { banner.animate().alpha(0f).setDuration(250).withEndAction { banner.gone(true) }.start() }
    private var actionUntil = 0L
    private var mood = Mood.CALM
    private var theme: MoodTheme? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(Ui.BG)
        val pad = context.dp(20)
        setPadding(pad, context.dp(14), pad, context.dp(10))

        // top bar: title, the tabs, then 멀미 완화 (large, warm) and settings
        val top = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(context.text("멀미 케어", 26f, bold = true).apply { maxLines = 1 }, lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(24) })
        tabs = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(tabs, lp(0, WRAP_CONTENT, 1f))
        top.addView(reliefButton(), lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(12) })
        top.addView(context.pill("설정") { callbacks.onSettings() }, lp(WRAP_CONTENT, WRAP_CONTENT))
        addView(top, lp(MATCH_PARENT, WRAP_CONTENT))

        tabMap = context.pill("지도", filled = true) { showTab(TAB_MAP) }
        tabAnalysis = context.pill("분석") { showTab(TAB_ANALYSIS) }
        tabs.addView(tabMap, lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(10) })
        tabs.addView(tabAnalysis, lp(WRAP_CONTENT, WRAP_CONTENT))

        // body: the face card is the compact pane; the wide screen shows the tabs instead
        body = LinearLayout(context).apply { orientation = HORIZONTAL }
        leftCard = context.card().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            // the face is the shake right now; the pill under it is the chance of sickness
            addView(context.text("지금 흔들림", 16f, Ui.MUTED, bold = true).apply { maxLines = 1 }, lp(MATCH_PARENT, WRAP_CONTENT))
            addView(icon, lp(MATCH_PARENT, 0, 1f))
            // fixed height: auto-size never grows back with WRAP_CONTENT (stayed small after compact mode)
            addView(moodLabel, lp(MATCH_PARENT, context.dp(48)))
            addView(riskPill, lp(WRAP_CONTENT, WRAP_CONTENT).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = context.dp(6) })
            addView(advice, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(6) })
            val chips = LinearLayout(context).apply { orientation = HORIZONTAL }
            listOf(chipSpeed, chipAir).forEach { chips.addView(it, lp(0, WRAP_CONTENT, 1f).apply { setMargins(context.dp(4), 0, context.dp(4), 0) }) }
            addView(chips, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(14) })
        }
        body.addView(leftCard, lp(0, MATCH_PARENT, 1f).apply { setMargins(0, context.dp(14), 0, context.dp(10)) })

        // 지도 tab: our own map with the character badge on top
        mapCard = context.card().apply {
            setPadding(0, 0, 0, 0)
            clipToOutline = true
            mapFrame.addView(map, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(mapFrame, lp(MATCH_PARENT, MATCH_PARENT))
        }
        rightHost.addView(mapCard, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        analysis = analysisTab()
        rightHost.addView(analysis, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        rightHost.addView(banner, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = context.dp(14) })
        body.addView(rightHost, lp(0, MATCH_PARENT, 1f).apply { setMargins(0, context.dp(14), 0, context.dp(10)) })
        addView(body, lp(MATCH_PARENT, 0, 1f))

        // demo controls
        val demo = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        demo.addView(context.text("시연", 16f, Ui.MUTED, bold = true), lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(12) })
        listOf(
            "굽은 길 · 거칠게" to "B_winding_rough.csv",
            "굽은 길 · 부드럽게" to "B_winding_smooth.csv",
            "도심 · 거칠게" to "A_city_rough.csv",
            "나들이길" to "D_family.csv",
        ).forEach { (label, file) ->
            val pill = context.pill(label) { callbacks.onScenario(file) }
            demoPills[file.removeSuffix(".csv")] = pill
            demo.addView(pill, lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(10) })
        }
        val livePill = context.pill("실차 신호") { callbacks.onLive() }
        demoPills[null] = livePill
        demo.addView(livePill, lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(10) })
        demo.addView(context.pill("정지", color = Ui.INK) { callbacks.onStopTrip() }, lp(WRAP_CONTENT, WRAP_CONTENT))
        demoBar = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false; addView(demo) }
        addView(demoBar, lp(MATCH_PARENT, WRAP_CONTENT))

        showMood(Mood.CALM, 0f)
        updateSummary(0, 0, 0, Mood.CALM)
        applyCompact()
        markSource(null)
    }

    /** The one button a passenger may press while moving: big, warm and filled. */
    private fun reliefButton() = context.pill("멀미 완화", filled = true, color = Ui.RELIEF) { callbacks.onRelief() }.apply {
        textSize = 22f * Ui.SCALE
        maxLines = 1
        minHeight = context.dp(64); minWidth = context.dp(170)
        setPadding(context.dp(30), context.dp(10), context.dp(30), context.dp(10))
        elevation = context.dpf(2f)
    }

    /**
     * 분석 tab: left, the chance of sickness and this trip's tiles; right, the graph (흔들림 지수 /
     * 멀미 가능성) with the event log and the habit pictograms under it.
     */
    private fun analysisTab() = LinearLayout(context).apply {
        orientation = HORIZONTAL
        val left = LinearLayout(context).apply { orientation = VERTICAL }
        left.addView(context.card().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(context.text("멀미 가능성", 16f, Ui.MUTED, bold = true), lp(MATCH_PARENT, WRAP_CONTENT))
            addView(gauge, lp(MATCH_PARENT, 0, 1f).apply { topMargin = context.dp(8) })
        }, lp(MATCH_PARENT, 0, 1f))
        left.addView(context.card().apply {
            addView(context.text("이번 주행", 16f, Ui.MUTED, bold = true))
            listOf(statTime to statAir, statRelief to statPeak).forEach { (a, b) ->
                val row = LinearLayout(context).apply { orientation = HORIZONTAL }
                row.addView(a, lp(0, MATCH_PARENT, 1f).apply { marginEnd = context.dp(8) })
                row.addView(b, lp(0, MATCH_PARENT, 1f))
                addView(row, lp(MATCH_PARENT, 0, 1f).apply { topMargin = context.dp(8) })
            }
        }, lp(MATCH_PARENT, 0, 1.25f).apply { topMargin = context.dp(12) })
        addView(left, lp(0, MATCH_PARENT, 0.38f).apply { marginEnd = context.dp(12) })

        val right = LinearLayout(context).apply { orientation = VERTICAL }
        right.addView(context.card().apply {
            setPadding(context.dp(8), context.dp(10), context.dp(8), context.dp(8))
            addView(graphToggle(), lp(WRAP_CONTENT, WRAP_CONTENT).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = context.dp(6) })
            addView(dashboard, lp(MATCH_PARENT, 0, 1f))
        }, lp(MATCH_PARENT, 0, 1f))
        right.addView(habitsCard(), lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(12) })
        addView(right, lp(0, MATCH_PARENT, 0.62f))
    }

    /** Filled pill = what is feeding the screen now (a scenario, or the car when [name] is null). */
    private fun markSource(name: String?) = demoPills.forEach { (k, v) ->
        val on = k == name
        v.setTextColor(if (on) Color.WHITE else Ui.PRIMARY)
        v.background = Ui.rounded(if (on) Ui.PRIMARY else Ui.CARD, context.dpf(28f), context.dp(2), Ui.PRIMARY)
    }

    /** 분석 tab: one graph at a time, switched by a two-way segmented pill. */
    private fun graphToggle(): LinearLayout {
        val bar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            background = Ui.rounded(Ui.CARD, context.dpf(28f), context.dp(2), Ui.PRIMARY)
            setPadding(context.dp(3), context.dp(3), context.dp(3), context.dp(3))
        }
        val modes = listOf("흔들림 지수" to DashboardView.Mode.INDEX, "멀미 가능성" to DashboardView.Mode.RISK)
        val segs = modes.map { (label, _) ->
            context.text(label, 18f, Ui.PRIMARY, bold = true).apply {
                gravity = Gravity.CENTER
                minHeight = context.dp(48); minWidth = context.dp(150)
                setPadding(context.dp(18), context.dp(6), context.dp(18), context.dp(6))
                isClickable = true; isFocusable = true
            }
        }
        fun paint() = segs.forEachIndexed { i, v ->
            val on = modes[i].second == dashboard.mode
            v.setTextColor(if (on) Color.WHITE else Ui.PRIMARY)
            v.background = if (on) Ui.rounded(Ui.PRIMARY, context.dpf(25f)) else null
        }
        segs.forEachIndexed { i, v ->
            v.setOnClickListener { dashboard.mode = modes[i].second; paint() }
            bar.addView(v, lp(WRAP_CONTENT, WRAP_CONTENT))
        }
        paint()
        return bar
    }

    private fun chip(title: String, value: String) = LinearLayout(context).apply {
        orientation = VERTICAL; gravity = Gravity.CENTER
        background = Ui.rounded(Ui.BG, context.dpf(14f))
        setPadding(context.dp(6), context.dp(8), context.dp(6), context.dp(8))
        addView(context.text(title, 16f, Ui.MUTED).apply { gravity = Gravity.CENTER; maxLines = 1; tag = "title" })
        addView(context.text(value, 26f, bold = true).apply { gravity = Gravity.CENTER; maxLines = 1; tag = "value" })
    }

    private fun LinearLayout.setChip(value: String, title: String? = null) {
        findViewWithTag<TextView>("value").text = value
        title?.let { findViewWithTag<TextView>("title").text = it }
    }

    /** Pictogram + big value + 1–2 word label, on a soft tile. */
    private fun statTile(kind: Glyph.Kind, label: String) = LinearLayout(context).apply {
        orientation = VERTICAL; gravity = Gravity.CENTER
        background = Ui.rounded(Ui.BG, context.dpf(14f))
        setPadding(context.dp(6), context.dp(6), context.dp(6), context.dp(6))
        addView(Glyph(context, kind, if (kind == Glyph.Kind.RELIEF) Ui.RELIEF else Ui.PRIMARY).apply { tag = "glyph" }, lp(context.dp(48), context.dp(48)))
        addView(context.text("0", 34f, bold = true).apply { gravity = Gravity.CENTER; maxLines = 1; tag = "value" }, lp(WRAP_CONTENT, WRAP_CONTENT))
        addView(context.text(label, 16f, Ui.MUTED, bold = true).apply { gravity = Gravity.CENTER; maxLines = 1 }, lp(WRAP_CONTENT, WRAP_CONTENT))
    }

    private fun LinearLayout.setStat(value: String, color: Int = Ui.INK) {
        findViewWithTag<TextView>("value").apply { text = value; setTextColor(color) }
    }

    /** Four habits as pictograms with a two-word caption, no bullet text. */
    private fun habitsCard() = context.card().apply {
        setPadding(context.dp(16), context.dp(12), context.dp(16), context.dp(12))
        addView(context.text("멀미 줄이는 습관", 16f, Ui.MUTED, bold = true))
        val row = LinearLayout(context).apply { orientation = HORIZONTAL }
        listOf(
            Glyph.Kind.WINDOW to "창밖 보기",
            Glyph.Kind.PHONE to "화면 짧게",
            Glyph.Kind.WIND to "바람 쐬기",
            Glyph.Kind.FOOD to "가볍게 먹기",
        ).forEachIndexed { i, (kind, caption) ->
            row.addView(LinearLayout(context).apply {
                orientation = VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                background = Ui.rounded(Ui.PRIMARY_SOFT, context.dpf(14f))
                setPadding(context.dp(4), context.dp(10), context.dp(4), context.dp(8))
                addView(Glyph(context, kind, Ui.PRIMARY), lp(context.dp(48), context.dp(48)))
                addView(context.text(caption, 17f, Ui.INK, bold = true).apply { gravity = Gravity.CENTER; maxLines = 1 },
                    lp(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = context.dp(6) })
            }, lp(0, WRAP_CONTENT, 1f).apply { if (i < 3) marginEnd = context.dp(8) })
        }
        addView(row, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(8) })
    }

    fun setTheme(t: MoodTheme) {
        theme = t
        icon.theme = t
        moodLabel.text = labelFor(t, mood)
    }

    /** Every theme shows the same shake word first, so a parent can read the face in any theme. */
    private fun labelFor(t: MoodTheme, m: Mood): String {
        val word = shakeWord(m)
        val own = t.label(m)
        return if (own.contains(word)) own else "$word · $own"
    }

    /** Clears the mood, chips and gauge when a trip ends or the signal source changes. */
    fun resetTrip() {
        actionUntil = 0L
        lastRisk = 0.0
        updateRiskPill()
        showMood(Mood.CALM, 0f)
        icon.ring = 0f
        chipSpeed.setChip("—")
        chipAir.setChip("—", "실내")
        gauge.set(0.0)
        map.reset()
        hidePrompt()
    }

    /**
     * A question with large answer buttons inside the home screen (no system dialog while driving).
     * [answers] are (label, action) pairs; the first is drawn filled.
     */
    fun prompt(question: String, answers: List<Pair<String, () -> Unit>>) {
        // one sentence per line, so a wrap never lands in the middle of the question
        promptText.text = question.replace(". ", ".\n").replace("? ", "?\n")
        promptButtons.removeAllViews()
        answers.forEachIndexed { i, (label, action) ->
            promptButtons.addView(context.pill(label, filled = i == 0) { hidePrompt(); action() },
                lp(0, WRAP_CONTENT, 1f).apply { setMargins(context.dp(5), 0, context.dp(5), 0) })
        }
        placePrompt()
        promptBox.visibility = VISIBLE
    }

    fun hidePrompt() { promptBox.visibility = GONE }

    /**
     * Compact: under the face. Wide: a card floating over the tab, so nothing changes size —
     * on the map above the speed badge (bottom-left), on 분석 in the middle.
     */
    private fun placePrompt() {
        val host = if (compact) leftCard else rightHost
        (promptBox.parent as? android.view.ViewGroup)?.takeIf { it !== host }?.removeView(promptBox)
        if (compact) {
            promptBox.elevation = 0f
            promptBox.background = Ui.rounded(Ui.PRIMARY_SOFT, context.dpf(18f), context.dp(2), Ui.PRIMARY)
            val at = leftCard.indexOfChild(advice) + 1
            if (promptBox.parent == null) leftCard.addView(promptBox, at, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(10) })
            return
        }
        promptBox.elevation = context.dpf(8f)
        promptBox.background = Ui.rounded(Ui.PRIMARY_SOFT, context.dpf(18f), context.dp(3), Ui.PRIMARY)
        val params = if (tab == TAB_MAP) FrameLayout.LayoutParams(context.dp(400), WRAP_CONTENT, Gravity.BOTTOM or Gravity.START).apply {
            // above the speed badge, clear of the arrival bar
            setMargins(context.dp(14), 0, 0, context.dp(16 + 84 + 12))
        } else FrameLayout.LayoutParams(context.dp(460), WRAP_CONTENT, Gravity.CENTER)
        if (promptBox.parent == null) rightHost.addView(promptBox, params) else promptBox.layoutParams = params
    }

    /** Demo panel = scenario replay buttons for judges. */
    fun setDemoPanel(on: Boolean) {
        demoOn = on
        demoBar.gone(!on || compact)
    }

    fun showTab(which: Int) {
        tab = which
        mapCard.gone(which != TAB_MAP)
        analysis.gone(which != TAB_ANALYSIS)
        placePrompt()
        listOf(tabMap to (which == TAB_MAP), tabAnalysis to (which == TAB_ANALYSIS)).forEach { (v, on) ->
            v.setTextColor(if (on) Color.WHITE else Ui.PRIMARY)
            v.background = Ui.rounded(if (on) Ui.PRIMARY else Ui.CARD, context.dpf(28f), context.dp(2), Ui.PRIMARY)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val c = w < context.dp(COMPACT_BELOW_DP)
        if (c != compact) { compact = c; post { applyCompact(); callbacks.onCompactChanged(compact) } }
    }

    /**
     * Narrow pane beside the navigation: only the big character, its level word, the chance of
     * sickness as a coloured pill and what the car just did. The tabs come back when widened.
     */
    private fun applyCompact() {
        leftCard.gone(!compact)
        rightHost.gone(compact)
        tabs.visibility = if (compact) INVISIBLE else VISIBLE
        demoBar.gone(compact || !demoOn)
        updateRiskPill()
        showTab(tab)
    }

    private fun updateRiskPill() {
        riskPill.text = "멀미 가능성 ${Ui.pct(lastRisk)}%"
        riskPill.background = Ui.rounded(Ui.riskColor(lastRisk), context.dpf(24f))
        // yellow needs dark text to stay legible
        riskPill.setTextColor(if (lastRisk >= 0.25 && lastRisk < 0.5) Ui.INK else Color.WHITE)
    }

    /** Shows what the car just did for a few seconds instead of the generic advice. */
    fun showAction(msg: String) {
        actionUntil = SystemClock.uptimeMillis() + 8000
        advice.text = msg
        advice.setTextColor(Ui.PRIMARY)
        banner.text = msg
        banner.animate().cancel()
        banner.alpha = 1f
        banner.gone(false)
        removeCallbacks(hideBanner)
        postDelayed(hideBanner, 6000)
    }

    /** [risk] = sickness probability of the passenger (the character's ring, here and on the map). */
    fun update(s: Sample, o: Output, risk: Double = 0.0) {
        val level = if (o.thetaHigh > 0) (o.index / o.thetaHigh).toFloat() else 0f
        showMood(Mood.shown(o.state, risk), level)
        chipSpeed.setChip("%.0f".format(s.speed * 3.6))
        chipAir.setChip("%.0f°".format(s.cabinTemp), if (s.recirc) "실내 · 내기" else "실내 · 외기")
        theme?.let { map.setMood(it, mood, level) }
        map.setRisk(risk)
        icon.ring = risk.toFloat()
        gauge.set(risk)
        lastRisk = risk
        updateRiskPill()
        map.update(s)
    }

    /** Replay scenario whose route the map draws (null = live mode). Call after [resetTrip]. */
    fun setRoute(name: String?, track: DoubleArray?) {
        markSource(name)
        map.setRoute(name, track)
    }

    /** Live navigation guidance for the map's turn card and arrival bar. */
    fun setNaviGuidance(g: ai.motioncare.app.NaviGuide.Guidance?) = map.setGuidance(g)

    /** Releases the map's WebView; call from the activity's onDestroy. */
    fun destroy() = map.destroy()

    private fun shakeWord(m: Mood) = when (m) { Mood.CALM -> "잔잔"; Mood.UNEASY -> "흔들림"; Mood.SICK -> "흔들림 많음" }

    private fun showMood(m: Mood, level: Float) {
        mood = m
        icon.mood = m
        icon.level = level
        theme?.let { moodLabel.text = labelFor(it, m) }
        moodLabel.setTextColor(moodInk(m))
        if (SystemClock.uptimeMillis() > actionUntil) {
            advice.setTextColor(Ui.MUTED)
            advice.text = when (m) {
                Mood.CALM -> "편안하게 가요"
                Mood.UNEASY -> "창밖 먼 곳 보기"
                Mood.SICK -> "바람 쐬고 쉬어 가요"
            }
        }
    }

    private fun moodInk(m: Mood) = when (m) { Mood.CALM -> Ui.RISK_LOW; Mood.UNEASY -> Ui.RISK_MID_INK; Mood.SICK -> Ui.RISK_HIGH }

    /** [reliefs]: 멀미 완화 presses; [peak]: the strongest face of the trip, in the same words the face uses. */
    fun updateSummary(minutes: Int, reliefs: Int, airs: Int, peak: Mood) {
        statTime.setStat("${minutes}분")
        statRelief.setStat("$reliefs")
        statAir.setStat("$airs")
        val (word, face) = when (peak) {
            Mood.SICK -> "많음" to Glyph.Kind.FACE_SICK
            Mood.UNEASY -> "보통" to Glyph.Kind.FACE_UNEASY
            Mood.CALM -> "잔잔" to Glyph.Kind.FACE_CALM
        }
        statPeak.setStat(word, moodInk(peak))
        statPeak.findViewWithTag<Glyph>("glyph").apply { kind = face; color = when (peak) { Mood.SICK -> Ui.RISK_HIGH; Mood.UNEASY -> Ui.RISK_MID; Mood.CALM -> Ui.RISK_LOW } }
    }

    fun note(msg: String) = dashboard.note(msg)
}
