package ai.motioncare.app.ui

import ai.motioncare.app.Settings
import ai.motioncare.app.theme.Mood
import ai.motioncare.app.theme.MoodIconView
import ai.motioncare.app.theme.MoodTheme
import ai.motioncare.app.theme.MoodThemes
import ai.motioncare.app.ui.Ui.card
import ai.motioncare.app.ui.Ui.dp
import ai.motioncare.app.ui.Ui.dpf
import ai.motioncare.app.ui.Ui.lp
import ai.motioncare.app.ui.Ui.pill
import ai.motioncare.app.ui.Ui.text
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch

/**
 * Full-screen settings over the home screen: icon theme (live preview of all three moods),
 * how the car helps (voice, climate) and the demo panel switch. Shown on first launch as a short
 * welcome with the theme choice. The family profiles and turn cue picker are parked in [ai.motioncare.app.later].
 */
class SettingsPanel(
    context: Context,
    private val settings: Settings,
    private val onThemeChanged: (MoodTheme) -> Unit,
    private val onChanged: () -> Unit,
    private val onClose: () -> Unit,
) : LinearLayout(context) {
    private val themeCards = mutableMapOf<String, LinearLayout>()
    private val welcome = context.text("", 19f, Ui.PRIMARY, bold = true)
    private val drivingNote = context.text("주행 중에는 테마 잠김", 18f, Ui.ALERT, bold = true)
    private val parkedOnly = mutableListOf<android.view.View>()
    private val themeRows = mutableListOf<LinearLayout>()
    private var oneColumn = false
    private var restricted = false

    init {
        orientation = VERTICAL
        setBackgroundColor(Ui.BG)
        isClickable = true   // swallow touches meant for the home screen underneath
        setPadding(context.dp(28), context.dp(18), context.dp(28), context.dp(12))

        val header = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(context.text("설정", 28f, bold = true), lp(0, WRAP_CONTENT, 1f))
        header.addView(context.pill("완료", filled = true) { onClose() })
        addView(header, lp(MATCH_PARENT, WRAP_CONTENT))

        val content = LinearLayout(context).apply { orientation = VERTICAL }
        content.addView(welcome, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(8) })
        drivingNote.visibility = GONE
        content.addView(drivingNote, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(8) })

        section(content, "아이콘 테마", "흔들림에 따라 표정이 바뀌어요")
        val grid = LinearLayout(context).apply { orientation = VERTICAL }
        MoodThemes.all.chunked(2).forEach { pair ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            pair.forEach { t -> row.addView(themeCard(t), lp(0, WRAP_CONTENT, 1f, context.dp(6))) }
            grid.addView(row, lp(MATCH_PARENT, WRAP_CONTENT))
            themeRows += row
        }
        content.addView(grid, lp(MATCH_PARENT, WRAP_CONTENT))
        parkedOnly += grid

        section(content, "차가 도와주는 방법", null)
        val toggles = context.card()
        toggles.addView(toggle("소리 안내", "질문과 위험 안내를 소리로", settings.voiceOn) { settings.voiceOn = it })
        toggles.addView(toggle("공조 자동 조절", "위험할 때 바깥 공기·22°C", settings.autoClimateOn) { settings.autoClimateOn = it })
        toggles.addView(toggle("시연 패널", "시나리오 재생 버튼 (시연용)", settings.demoPanelOn) { settings.demoPanelOn = it })
        content.addView(toggles, lp(MATCH_PARENT, WRAP_CONTENT, 0f, context.dp(6)))

        content.addView(context.text("운전 중에는 화면을 조작하지 마세요", 16f, Ui.MUTED),
            lp(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(context.dp(6), context.dp(16), context.dp(6), 0) })

        // credits: the team logo lives here and in the README, not on the launcher icon
        val credits = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(android.widget.ImageView(context).apply { setImageResource(ai.motioncare.app.R.drawable.crave_logo) },
                lp(context.dp(28), context.dp(28)).apply { marginEnd = context.dp(10) })
            addView(context.text("만든 팀 CRAVE · Pleos Connect 공모전", 16f, Ui.MUTED))
        }
        content.addView(credits, lp(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(context.dp(6), context.dp(14), context.dp(6), context.dp(24)) })

        addView(ScrollView(context).apply { addView(content) }, lp(MATCH_PARENT, 0, 1f).apply { topMargin = context.dp(8) })
        markSelected()
    }

    /**
     * Driving UX restrictions (no setup / no keyboard while moving): theme editing is
     * locked; the guidance toggles stay usable because they only reduce what the car does.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // the narrow pane beside the navigation: one theme card per row, so captions never wrap
        val one = w < context.dp(900)
        if (one == oneColumn) return
        oneColumn = one
        post {
            themeRows.forEach { row ->
                row.orientation = if (one) VERTICAL else HORIZONTAL
                for (i in 0 until row.childCount) row.getChildAt(i).layoutParams =
                    if (one) lp(MATCH_PARENT, WRAP_CONTENT, 0f, context.dp(6)) else lp(0, WRAP_CONTENT, 1f, context.dp(6))
            }
        }
    }

    fun setRestricted(on: Boolean) {
        restricted = on
        drivingNote.visibility = if (on) VISIBLE else GONE
        parkedOnly.forEach { setEnabledDeep(it, !on); it.alpha = if (on) 0.4f else 1f }
    }

    private fun setEnabledDeep(v: android.view.View, enabled: Boolean) {
        v.isEnabled = enabled
        if (v is android.view.ViewGroup) for (i in 0 until v.childCount) setEnabledDeep(v.getChildAt(i), enabled)
    }

    fun showWelcome(first: Boolean) {
        welcome.text = if (first) "환영해요! 아이콘 테마를 골라 주세요" else ""
        welcome.visibility = if (first) VISIBLE else GONE
    }

    private fun section(parent: LinearLayout, title: String, sub: String?) {
        parent.addView(context.text(title, 21f, bold = true), lp(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(context.dp(6), context.dp(18), 0, 0) })
        sub?.let { parent.addView(context.text(it, 16f, Ui.MUTED), lp(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(context.dp(6), context.dp(2), 0, context.dp(4)) }) }
    }

    private fun themeCard(t: MoodTheme) = context.card().apply {
        isClickable = true
        addView(context.text(t.title, 19f, bold = true))
        addView(context.text(t.subtitle, 16f, Ui.MUTED).apply { maxLines = 1 })
        val faces = LinearLayout(context).apply { orientation = HORIZONTAL }
        Mood.values().forEach { m ->
            val col = LinearLayout(context).apply { orientation = VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
            col.addView(MoodIconView(context).apply { theme = t; mood = m; level = when (m) { Mood.CALM -> 0.2f; Mood.UNEASY -> 0.75f; Mood.SICK -> 1.2f }; ring = when (m) { Mood.CALM -> 0.15f; Mood.UNEASY -> 0.4f; Mood.SICK -> 0.8f } },
                lp(MATCH_PARENT, context.dp(120)))
            col.addView(context.text(t.label(m), 16f, Ui.MUTED, bold = true).apply {
                gravity = Gravity.CENTER; maxLines = 1
                setAutoSizeTextTypeUniformWithConfiguration((11 * Ui.SCALE).toInt(), (16 * Ui.SCALE).toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
            }, lp(MATCH_PARENT, context.dp(26)))
            faces.addView(col, lp(0, WRAP_CONTENT, 1f))
        }
        addView(faces, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(8) })
        setOnClickListener {
            if (restricted) return@setOnClickListener
            settings.themeId = t.id
            markSelected()
            onThemeChanged(t)
        }
        themeCards[t.id] = this
    }

    private fun markSelected() {
        themeCards.forEach { (id, v) ->
            val on = id == settings.themeId
            v.background = Ui.rounded(if (on) Ui.PRIMARY_SOFT else Ui.CARD, context.dpf(20f), context.dp(if (on) 3 else 1), if (on) Ui.PRIMARY else Ui.LINE)
        }
    }

    private fun toggle(title: String, sub: String, value: Boolean, onSet: (Boolean) -> Unit) = LinearLayout(context).apply {
        orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        minimumHeight = context.dp(72)
        setPadding(0, 0, context.dp(4), 0)
        val texts = LinearLayout(context).apply {
            orientation = VERTICAL
            addView(context.text(title, 18f, bold = true))
            addView(context.text(sub, 16f, Ui.MUTED))
        }
        addView(texts, lp(0, WRAP_CONTENT, 1f))
        val sw = Switch(context).apply {
            isChecked = value
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            thumbTintList = android.content.res.ColorStateList(states, intArrayOf(Ui.PRIMARY, android.graphics.Color.WHITE))
            trackTintList = android.content.res.ColorStateList(states, intArrayOf(Ui.PRIMARY_SOFT, Ui.LINE))
            setOnCheckedChangeListener { _, on -> onSet(on); onChanged() }
        }
        addView(sw)
        // the whole row toggles, not just the small switch
        isClickable = true
        setOnClickListener { sw.toggle() }
    }
}
