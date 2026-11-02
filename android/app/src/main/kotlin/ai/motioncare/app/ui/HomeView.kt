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
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * Home screen, built for a glance (people in a car do not read): big numbers, pictograms and
 * colour instead of sentences. Left: themed mood icon, one short line, three facts. Right tabs:
 * 주행 (risk gauge, trip tiles, family bars, habit tiles), 분석 (graphs), 지도 (map).
 * Bottom: demo controls (scenario replay / live signals), hidden when the demo panel is off.
 */
private const val COMPACT_BELOW_DP = 900
private const val TAB_DRIVE = 0
private const val TAB_ANALYSIS = 1
private const val TAB_MAP = 2

class HomeView(context: Context, private val callbacks: Callbacks) : LinearLayout(context) {
    interface Callbacks {
        fun onFamilyToggle()
        fun onReportSick()
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
    private val chipTurn = chip("다음 회전", "—")
    private val themeName = context.text("", 15f, Ui.MUTED)
    private val familyChip: TextView
    private lateinit var rightColumn: LinearLayout
    private lateinit var leftCard: LinearLayout
    private var demoOn = true
    private var familyNames: List<String> = emptyList()
    /** One app pane next to the navigation (Pleos lets an app take one of two panes): character only. */
    private var compact = false
    val isCompact get() = compact
    private val familyBars = FamilyBars(context)
    private val familyCard: LinearLayout
    private val graphCard: LinearLayout
    private val gauge = RiskGauge(context)
    private val statTime = statTile(Glyph.Kind.CLOCK, "주행")
    private val statCues = statTile(Glyph.Kind.TURN, "회전 예고")
    private val statAir = statTile(Glyph.Kind.WIND, "공기 바꿈")
    private val statPeak = statTile(Glyph.Kind.FACE_CALM, "최고 흔들림")
    private val summaryCard: LinearLayout
    private val tabDrive: TextView
    private val tabAnalysis: TextView
    private val tabMap: TextView
    private lateinit var habits: LinearLayout
    private val map = MapPanel(context)
    private val mapCard: LinearLayout
    private var familyOn = false
    private var analysisShown = false
    private var tab = TAB_DRIVE
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
            // fixed height: auto-size never grows back with WRAP_CONTENT (stayed small after compact mode)
            addView(moodLabel, lp(MATCH_PARENT, context.dp(48)))
            addView(advice, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(6) })
            addView(promptBox, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(10) })
            val chips = LinearLayout(context).apply { orientation = HORIZONTAL }
            listOf(chipSpeed, chipAir, chipTurn).forEach { chips.addView(it, lp(0, WRAP_CONTENT, 1f).apply { setMargins(context.dp(4), 0, context.dp(4), 0) }) }
            addView(chips, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(14) })
        }
        body.addView(left, lp(0, MATCH_PARENT, 0.4f).apply { setMargins(0, context.dp(14), context.dp(14), context.dp(10)) })

        val right = LinearLayout(context).apply { orientation = VERTICAL }
        rightColumn = right
        leftCard = left
        // two tabs: 주행 (family + trip summary, for passengers) and 분석 (index graph and event log)
        val tabs = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        tabDrive = context.pill("주행", filled = true) { showTab(TAB_DRIVE) }
        tabAnalysis = context.pill("분석") { showTab(TAB_ANALYSIS) }
        tabMap = context.pill("지도") { showTab(TAB_MAP) }
        tabs.addView(tabDrive, lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(10) })
        tabs.addView(tabAnalysis, lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(10) })
        tabs.addView(tabMap, lp(WRAP_CONTENT, WRAP_CONTENT))
        right.addView(tabs, lp(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = context.dp(10) })
        familyCard = context.card().apply {
            setPadding(context.dp(20), context.dp(12), context.dp(20), context.dp(12))
            addView(context.text("아이별 멀미 가능성", 16f, Ui.MUTED, bold = true))
            addView(familyBars, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(4) })
        }
        graphCard = context.card().apply {
            setPadding(context.dp(8), context.dp(10), context.dp(8), context.dp(8))
            addView(graphToggle(), lp(WRAP_CONTENT, WRAP_CONTENT).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = context.dp(6) })
            addView(dashboard, lp(MATCH_PARENT, 0, 1f))
        }
        right.addView(graphCard, lp(MATCH_PARENT, 0, 1f))
        // 지도 tab: our own map with the character badge on top, like a speed-camera icon (prototype)
        mapCard = context.card().apply {
            setPadding(0, 0, 0, 0)
            clipToOutline = true
            addView(map, lp(MATCH_PARENT, MATCH_PARENT))
        }
        right.addView(mapCard, lp(MATCH_PARENT, 0, 1f))
        // 주행 tab: gauge + trip tiles on top, family bars (family mode), habit pictograms below
        summaryCard = LinearLayout(context).apply { orientation = VERTICAL }
        val topRow = LinearLayout(context).apply { orientation = HORIZONTAL }
        topRow.addView(context.card().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(context.text("멀미 가능성", 16f, Ui.MUTED, bold = true), lp(MATCH_PARENT, WRAP_CONTENT))
            addView(gauge, lp(MATCH_PARENT, 0, 1f).apply { topMargin = context.dp(8) })
        }, lp(0, MATCH_PARENT, 0.42f).apply { marginEnd = context.dp(12) })
        topRow.addView(context.card().apply {
            addView(context.text("이번 주행", 16f, Ui.MUTED, bold = true))
            listOf(statTime to statCues, statAir to statPeak).forEach { (a, b) ->
                val row = LinearLayout(context).apply { orientation = HORIZONTAL }
                row.addView(a, lp(0, MATCH_PARENT, 1f).apply { marginEnd = context.dp(8) })
                row.addView(b, lp(0, MATCH_PARENT, 1f))
                addView(row, lp(MATCH_PARENT, 0, 1f).apply { topMargin = context.dp(8) })
            }
        }, lp(0, MATCH_PARENT, 0.58f))
        summaryCard.addView(topRow, lp(MATCH_PARENT, 0, 1f))
        summaryCard.addView(familyCard, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(12) })
        // with more than two children the family bars take the habits. place, or the cards overflow the tab
        habits = habitsCard()
        summaryCard.addView(habits, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(12) })
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
        showTab(TAB_DRIVE)
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
        addView(Glyph(context, kind, Ui.PRIMARY).apply { tag = "glyph" }, lp(context.dp(48), context.dp(48)))
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
        chipAir.setChip("—", "실내")
        gauge.set(0.0)
        familyBars.set(familyBars.names().map { it to 0.0 })
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
        promptBox.visibility = VISIBLE
        layoutLeft()
    }

    fun hidePrompt() { promptBox.visibility = GONE; layoutLeft() }

    /**
     * The 지도 tab is a full-width navigation screen (the character rides on the map), so the big
     * character card steps aside, except while it holds a question.
     */
    private fun layoutLeft() {
        if (!::leftCard.isInitialized) return
        leftCard.gone(!compact && tab == TAB_MAP && promptBox.visibility != VISIBLE)
    }

    /** Demo panel = scenario replay buttons for judges; the graph lives in the 분석 tab either way. */
    fun setDemoPanel(on: Boolean) {
        demoOn = on
        demoBar.gone(!on || compact)
    }

    fun showTab(which: Int) {
        tab = which
        analysisShown = which != TAB_DRIVE
        graphCard.gone(which != TAB_ANALYSIS)
        mapCard.gone(which != TAB_MAP)
        summaryCard.gone(which != TAB_DRIVE)
        familyCard.gone(!familyOn)
        if (::habits.isInitialized) habits.gone(familyOn && familyNames.size > 2)
        layoutLeft()
        listOf(tabDrive to (which == TAB_DRIVE), tabAnalysis to (which == TAB_ANALYSIS), tabMap to (which == TAB_MAP)).forEach { (v, on) ->
            v.setTextColor(if (on) Color.WHITE else Ui.PRIMARY)
            v.background = Ui.rounded(if (on) Ui.PRIMARY else Ui.CARD, context.dpf(28f), context.dp(2), Ui.PRIMARY)
        }
    }

    fun setFamily(on: Boolean, names: List<String>) {
        familyNames = names
        familyChip.text = familyLabel(on)
        familyChip.setTextColor(if (on) Color.WHITE else Ui.PRIMARY)
        familyChip.background = Ui.rounded(if (on) Ui.PRIMARY else Ui.CARD, context.dpf(28f), context.dp(2), Ui.PRIMARY)
        familyOn = on
        familyCard.gone(!on)
        if (::habits.isInitialized) habits.gone(on && names.size > 2)
        // only reset the bars when the family itself changed, not on every settings change mid-trip
        val key = "$on:" + names.joinToString(",")
        if (key != familyKey) { familyKey = key; familyBars.set(names.map { it to 0.0 }) }
    }

    private fun familyLabel(on: Boolean) = when {
        compact -> if (on) "가족 켬" else "가족 끔"
        on -> "가족 모드 · " + familyNames.joinToString(", ")
        else -> "가족 모드 끔"
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val c = w < context.dp(COMPACT_BELOW_DP)
        if (c != compact) { compact = c; post { applyCompact() } }
    }

    /**
     * Narrow pane beside the navigation: only the big character, its level word and what the car
     * just did. The family bars, graph and demo buttons come back when the app is widened.
     */
    private fun applyCompact() {
        rightColumn.gone(compact)
        demoBar.gone(compact || !demoOn)
        (leftCard.layoutParams as LayoutParams).apply { weight = if (compact) 1f else 0.4f; marginEnd = if (compact) 0 else context.dp(14) }
        leftCard.requestLayout()
        layoutLeft()
        familyChip.text = familyLabel(familyOn)
        callbacks.onCompactChanged(compact)
    }

    fun setFamilyProbabilities(values: List<Pair<String, Double>>) = familyBars.set(values)

    /** Shows what the car just did for a few seconds instead of the generic advice. */
    fun showAction(msg: String) {
        actionText = msg
        actionUntil = SystemClock.uptimeMillis() + 8000
        advice.text = msg
        advice.setTextColor(Ui.PRIMARY)
    }

    /** [risk] = sickness probability of the passenger who matters (shown on the map). */
    fun update(s: Sample, o: Output, nextTurnM: Double?, risk: Double = 0.0) {
        val level = if (o.thetaHigh > 0) (o.index / o.thetaHigh).toFloat() else 0f
        showMood(Mood.of(o.state), level)
        chipSpeed.setChip("%.0f".format(s.speed * 3.6))
        chipAir.setChip("%.0f°".format(s.cabinTemp), if (s.recirc) "실내 · 내기" else "실내 · 외기")
        val turnText = nextTurnM?.takeIf { it >= 0 }?.let { if (it >= 1000) "%.1f km".format(it / 1000) else "%.0f m".format(it) }
        chipTurn.setChip(turnText ?: "—")
        theme?.let { map.setMood(it, mood, level, labelFor(it, mood)) }
        map.setRisk(risk)
        gauge.set(risk)
        map.update(s)
    }

    /** Replay scenario whose route the map draws (null = live mode). Call after [resetTrip]. */
    fun setRoute(name: String?, track: DoubleArray?) = map.setRoute(name, track)

    /** Live navigation guidance for the map's turn card and arrival bar. */
    fun setNaviGuidance(g: ai.motioncare.app.NaviGuide.Guidance?) = map.setGuidance(g)

    /** A turn cue fired: the character on the map says it. */
    fun showCue(line: String) = map.showCue(line)

    /** Releases the map's WebView; call from the activity's onDestroy. */
    fun destroy() = map.destroy()

    private fun showMood(m: Mood, level: Float) {
        mood = m
        icon.mood = m
        icon.level = level
        theme?.let { moodLabel.text = labelFor(it, m) }
        moodLabel.setTextColor(when (m) { Mood.CALM -> Ui.PRIMARY; Mood.UNEASY -> Color.parseColor("#B45309"); Mood.SICK -> Ui.ALERT })
        if (SystemClock.uptimeMillis() > actionUntil) {
            advice.setTextColor(Ui.MUTED)
            advice.text = when (m) {
                Mood.CALM -> "편안하게 가요"
                Mood.UNEASY -> "창밖 먼 곳 보기"
                Mood.SICK -> "바람 쐬고 쉬어 가요"
            }
        }
    }

    fun updateSummary(minutes: Int, cues: Int, airs: Int, peak: Float) {
        statTime.setStat("${minutes}분")
        statCues.setStat("$cues")
        statAir.setStat("$airs")
        val (word, color, face) = when {
            peak >= 1f -> Triple("위험", Ui.ALERT, Glyph.Kind.FACE_SICK)
            peak >= 0.6f -> Triple("주의", RiskGauge.AMBER, Glyph.Kind.FACE_UNEASY)
            else -> Triple("편안", Ui.PRIMARY, Glyph.Kind.FACE_CALM)
        }
        statPeak.setStat(word, color)
        statPeak.findViewWithTag<Glyph>("glyph").apply { kind = face; this.color = color }
    }

    fun note(msg: String) = dashboard.note(msg)

    /** One row per child: initial in a coloured circle, name, thick bar, level word and big %. */
    private class FamilyBars(context: Context) : View(context) {
        /** Same order and colours as the child lines in the 분석 graph. */
        private val childColors = intArrayOf(Color.parseColor("#453ECE"), Color.parseColor("#DB2777"), Color.parseColor("#0284C7"), Color.parseColor("#65A30D"))
        private var rows: List<Pair<String, Double>> = emptyList()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val initial = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = context.dpf(20f); color = Color.WHITE; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        private val name = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = context.dpf(18f); color = Ui.INK; isFakeBoldText = true }
        private val pct = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = context.dpf(28f); textAlign = Paint.Align.RIGHT; isFakeBoldText = true }
        private val word = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = context.dpf(17f); textAlign = Paint.Align.RIGHT; isFakeBoldText = true }
        private val rowH = context.dpf(50f)
        private val r = context.dpf(12f)

        fun set(values: List<Pair<String, Double>>) {
            val resize = values.size != rows.size
            rows = values
            if (resize) requestLayout()
            invalidate()
        }
        fun names() = rows.map { it.first }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (rowH * rows.size.coerceAtLeast(1)).toInt())

        override fun onDraw(c: Canvas) {
            if (rows.isEmpty()) return
            val circle = context.dpf(18f)
            val nameX = circle * 2 + context.dpf(10f)
            val nameW = context.dpf(64f)
            val pctW = context.dpf(76f)
            val wordW = context.dpf(44f)
            rows.forEachIndexed { i, (n, p) ->
                val cy = rowH * i + rowH / 2
                paint.color = childColors[i % childColors.size]
                c.drawCircle(circle, cy, circle, paint)
                c.drawText(n.take(1), circle, cy + initial.textSize / 3, initial)
                c.drawText(n, nameX, cy + name.textSize / 3, name)
                val x0 = nameX + nameW; val x1 = width - pctW - wordW
                paint.color = Ui.LINE
                c.drawRoundRect(x0, cy - r, x1, cy + r, r, r, paint)
                paint.color = RiskGauge.color(p)
                c.drawRoundRect(x0, cy - r, x0 + (x1 - x0) * p.toFloat().coerceIn(0.04f, 1f), cy + r, r, r, paint)
                word.color = paint.color; pct.color = paint.color
                c.drawText(RiskGauge.label(p), x1 + wordW, cy + word.textSize / 3, word)
                c.drawText("${(p * 100).roundToInt()}%", width.toFloat(), cy + pct.textSize / 3, pct)
            }
        }
    }
}
