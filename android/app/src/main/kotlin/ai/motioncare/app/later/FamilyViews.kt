package ai.motioncare.app.later

import ai.motioncare.app.ProfileStore
import ai.motioncare.app.Settings
import ai.motioncare.app.ui.RiskGauge
import ai.motioncare.app.ui.Ui
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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout

/** Parked: "아이별 멀미 가능성" card for the home screen, wrapping [FamilyBars]. */
fun familyCard(context: Context, bars: FamilyBars) = context.card().apply {
    setPadding(context.dp(20), context.dp(12), context.dp(20), context.dp(12))
    addView(context.text("아이별 멀미 가능성", 16f, Ui.MUTED, bold = true))
    addView(bars, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(4) })
}

/** Parked: the top-bar "가족 모드" toggle; [paint] shows on/off by fill and icon. */
fun familyChip(context: Context, onToggle: () -> Unit) = context.pill("가족 모드") { onToggle() }.apply {
    compoundDrawablePadding = context.dp(8)
    maxLines = 1
}

fun paintFamilyChip(chip: android.widget.TextView, on: Boolean) {
    val c = chip.context
    chip.setTextColor(if (on) Color.WHITE else Ui.PRIMARY)
    chip.background = Ui.rounded(if (on) Ui.PRIMARY else Ui.CARD, c.dpf(28f), c.dp(2), Ui.PRIMARY)
    chip.setCompoundDrawablesRelativeWithIntrinsicBounds(FamilyIcon(c.dp(26), if (on) Color.WHITE else Ui.PRIMARY, on), null, null, null)
}

/** One row per child: initial in a coloured circle, name, thick bar, level word and big %. */
class FamilyBars(context: Context) : View(context) {
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
            word.color = RiskGauge.ink(p); pct.color = word.color
            c.drawText(RiskGauge.label(p), x1 + wordW, cy + word.textSize / 3, word)
            c.drawText("${Ui.pct(p)}%", width.toFloat(), cy + pct.textSize / 3, pct)
        }
    }
}

/** Two heads (adult and child): the family button's icon, filled while family mode is on. */
class FamilyIcon(private val size: Int, color: Int, private val filled: Boolean) : android.graphics.drawable.Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = if (filled) Paint.Style.FILL else Paint.Style.STROKE
        strokeWidth = size * 0.09f
    }
    override fun getIntrinsicWidth() = size
    override fun getIntrinsicHeight() = size
    override fun draw(c: Canvas) {
        val b = bounds
        val s = b.width().toFloat()
        val x = b.left.toFloat(); val y = b.top.toFloat()
        c.drawCircle(x + s * 0.34f, y + s * 0.26f, s * 0.15f, paint)
        c.drawRoundRect(x + s * 0.12f, y + s * 0.5f, x + s * 0.56f, y + s * 0.95f, s * 0.2f, s * 0.2f, paint)
        c.drawCircle(x + s * 0.74f, y + s * 0.46f, s * 0.11f, paint)
        c.drawRoundRect(x + s * 0.6f, y + s * 0.65f, x + s * 0.9f, y + s * 0.95f, s * 0.14f, s * 0.14f, paint)
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(cf: android.graphics.ColorFilter?) { paint.colorFilter = cf }
    @Deprecated("Drawable opacity", ReplaceWith("PixelFormat.TRANSLUCENT"))
    override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
}

/**
 * Parked: the settings "가족 프로필" card (children's nicknames, learnt sensitivity, rename,
 * delete, add, reset learning). Profiles stay on the device.
 */
class FamilyProfilesCard(
    context: Context,
    private val profiles: ProfileStore,
    private val params: Params,
    private val onChanged: () -> Unit,
) : LinearLayout(context) {
    private val profileList = LinearLayout(context).apply { orientation = VERTICAL }

    init {
        orientation = VERTICAL
        background = Ui.rounded(Ui.CARD, context.dpf(20f))
        setPadding(context.dp(20), context.dp(18), context.dp(20), context.dp(18))
        addView(profileList, lp(MATCH_PARENT, WRAP_CONTENT))
        val actions = LinearLayout(context).apply { orientation = HORIZONTAL }
        actions.addView(context.pill("+ 아이 추가") { askName("아이 별명", "") { name -> profiles.save(profiles.load() + Profile(name)); refresh() } },
            lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(10) })
        actions.addView(context.pill("학습 기록 초기화", color = Ui.MUTED) {
            AlertDialog.Builder(context).setTitle("학습한 기준선을 지울까요?")
                .setMessage("아이 별명은 남고, 멀미 기록만 지워져요.")
                .setPositiveButton("지우기") { _, _ -> profiles.save(profiles.load().map { it.copy(onsetDoses = emptyList()) }); refresh() }
                .setNegativeButton("취소", null).show()
        })
        addView(actions, lp(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(12) })
        refresh()
    }

    fun refresh() {
        profileList.removeAllViews()
        val list = profiles.load()
        if (list.isEmpty()) profileList.addView(context.text("아직 등록한 아이가 없어요.", 16f, Ui.MUTED))
        list.forEachIndexed { i, p ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; minimumHeight = context.dp(68) }
            val info = LinearLayout(context).apply {
                orientation = VERTICAL
                addView(context.text(p.name, 19f, bold = true))
                val sensitivity = when { p.onsetDoses.isEmpty() -> "아직 기록 없음"; p.threshold(params) < 26 -> "멀미를 쉽게 해요"; p.threshold(params) < 34 -> "보통이에요"; else -> "멀미를 잘 안 해요" }
                addView(context.text("멀미 기록 ${p.onsetDoses.size}번 · $sensitivity", 16f, Ui.MUTED))
            }
            row.addView(info, lp(0, WRAP_CONTENT, 1f))
            row.addView(context.pill("이름 바꾸기", color = Ui.PRIMARY) {
                askName("새 별명", p.name) { name -> profiles.save(list.toMutableList().also { it[i] = p.copy(name = name) }); refresh(); onChanged() }
            }, lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = context.dp(8) })
            row.addView(context.pill("삭제", color = Ui.ALERT) {
                AlertDialog.Builder(context).setTitle("${p.name} 프로필을 지울까요?")
                    .setPositiveButton("지우기") { _, _ -> profiles.save(list.filterIndexed { j, _ -> j != i }); refresh(); onChanged() }
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

/** Parked: the settings "회전 예고 방식" picker (tone, the theme's voice, or light only). */
fun cueStylePicker(context: Context, settings: Settings, onChanged: () -> Unit) = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(0, context.dp(6), 0, context.dp(10))
    addView(context.text("회전 예고 방식", 18f, bold = true))
    addView(context.text("기본은 짧은 알림음 · 내비 안내와 겹치지 않아요", 16f, Ui.MUTED))
    val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    val options = listOf("chime" to "알림음", "voice" to "캐릭터 목소리", "silent" to "무드등만")
    val pills = mutableListOf<android.widget.TextView>()
    fun paint() = pills.forEachIndexed { i, v ->
        val on = options[i].first == settings.cueStyle
        v.setTextColor(if (on) Color.WHITE else Ui.PRIMARY)
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
