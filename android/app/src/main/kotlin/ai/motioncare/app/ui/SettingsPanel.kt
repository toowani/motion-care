package ai.motioncare.app.ui

import ai.motioncare.app.ProfileStore
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
import ai.motioncare.core.Params
import ai.motioncare.core.Profile
import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch

/**
 * Full-screen settings over the home screen: icon theme (live preview of all three moods),
 * how the car helps (voice, light, climate), family profiles, and the demo panel switch.
 * Shown on first launch as a short welcome with the theme choice.
 */
class SettingsPanel(
    context: Context,
    private val settings: Settings,
    private val profiles: ProfileStore,
    private val params: Params,
    private val onThemeChanged: (MoodTheme) -> Unit,
    private val onChanged: () -> Unit,
    private val onClose: () -> Unit,
) : LinearLayout(context) {
    private val themeCards = mutableMapOf<String, LinearLayout>()
    private val profileList = LinearLayout(context).apply { orientation = VERTICAL }
    private val welcome = context.text("", 19f, Ui.PRIMARY, bold = true)
    private val drivingNote = context.text("주행 중에는 테마와 가족 프로필을 바꿀 수 없어요. 차를 세운 뒤 바꿔 주세요.", 18f, Ui.ALERT, bold = true)
    private val parkedOnly = mutableListOf<android.view.View>()
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

        section(content, "아이콘 테마", "멀미 지수에 따라 표정이 바뀌어요. 아이와 함께 골라 보세요.")
        val grid = LinearLayout(context).apply { orientation = VERTICAL }
        MoodThemes.all.chunked(2).forEach { pair ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            pair.forEach { t -> row.addView(themeCard(t), lp(0, WRAP_CONTENT, 1f, context.dp(6))) }
            grid.addView(row, lp(MATCH_PARENT, WRAP_CONTENT))
        }
        content.addView(grid, lp(MATCH_PARENT, WRAP_CONTENT))
        parkedOnly += grid

        section(content, "차가 도와주는 방법", null)
        val toggles = context.card()
        toggles.addView(toggle("소리 안내", "휴게소·가족 모드 질문과 멀미 위험 안내를 소리로 알려요", settings.voiceOn) { settings.voiceOn = it })
        toggles.addView(cueStylePicker())
        toggles.addView(toggle("무드등 예고", "회전 직전 실내 무드등을 깜빡여요 (어린아이용)", settings.lightCueOn) { settings.lightCueOn = it })
        toggles.addView(toggle("공조 자동 조절", "멀미 위험이 높으면 바깥 공기·22°C로 바꿔요", settings.autoClimateOn) { settings.autoClimateOn = it })
        toggles.addView(toggle("시연 패널", "그래프와 시나리오 재생 버튼을 보여요 (심사·시연용)", settings.demoPanelOn) { settings.demoPanelOn = it })
        content.addView(toggles, lp(MATCH_PARENT, WRAP_CONTENT, 0f, context.dp(6)))

        section(content, "가족 프로필", "아이 프로필은 이 차에만 저장되고 밖으로 나가지 않아요. 이름 대신 별명을 써 주세요.")
        val family = context.card()
        family.addView(profileList, lp(MATCH_PARENT, WRAP_CONTENT))
        val actions = LinearLayout(context).apply { orientation = HORIZONTAL }
        actions.addView(context.pill("+ 아이 추가") { askName("아이 별명", "") { name -> profiles.save(profiles.load() + Profile(name)); refreshProfiles() } },
            lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(10) })
        actions.addView(context.pill("학습 기록 초기화", color = Ui.MUTED) {
            AlertDialog.Builder(context).setTitle("학습한 기준선을 지울까요?")
                .setMessage("아이 별명은 남고, 멀미 기록만 지워져요.")
                .setPositiveButton("지우기") { _, _ -> profiles.save(profiles.load().map { it.copy(onsetDoses = emptyList()) }); refreshProfiles() }
                .setNegativeButton("취소", null).show()
        })
        family.addView(actions, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(12) })
        parkedOnly += family
        content.addView(family, lp(MATCH_PARENT, WRAP_CONTENT, 0f, context.dp(6)))

        content.addView(context.text("멀미 케어 · 차의 방향지시등·내비·공조 신호로 동승자 멀미를 미리 막아요. 운전 중에는 화면을 조작하지 마세요.", 14f, Ui.MUTED),
            lp(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(context.dp(6), context.dp(16), context.dp(6), context.dp(24)) })

        addView(ScrollView(context).apply { addView(content) }, lp(MATCH_PARENT, 0, 1f).apply { topMargin = context.dp(8) })
        refreshProfiles()
        markSelected()
    }

    /**
     * Driving UX restrictions (no setup / no keyboard while moving): theme and profile editing are
     * locked; the guidance toggles stay usable because they only reduce what the car does.
     */
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
        welcome.text = if (first) "환영해요! 먼저 아이콘 테마를 골라 주세요. 언제든 설정에서 바꿀 수 있어요." else ""
        welcome.visibility = if (first) VISIBLE else GONE
        refreshProfiles()
    }

    private fun section(parent: LinearLayout, title: String, sub: String?) {
        parent.addView(context.text(title, 21f, bold = true), lp(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(context.dp(6), context.dp(18), 0, 0) })
        sub?.let { parent.addView(context.text(it, 15f, Ui.MUTED), lp(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(context.dp(6), context.dp(2), 0, context.dp(4)) }) }
    }

    private fun themeCard(t: MoodTheme) = context.card().apply {
        isClickable = true
        addView(context.text(t.title, 19f, bold = true))
        addView(context.text(t.subtitle, 14f, Ui.MUTED))
        val faces = LinearLayout(context).apply { orientation = HORIZONTAL }
        Mood.values().forEach { m ->
            val col = LinearLayout(context).apply { orientation = VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
            col.addView(MoodIconView(context).apply { theme = t; mood = m; level = when (m) { Mood.CALM -> 0.2f; Mood.UNEASY -> 0.75f; Mood.SICK -> 1.2f } },
                lp(MATCH_PARENT, context.dp(120)))
            col.addView(context.text(t.label(m), 13f, Ui.MUTED).apply { gravity = Gravity.CENTER })
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

    /** Turn cue: short tone (default), the theme's sentence, or light only. Navigation turns are never spoken twice. */
    private fun cueStylePicker() = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(0, context.dp(6), 0, context.dp(10))
        addView(context.text("회전 예고 방식", 18f, bold = true))
        addView(context.text("말로 하는 예고는 금방 피곤해져요. 기본은 짧은 알림음이고, 내비가 회전을 안내할 때는 겹쳐 말하지 않아요.", 14f, Ui.MUTED))
        val row = LinearLayout(context).apply { orientation = HORIZONTAL }
        val options = listOf("chime" to "알림음", "voice" to "캐릭터 목소리", "silent" to "무드등만")
        val pills = mutableListOf<android.widget.TextView>()
        fun paint() = pills.forEachIndexed { i, v ->
            val on = options[i].first == settings.cueStyle
            v.setTextColor(if (on) android.graphics.Color.WHITE else Ui.PRIMARY)
            v.background = Ui.rounded(if (on) Ui.PRIMARY else Ui.CARD, context.dpf(28f), context.dp(2), Ui.PRIMARY)
        }
        options.forEach { (id, label) ->
            val p = context.pill(label) { settings.cueStyle = id; paint(); onChanged() }
            pills += p
            row.addView(p, lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(10) })
        }
        paint()
        addView(row, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(8) })
    }

    private fun toggle(title: String, sub: String, value: Boolean, onSet: (Boolean) -> Unit) = LinearLayout(context).apply {
        orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        minimumHeight = context.dp(72)
        setPadding(0, 0, context.dp(4), 0)
        val texts = LinearLayout(context).apply {
            orientation = VERTICAL
            addView(context.text(title, 18f, bold = true))
            addView(context.text(sub, 14f, Ui.MUTED))
        }
        addView(texts, lp(0, WRAP_CONTENT, 1f))
        val sw = Switch(context).apply {
            isChecked = value
            setOnCheckedChangeListener { _, on -> onSet(on); onChanged() }
        }
        addView(sw)
        // the whole row toggles, not just the small switch
        isClickable = true
        setOnClickListener { sw.toggle() }
    }

    private fun refreshProfiles() {
        profileList.removeAllViews()
        val list = profiles.load()
        if (list.isEmpty()) profileList.addView(context.text("아직 등록한 아이가 없어요.", 16f, Ui.MUTED))
        list.forEachIndexed { i, p ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; minimumHeight = context.dp(68) }
            val info = LinearLayout(context).apply {
                orientation = VERTICAL
                addView(context.text(p.name, 19f, bold = true))
                val sensitivity = when { p.onsetDoses.isEmpty() -> "아직 기록 없음 · 평균 기준으로 시작"; p.threshold(params) < 26 -> "멀미를 쉽게 해요"; p.threshold(params) < 34 -> "보통이에요"; else -> "멀미를 잘 안 해요" }
                addView(context.text("멀미 기록 ${p.onsetDoses.size}번 · $sensitivity · 기준선 %.1f".format(p.threshold(params)), 14f, Ui.MUTED))
            }
            row.addView(info, lp(0, WRAP_CONTENT, 1f))
            row.addView(context.pill("이름 바꾸기", color = Ui.PRIMARY) {
                askName("새 별명", p.name) { name -> profiles.save(list.toMutableList().also { it[i] = p.copy(name = name) }); refreshProfiles(); onChanged() }
            }, lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(8) })
            row.addView(context.pill("삭제", color = Ui.ALERT) {
                AlertDialog.Builder(context).setTitle("${p.name} 프로필을 지울까요?")
                    .setPositiveButton("지우기") { _, _ -> profiles.save(list.filterIndexed { j, _ -> j != i }); refreshProfiles(); onChanged() }
                    .setNegativeButton("취소", null).show()
            })
            profileList.addView(row, lp(MATCH_PARENT, WRAP_CONTENT))
        }
    }

    private fun askName(title: String, initial: String, onOk: (String) -> Unit) {
        val input = EditText(context).apply { setText(initial); inputType = InputType.TYPE_CLASS_TEXT; textSize = 20f; setSingleLine() }
        AlertDialog.Builder(context).setTitle(title).setView(input)
            .setPositiveButton("확인") { _, _ -> input.text.toString().trim().take(8).takeIf { it.isNotEmpty() && !it.contains('=') }?.let(onOk) }
            .setNegativeButton("취소", null).show()
    }
}
