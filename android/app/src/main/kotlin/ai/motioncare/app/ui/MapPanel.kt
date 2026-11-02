package ai.motioncare.app.ui

import ai.motioncare.app.NaviGuide
import ai.motioncare.app.NavRoute
import ai.motioncare.app.theme.Mood
import ai.motioncare.app.theme.MoodIconView
import ai.motioncare.app.theme.MoodTheme
import ai.motioncare.app.ui.Ui.dp
import ai.motioncare.app.ui.Ui.dpf
import ai.motioncare.app.ui.Ui.gone
import ai.motioncare.app.ui.Ui.text
import ai.motioncare.core.Sample
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "MotionCare"
private const val CUE_SHOW_MS = 4000L

/**
 * Our own navigation-style map. Apps cannot draw over the Pleos navigation, so this screen looks
 * like it: a heading-up 3D map (MapLibre GL + OpenFreeMap vector tiles, Leaflet raster as the
 * fallback when WebGL or MapLibre is missing) with the route, the car snapped onto the road, a
 * turn-by-turn card, the arrival bar and our character docked as an overlay widget. The map is a
 * WebView; everything on top is native so it stays legible and animated while tiles load.
 */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class MapPanel(context: Context) : FrameLayout(context) {
    private val web = WebView(context)
    private val fallback = context.text("지도를 불러오지 못했어요.\n네트워크 연결을 확인해 주세요.", 18f, Ui.MUTED)

    // turn-by-turn card (top-left)
    private val maneuver = ManeuverView(context)
    private val tbtDist = context.text("", 34f, Color.WHITE, bold = true)
    private val tbtRoad = context.text("", 17f, Color.parseColor("#C9CED8"))
    private val tbtCard = LinearLayout(context)

    // character widget (top-right) and its speech bubble
    private val icon = MoodIconView(context)
    private val ring = FrameLayout(context)
    private val moodLabel = context.text("편안", 22f, Ui.INK, bold = true)
    private val riskLabel = context.text("멀미 가능성", 16f, Color.WHITE, bold = true)
    private val riskValue = context.text("0%", 28f, Color.WHITE, bold = true)
    private val riskChip = LinearLayout(context)
    // turn cue: a big arrow and the distance under the character, no sentence
    private val cueArrow = ManeuverView(context)
    private val cueDist = context.text("", 30f, Color.WHITE, bold = true)
    private val bubble = LinearLayout(context)
    private var cueAt = -1.0

    // bottom: speed, arrival bar, 2D/3D, attribution
    private val speedNum = context.text("0", 28f, Ui.INK, bold = true)
    private val etaTime = context.text("", 24f, Color.WHITE, bold = true)
    private val etaRest = context.text("", 20f, Color.parseColor("#D5D9E0"), bold = true)
    private val etaDest = context.text("", 15f, Color.parseColor("#9AA1AD"))
    private val etaBar = LinearLayout(context)
    private val viewBtn = context.text("2D", 17f, Ui.INK, bold = true)
    private val attribution = context.text("© OpenStreetMap contributors © OpenFreeMap", 9f, Ui.MUTED)

    private val main = Handler(Looper.getMainLooper())
    private val hideBubble = Runnable { cueAt = -1.0; bubble.animate().alpha(0f).setDuration(250).withEndAction { bubble.gone(true) }.start() }
    private var destroyed = false
    private var ready = false
    private val queue = ArrayList<String>()
    private var threeD = true

    /** Scenario name while a replay plays; null in live mode. */
    private var replayName: String? = null
    private var route: NavRoute? = null
    private var track: DoubleArray? = null
    private var trackScale = 1.0
    private var lastS: Double? = null
    private var live: NaviGuide.Guidance? = null

    /** The page reports which engine came up; the Kotlin side only logs it and shows the error view. */
    private inner class Bridge {
        @JavascriptInterface fun engine(name: String) = main.post {
            Log.i(TAG, "map engine: $name")
            if (destroyed) return@post
            if (name == "none") showFallback()
            if (name == "leaflet") attribution.text = "© OpenStreetMap contributors"
        }
        @JavascriptInterface fun log(msg: String) { Log.i(TAG, "map: $msg") }
    }

    init {
        setBackgroundColor(Color.parseColor("#F3F4F7"))
        web.setBackgroundColor(Color.parseColor("#F3F4F7"))
        if (ai.motioncare.app.BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(), "MC")
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                if (destroyed) return
                ready = true
                queue.forEach { view.evaluateJavascript(it, null) }
                queue.clear()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showFallback()
            }
        }
        addView(web, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        fallback.gravity = Gravity.CENTER
        fallback.gone(true)
        addView(fallback, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        buildTbtCard()
        buildCharacter()
        buildBottom()

        web.loadDataWithBaseURL("https://motioncare.local/", HTML, "text/html", "utf-8", null)
    }

    private fun buildTbtCard() = tbtCard.apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = Ui.rounded(Color.parseColor("#111317"), context.dpf(10f))
        elevation = context.dpf(6f)
        setPadding(context.dp(14), context.dp(12), context.dp(20), context.dp(12))
        addView(maneuver, LinearLayout.LayoutParams(context.dp(76), context.dp(76)))
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            tbtDist.maxLines = 1
            // one line, never cut: a road name that does not fit is left out (see showTbt)
            tbtRoad.maxLines = 1
            addView(tbtDist)
            addView(tbtRoad)
        }
        addView(col, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = context.dp(12) })
        gone(true)
        this@MapPanel.addView(this, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START).apply {
            setMargins(context.dp(14), context.dp(14), 0, 0)
        })
    }

    private fun buildCharacter() {
        ring.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.WHITE); setStroke(context.dp(5), Ui.PRIMARY) }
        ring.addView(icon, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            val m = context.dp(9); setMargins(m, m, m, m)
        })
        moodLabel.gravity = Gravity.CENTER
        moodLabel.maxLines = 1
        moodLabel.ellipsize = TextUtils.TruncateAt.END
        riskChip.apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = Ui.rounded(Ui.riskColor(0.0), context.dpf(14f))
            setPadding(context.dp(6), context.dp(4), context.dp(6), context.dp(4))
            riskLabel.gravity = Gravity.CENTER
            riskLabel.maxLines = 1
            riskValue.gravity = Gravity.CENTER
            riskValue.includeFontPadding = false
            addView(riskLabel)
            addView(riskValue)
        }
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = Ui.rounded(Color.WHITE, context.dpf(20f))
            elevation = context.dpf(6f)
            setPadding(context.dp(10), context.dp(10), context.dp(10), context.dp(10))
            addView(ring, LinearLayout.LayoutParams(context.dp(CHAR_RING), context.dp(CHAR_RING)))
            addView(moodLabel, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(6) })
            // navigation mode shows only the chance of sickness; the face word (the shake now) read as a contradiction next to it
            moodLabel.gone(true)
            addView(riskChip, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(6) })
        }
        // the cue sits under the character, at the side, so it never covers the road ahead
        bubble.apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Ui.rounded(Color.parseColor("#111317"), context.dpf(16f), context.dp(3), RiskGauge.AMBER)
            elevation = context.dpf(6f)
            setPadding(context.dp(8), context.dp(6), context.dp(14), context.dp(6))
            addView(cueArrow, LinearLayout.LayoutParams(context.dp(56), context.dp(56)))
            cueDist.maxLines = 1
            addView(cueDist, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = context.dp(4) })
            gone(true)
        }
        val side = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            addView(card, LinearLayout.LayoutParams(context.dp(CHAR_RING + 20), LayoutParams.WRAP_CONTENT))
            addView(bubble, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(10) })
        }
        addView(side, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
            setMargins(0, context.dp(14), context.dp(14), 0)
        })
    }

    private fun buildBottom() {
        val speed = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.WHITE); setStroke(context.dp(2), Ui.LINE) }
            elevation = context.dpf(4f)
            speedNum.gravity = Gravity.CENTER
            addView(speedNum)
            addView(context.text("km/h", 11f, Ui.MUTED, bold = true).apply { gravity = Gravity.CENTER })
        }
        addView(speed, LayoutParams(context.dp(84), context.dp(84), Gravity.BOTTOM or Gravity.START).apply {
            setMargins(context.dp(14), 0, 0, context.dp(16))
        })

        etaBar.apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#111317"))
            elevation = context.dpf(6f)
            setPadding(context.dp(20), 0, context.dp(22), 0)
            listOf(etaTime, etaRest, etaDest).forEach { it.maxLines = 1 }
            etaDest.ellipsize = TextUtils.TruncateAt.END
            etaDest.maxWidth = context.dp(180)
            addView(etaTime)
            addView(View(context).apply { setBackgroundColor(Color.parseColor("#3A3F48")) },
                LinearLayout.LayoutParams(context.dp(2), context.dp(26)).apply { setMargins(context.dp(14), 0, context.dp(14), 0) })
            addView(etaRest)
            addView(etaDest, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = context.dp(14) })
            gone(true)
        }
        addView(etaBar, LayoutParams(LayoutParams.WRAP_CONTENT, context.dp(62), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            setMargins(context.dp(110), 0, context.dp(80), context.dp(16))
        })

        viewBtn.apply {
            gravity = Gravity.CENTER
            background = Ui.rounded(Color.WHITE, context.dpf(4f), context.dp(1), Ui.LINE)
            elevation = context.dpf(4f)
            isClickable = true
            setOnClickListener { threeD = !threeD; text = if (threeD) "2D" else "3D"; js("set3d($threeD)") }
        }
        addView(viewBtn, LayoutParams(context.dp(58), context.dp(58), Gravity.BOTTOM or Gravity.END).apply {
            setMargins(0, 0, context.dp(14), context.dp(30))
        })

        attribution.setBackgroundColor(Color.argb(170, 255, 255, 255))
        attribution.setPadding(context.dp(4), context.dp(1), context.dp(4), context.dp(1))
        addView(attribution, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.END).apply {
            setMargins(0, 0, context.dp(4), context.dp(4))
        })
    }

    /** Replay scenario (loads assets/NAME.route.json and its per-second track) or live mode (null). */
    fun setRoute(name: String?, track: DoubleArray?) = onMain {
        replayName = name
        this.track = track
        lastS = null
        route = name?.let { n ->
            runCatching { NavRoute.parse(context.assets.open("$n.route.json").bufferedReader().readText()) }
                .onFailure { Log.i(TAG, "no route file for $n: ${it.message}") }.getOrNull()
        }
        val r = route
        // older CSVs without the "s" column give the distance driven, which can differ a little from the road length
        val end = track?.lastOrNull() ?: 0.0
        trackScale = if (r != null && end > 0 && abs(end - r.totalM) / r.totalM > 0.03) r.totalM / end else 1.0
        if (r != null) {
            Log.i(TAG, "route ${r.name}: ${r.totalM.roundToInt()} m, ${r.steps.size} steps, track scale %.3f".format(trackScale))
            js("setRoute(${r.geometryJson()})")
            track?.firstOrNull()?.takeIf { it.isFinite() }?.let { js(String.format(Locale.US, "preview(%.1f)", it * trackScale)) }
        } else js("clearRoute()")
    }

    /** Live navigation guidance from NaviHelper (null when none). */
    fun setGuidance(g: NaviGuide.Guidance?) = onMain { live = g }

    fun update(s: Sample) = onMain {
        speedNum.text = (s.speed * 3.6).roundToInt().coerceAtLeast(0).toString()
        val r = route
        if (replayName != null && r != null) {
            val fromTrack = track?.getOrNull(s.t.toInt())?.takeIf { it.isFinite() }?.let { it * trackScale }
            val pos = (fromTrack ?: if (s.lat.isFinite() && s.lon.isFinite()) r.nearestS(s.lat, s.lon, lastS) else return@onMain)
                .coerceIn(0.0, r.totalM)
            lastS = pos
            js(String.format(Locale.US, "setS(%.1f,%.2f)", pos, s.speed))
            val step = r.nextStep(pos)
            when {
                // one turn source: the turn the engine (and the cue) sees; the route only lends a road name
                s.nextTurnM >= 0 -> {
                    val at = pos + s.nextTurnM
                    val named = r.steps.firstOrNull { abs(it.s - at) < 60 && it.type != "TURN_ARRIVE" }
                    val type = named?.type?.takeIf { ManeuverView.kindOf(it).isLeft() == s.nextTurnLeft && ManeuverView.kindOf(it).isTurn() }
                        ?: if (s.nextTurnLeft) "TURN_LEFT" else "TURN_RIGHT"
                    // the route's own short word when it names this very turn ("왼쪽 커브"), else the generic one
                    val word = named?.takeIf { it.type == type }?.text?.takeIf { it.isNotBlank() } ?: turnWord(type)
                    showTbt(type, s.nextTurnM, named?.road.orEmpty(), word)
                }
                step == null || step.type == "TURN_ARRIVE" ->
                    showTbt("TURN_ARRIVE", (step?.s ?: r.totalM) - pos, r.dest.ifBlank { r.title }, "목적지")
                else -> showTbt(step.type, step.s - pos, step.road, step.text.ifBlank { turnWord(step.type) })
            }
            followCue(s.nextTurnM)
            val remain = (r.totalM - pos).coerceAtLeast(0.0)
            // a replay knows its own timetable (stops included), like a navigation's traffic-aware estimate;
            // the trip average would jump by minutes at every red light
            val left = track?.let { (it.size - 1 - s.t).toDouble() }?.takeIf { it >= 0 }
            val avg = if (s.t > 20 && pos > 100) pos / s.t else maxOf(s.speed, 8.0)
            if (remain < 30) etaBar.gone(true) else showEta(remain, left ?: (remain / avg.coerceAtLeast(3.0)), r.dest)
            return@onMain
        }
        if (s.lat.isFinite() && s.lon.isFinite()) {
            js(String.format(Locale.US, "setPos(%.7f,%.7f,%.1f,%.2f)", s.lat, s.lon, if (s.bearing.isFinite()) s.bearing else 0.0, s.speed))
        }
        val g = live
        when {
            g?.turnType != null && g.turnM != null && g.turnM >= 0 ->
                showTbt(g.turnType, g.turnM, g.description.orEmpty(), turnWord(g.turnType))
            s.nextTurnM >= 0 ->
                showTbt(if (s.nextTurnLeft) "TURN_LEFT" else "TURN_RIGHT", s.nextTurnM, "", if (s.nextTurnLeft) "좌회전" else "우회전")
            else -> tbtCard.gone(true)
        }
        followCue(s.nextTurnM)
        if (g?.remainM != null && g.remainM > 0) showEta(g.remainM.toDouble(), g.remainS?.toDouble(), g.destName ?: "")
        else etaBar.gone(true)
    }

    /** Kept for callers that only know a position (no sample). */
    fun setPosition(lat: Double, lon: Double, bearing: Double) = onMain {
        if (!lat.isFinite() || !lon.isFinite()) return@onMain
        js(String.format(Locale.US, "setPos(%.7f,%.7f,%.1f,0)", lat, lon, if (bearing.isFinite()) bearing else 0.0))
    }

    fun setMood(theme: MoodTheme, mood: Mood, level: Float, label: String) = onMain {
        icon.theme = theme
        icon.mood = mood
        icon.level = level
        // a glance only: the level word (편안·주의·위험), the theme's phrase stays on the home card
        moodLabel.text = (label).substringBefore(" · ")
        val c = moodColor(mood)
        (ring.background as GradientDrawable).setStroke(context.dp(5), c)
        moodLabel.setTextColor(c)
    }

    /** Sickness probability (0..1) of the passenger who matters, built up from the trip's dose. */
    fun setRisk(p: Double) = onMain {
        riskValue.text = "${Ui.pct(p)}%"
        riskChip.background = Ui.rounded(Ui.riskColor(p), context.dpf(14f))
    }

    /** Next turn hint without a route ("300m 앞 좌회전"); null hides the card. */
    fun setNextTurn(text: String?) = onMain { if (text.isNullOrBlank()) tbtCard.gone(true) }

    /** A turn cue fired: arrow + distance under the character until the turn is passed (or a few seconds). */
    fun showCue(left: Boolean) = onMain {
        cueArrow.kind = if (left) ManeuverView.Kind.LEFT else ManeuverView.Kind.RIGHT
        cueAt = lastTurnM
        cueDist.text = cueText(lastTurnM)
        main.removeCallbacks(hideBubble)
        bubble.animate().cancel()
        if (bubble.visibility != VISIBLE) { bubble.alpha = 0f; bubble.translationY = -context.dpf(10f) }
        bubble.gone(false)
        bubble.animate().alpha(1f).translationY(0f).setDuration(220).start()
        main.postDelayed(hideBubble, CUE_SHOW_MS)
    }

    private var lastTurnM = -1.0

    private fun cueText(m: Double) = if (m in 0.0..30.0 || m < 0) "곧" else "${(m / 10).roundToInt() * 10}m"

    /** Counts the cue down with each sample; a growing distance means the turn is behind us. */
    private fun followCue(turnM: Double) {
        val prev = lastTurnM
        lastTurnM = turnM
        if (bubble.visibility != VISIBLE || cueAt < 0) return
        if (turnM < 0 || (prev >= 0 && turnM > prev + 5)) { main.removeCallbacks(hideBubble); hideBubble.run(); return }
        cueDist.text = cueText(turnM)
    }

    /** Clears the trail, route and guidance, e.g. when a new trip starts. */
    fun reset() = onMain {
        replayName = null
        route = null
        track = null
        lastS = null
        tbtCard.gone(true)
        etaBar.gone(true)
        speedNum.text = "0"
        riskValue.text = "0%"
        riskChip.background = Ui.rounded(Ui.riskColor(0.0), context.dpf(14f))
        main.removeCallbacks(hideBubble)
        bubble.gone(true)
        cueAt = -1.0
        lastTurnM = -1.0
        js("resetAll()")
    }

    /** Releases the WebView; call from the host's onDestroy. The panel is unusable afterwards. */
    fun destroy() = onMain {
        destroyed = true
        ready = false
        queue.clear()
        main.removeCallbacksAndMessages(null)
        web.stopLoading()
        removeView(web)
        web.destroy()
    }

    /** Turn card: arrow, distance and the road name when it fits on one line, otherwise the maneuver word. */
    private fun showTbt(type: String, distM: Double, road: String, word: String) {
        maneuver.kind = ManeuverView.kindOf(type)
        tbtDist.text = when {
            type == "TURN_ARRIVE" && distM < 30 -> "도착"
            distM >= 1000 -> String.format(Locale.US, "%.1fkm", distM / 1000)
            distM >= 100 -> "${(distM / 10).roundToInt() * 10}m"
            else -> "${distM.roundToInt().coerceAtLeast(0)}m"
        }
        val fits = road.isNotBlank() && tbtRoad.paint.measureText(road) <= context.dp(300)
        tbtRoad.text = if (fits) road else word
        tbtRoad.gone(tbtRoad.text.isBlank())
        tbtCard.gone(false)
    }

    private fun showEta(remainM: Double, remainS: Double?, dest: String) {
        val rest = if (remainM >= 1000) String.format(Locale.US, "%.1fkm", remainM / 1000) else "${remainM.roundToInt()}m"
        if (remainS != null && remainS.isFinite()) {
            val at = Calendar.getInstance().apply { add(Calendar.SECOND, remainS.roundToInt()) }
            etaTime.text = String.format(Locale.KOREA, "도착 %02d:%02d", at.get(Calendar.HOUR_OF_DAY), at.get(Calendar.MINUTE))
            val min = (remainS / 60).roundToInt().coerceAtLeast(1)
            etaRest.text = "$rest · " + if (min >= 60) "${min / 60}시간 ${min % 60}분" else "${min}분"
        } else {
            etaTime.text = "남은 거리"
            etaRest.text = rest
        }
        etaDest.text = dest
        etaDest.gone(dest.isBlank())
        etaBar.gone(false)
    }

    private fun turnWord(type: String) = when (ManeuverView.kindOf(type)) {
        ManeuverView.Kind.LEFT -> "좌회전"
        ManeuverView.Kind.RIGHT -> "우회전"
        ManeuverView.Kind.KEEP_L -> "왼쪽 방향"
        ManeuverView.Kind.KEEP_R -> "오른쪽 방향"
        ManeuverView.Kind.UTURN -> "유턴"
        ManeuverView.Kind.ARRIVE -> "목적지"
        ManeuverView.Kind.STRAIGHT -> "직진"
    }


    private fun moodColor(m: Mood) = when (m) { Mood.CALM -> Ui.PRIMARY; Mood.UNEASY -> Color.parseColor("#D97706"); Mood.SICK -> Ui.ALERT }

    private fun js(code: String) {
        if (destroyed) return
        if (ready) web.evaluateJavascript(code, null)
        else {
            // before the page is up only the latest of each call matters
            val fn = code.substringBefore('(')
            queue.removeAll { it.substringBefore('(') == fn }
            queue += code
        }
    }

    /** Sensor/location SDK callbacks may arrive off the main thread; WebView and views are main-only. */
    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) { if (!destroyed) block() }
        else main.post { if (!destroyed) block() }
    }

    private fun showFallback() {
        ready = false
        web.gone(true)
        fallback.gone(false)
    }

    private companion object {
        const val CHAR_RING = 112

        val HTML = """
<!DOCTYPE html><html><head>
<meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
<style>
html,body,#map{margin:0;padding:0;height:100%;width:100%;background:#F3F4F7;overflow:hidden}
.car{width:64px;height:64px;filter:drop-shadow(0 3px 5px rgba(20,60,140,.45))}
.car svg{width:64px;height:64px;display:block}
</style></head><body><div id="map"></div>
<script>
var MCB=window.MC||{engine:function(){},log:function(){}};
var STYLE='https://tiles.openfreemap.org/styles/positron';
var GL='https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.';
var LF='https://unpkg.com/leaflet@1.9.4/dist/leaflet.';
var BLUE='#2B6CF6',DONE='#A7AFBD';
var CAR='<svg viewBox="0 0 64 64"><circle cx="32" cy="32" r="29" fill="#fff"/><circle cx="32" cy="32" r="24" fill="'+BLUE+'"/><path d="M32 14 L44 45 L32 38 L20 45 Z" fill="#fff"/></svg>';
var EMPTY={type:'FeatureCollection',features:[]};
var ENG=null,route=null,cum=null,total=0,trail=[],have=false,threeD=true;
var sPrev=0,sTgt=0,pPrev=null,pTgt=null,bGps=0,tUpd=0,interval=1000,speed=0;
var carB=0,camB=null,zoom=17.0,zLvl=0,tDraw=0;
// per-sample camera for the canvas engine (see push): displayed route position and camera bearing
var sM=null,camT=null,K=2;
function now(){return performance.now();}
function dist(a,b){var dy=(b[0]-a[0])*111195,dx=(b[1]-a[1])*111195*Math.cos(a[0]*Math.PI/180);return Math.sqrt(dx*dx+dy*dy);}
function brg(a,b){var dy=b[0]-a[0],dx=(b[1]-a[1])*Math.cos(a[0]*Math.PI/180);return (Math.atan2(dx,dy)*180/Math.PI+360)%360;}
function angLerp(a,b,f){var d=((b-a+540)%360)-180;return (a+d*f+360)%360;}
function tick(){var t=now();if(tUpd)interval=Math.min(1500,Math.max(120,interval*0.7+(t-tUpd)*0.3));tUpd=t;}
// past 1 the car keeps going the same way (dead reckoning) for up to half an interval, so a late sample never freezes it
function frac(){return Math.min(1.5,(now()-tUpd)/interval);}
function at(s){var lo=0,hi=cum.length-1;if(s<=0)return route[0];if(s>=total)return route[hi];
 while(hi-lo>1){var m=(lo+hi)>>1;if(cum[m]<=s)lo=m;else hi=m;}
 var f=(s-cum[lo])/Math.max(1e-6,cum[hi]-cum[lo]),a=route[lo],b=route[hi];return [a[0]+(b[0]-a[0])*f,a[1]+(b[1]-a[1])*f];}
function curS(){return tUpd?sPrev+(sTgt-sPrev)*frac():sTgt;}
function curP(){var f=frac();return [pPrev[0]+(pTgt[0]-pPrev[0])*f,pPrev[1]+(pTgt[1]-pPrev[1])*f];}
function setRoute(g){sM=null;camT=null;if(ENG&&ENG.reset)ENG.reset();route=g;cum=[0];for(var i=1;i<g.length;i++)cum.push(cum[i-1]+dist(g[i-1],g[i]));total=cum[cum.length-1];have=false;trail=[];if(ENG){ENG.route(route);ENG.trail(trail);}}
function clearRoute(){route=null;cum=null;total=0;have=false;if(ENG)ENG.route(null);}
function resetAll(){clearRoute();trail=[];camB=null;sM=null;camT=null;if(ENG&&ENG.reset)ENG.reset();zLvl=0;zoom=17.0;if(ENG)ENG.trail(trail);}
// aim at where the car will be at the next sample (1 s of the scenario later), so the display neither lags nor
// overshoots and then backs up; never steer backwards
function setS(s,v){if(!route)return;speed=v;var c=have?curS():s;if(!have||Math.abs(s-c)>300)c=s;
 sPrev=c;sTgt=Math.max(c,s+Math.max(0,v));have=true;tick();push(s,v,false);}
// a new scenario: show its start (and load its tiles) before the first sample arrives
function preview(s){if(!route)return;s=Math.max(0,Math.min(total,s));sPrev=sTgt=s;speed=0;tUpd=0;have=true;camB=null;sM=null;push(s,0,true);}
function setPos(lat,lon,b,v){if(route)return;speed=v;var p=[lat,lon],c=have?curP():p;if(!have||dist(c,p)>500)c=p;pPrev=c;pTgt=p;bGps=b;var jump=!have||dist(c,p)>500;have=true;tick();push(0,v,jump);
 var l=trail[trail.length-1];if(!l||dist(l,p)>2){trail.push(p);if(trail.length>120)trail.shift();if(ENG)ENG.trail(trail);}}
// Canvas engine: the map is redrawn only now and then; between samples the browser's compositor glides the
// tilted map (CSS transitions), which keeps running even while a redraw blocks the page. Each sample aims the
// camera K samples ahead, so the next sample retargets it mid-way and the motion never stops and restarts.
function push(s,v,instant){if(!ENG||!ENG.move)return;var t=now(),dur=interval*K,p,b,look,prog=null;
 if(route){var sd=s;if(sM&&!instant){var f=Math.min(1,(t-sM.t0)/sM.dur);sd=sM.a+(sM.b-sM.a)*f;}
  if(!sM||instant||Math.abs(s-sd)>300){instant=true;sd=s;}
  var sT=instant?s:Math.min(total,Math.max(sd,s+Math.max(0,v)*K));sM={t0:t,dur:dur,a:sd,b:sT};
  p=at(sT);b=brg(at(Math.max(0,sT-4)),at(Math.min(total,sT+6)));var ah=at(Math.min(total,sT+45));look=dist(p,ah)>5?brg(p,ah):b;prog=total>0?sd/total:0;}
 else{var r=Math.max(0,v)*K/111195,q=b=look=bGps;var la=pTgt[0],lo=pTgt[1];if(!camT)instant=true;
  p=[la+r*Math.cos(q*Math.PI/180),lo+r*Math.sin(q*Math.PI/180)/Math.cos(la*Math.PI/180)];}
 // a calm camera: hairpins turn it at most 25 degrees per second of driving, so the map never whips round
 if(instant||camT==null)camT=look;else{var dc=((look-camT+540)%360)-180;camT=(camT+Math.max(-25,Math.min(25,dc*0.5))+360)%360;}
 ENG.move(p,camT,b-camT,dur,zoomLevel(v*3.6),prog,instant);}
function set3d(on){threeD=on;if(ENG&&ENG.tilt)ENG.tilt();}
// speed bands with hysteresis (up at 45/80 km/h, down at 38/70) so the zoom never flips back and forth
function zoomLevel(kmh){if(zLvl<2&&kmh>80)zLvl=2;else if(zLvl<1&&kmh>45)zLvl=1;else if(zLvl===2&&kmh<70)zLvl=1;else if(zLvl===1&&kmh<38)zLvl=0;return zLvl;}
function frame(){requestAnimationFrame(frame);var t=now();if(!have||!ENG||ENG.move)return;
 // the canvas engines redraw the whole map per frame: 30 fps is smooth and leaves the CPU some room
 if(ENG.fps&&t-tDraw<1000/ENG.fps-4)return;var el=tDraw?Math.min(100,t-tDraw):16.7,k=el/16.7;tDraw=t;
 var p,b,look,prog=null;
 if(route){var s=Math.max(0,Math.min(total,curS()));p=at(s);b=brg(at(Math.max(0,s-4)),at(Math.min(total,s+6)));
  var ahead=at(Math.min(total,s+45));look=dist(p,ahead)>5?brg(p,ahead):b;prog=total>0?s/total:0;}
 else{p=curP();b=bGps;look=bGps;}
 carB=angLerp(carB,b,1-Math.pow(0.7,k));camB=camB==null?look:angLerp(camB,look,1-Math.pow(0.93,k));
 var lv=zoomLevel(speed*3.6),zt=[17.0,16.3,15.6][lv];zoom+=(zt-zoom)*(1-Math.exp(-el/220));if(Math.abs(zt-zoom)<0.003)zoom=zt;
 ENG.draw(p,carB,camB,zoom,prog,lv);}
function load(src,ok,fail){var s=document.createElement('script');s.src=src;s.onload=ok;s.onerror=fail;document.head.appendChild(s);}
function css(href){var l=document.createElement('link');l.rel='stylesheet';l.href=href;document.head.appendChild(l);}
function webgl(){try{var c=document.createElement('canvas');return !!(window.WebGLRenderingContext&&(c.getContext('webgl')||c.getContext('experimental-webgl')));}catch(e){return false;}}
function feat(g){return g&&g.length>1?{type:'Feature',properties:{},geometry:{type:'LineString',coordinates:g.map(function(p){return [p[1],p[0]];})}}:EMPTY;}
function grad(p){p=Math.max(1e-6,Math.min(0.999999,p));return ['step',['line-progress'],DONE,p,BLUE];}
function startGl(){
 var m;
 try{m=new maplibregl.Map({container:'map',style:STYLE,center:[126.978,37.5665],zoom:16.8,pitch:56,maxPitch:70,
  interactive:false,attributionControl:false,fadeDuration:0,localIdeographFontFamily:"'Noto Sans CJK KR','Noto Sans KR',sans-serif"});}
 catch(e){MCB.log('maplibre init failed: '+e);startOl();return;}
 var el=document.createElement('div');el.className='car';el.innerHTML=CAR;
 var car=new maplibregl.Marker({element:el,rotationAlignment:'map',pitchAlignment:'map'});
 var placed=false,lastProg=-1,loaded=false,gave=false;
 // the style host may be down or slow: fall back to raster tiles from another host instead of a blank map
 function bail(why){if(loaded||gave)return;gave=true;MCB.log('maplibre fallback: '+why);try{m.remove();}catch(e){}
  document.getElementById('map').innerHTML='';startLeaflet();}
 m.on('error',function(e){MCB.log('gl error: '+((e&&e.error&&e.error.message)||e));if(!m.isStyleLoaded())bail('style error');});
 setTimeout(function(){bail('style timeout');},20000);
 m.on('load',function(){
  loaded=true;
  try{
  var first=null;
  m.getStyle().layers.forEach(function(l){
   if(l.type!=='symbol')return;if(!first)first=l.id;
   var tf=m.getLayoutProperty(l.id,'text-field');
   if(tf&&JSON.stringify(tf).indexOf('name')>=0)m.setLayoutProperty(l.id,'text-field',['coalesce',['get','name:ko'],['get','name'],['get','name:latin']]);
  });
  try{m.setPaintProperty('background','background-color','#F3F4F7');m.setPaintProperty('building','fill-color','#E4E6EB');}catch(e){}
  m.addLayer({id:'mc-3d',type:'fill-extrusion',source:'openmaptiles','source-layer':'building',minzoom:14,paint:{
   'fill-extrusion-color':'#DFE2E8','fill-extrusion-opacity':0.88,
   'fill-extrusion-height':['coalesce',['get','render_height'],8],'fill-extrusion-base':['coalesce',['get','render_min_height'],0]}},first);
  m.addSource('mc-route',{type:'geojson',data:EMPTY,lineMetrics:true});
  m.addSource('mc-trail',{type:'geojson',data:EMPTY});
  m.addSource('mc-dest',{type:'geojson',data:EMPTY});
  var w=function(a,b){return ['interpolate',['linear'],['zoom'],13,a,18,b];};
  m.addLayer({id:'mc-trail',type:'line',source:'mc-trail',layout:{'line-cap':'round','line-join':'round'},paint:{'line-color':BLUE,'line-opacity':0.45,'line-width':w(4,12)}},first);
  m.addLayer({id:'mc-route-case',type:'line',source:'mc-route',layout:{'line-cap':'round','line-join':'round'},paint:{'line-color':'#fff','line-width':w(9,30)}},first);
  m.addLayer({id:'mc-route',type:'line',source:'mc-route',layout:{'line-cap':'round','line-join':'round'},paint:{'line-gradient':grad(0),'line-width':w(5,20)}},first);
  m.addLayer({id:'mc-dest',type:'circle',source:'mc-dest',paint:{'circle-radius':11,'circle-color':'#E5484D','circle-stroke-color':'#fff','circle-stroke-width':4}});
  ENG={draw:function(p,cb,camb,z,prog){
    car.setLngLat([p[1],p[0]]).setRotation(cb);if(!placed){car.addTo(m);placed=true;}
    var H=m.getContainer().clientHeight;
    m.jumpTo({center:[p[1],p[0]],bearing:camb,pitch:threeD?56:0,zoom:threeD?z:z-0.3,padding:{top:H*(threeD?0.42:0.3),bottom:0,left:0,right:0}});
    if(prog!=null&&Math.abs(prog-lastProg)>0.0004){lastProg=prog;m.setPaintProperty('mc-route','line-gradient',grad(prog));}},
   route:function(g){m.getSource('mc-route').setData(feat(g));
    m.getSource('mc-dest').setData(g?{type:'Feature',properties:{},geometry:{type:'Point',coordinates:[g[g.length-1][1],g[g.length-1][0]]}}:EMPTY);
    lastProg=-1;m.setPaintProperty('mc-route','line-gradient',grad(0));},
   trail:function(t){m.getSource('mc-trail').setData(feat(route?null:t));}};
  ENG.route(route);ENG.trail(trail);
  MCB.engine('maplibre');
  }catch(e){ENG=null;loaded=false;bail('load handler: '+e);}
 });
}
// No WebGL (e.g. the emulator's WebView blocklists its GPU): OpenLayers draws the same OpenFreeMap
// vector style on a 2D canvas, so labels stay Korean, clean and upright while the map turns.
var OL='https://cdn.jsdelivr.net/npm/ol@10.2.1/';
// Darker ground and casings so white roads stand out, closer to the Pleos navigation palette.
var TUNE={background:{'background-color':'#E7E9EE'},landuse_residential:{'fill-color':'#E2E5EA'},park:{'fill-color':'#D9E6D6'},
 water:{'fill-color':'#BFD3EA'},building:{'fill-color':'#D5D9E0','fill-outline-color':'#C8CDD6'},
 highway_minor:{'line-color':'#FFFFFF','line-opacity':1},highway_path:{'line-color':'#F4F5F7'},
 highway_major_casing:{'line-color':'#BFC5CF'},highway_motorway_casing:{'line-color':'#BFC5CF'},
 highway_motorway_bridge_casing:{'line-color':'#BFC5CF'},tunnel_motorway_casing:{'line-color':'#C9CED6'}};
function bigger(v){if(typeof v==='number')return v*1.7;
 if(v&&v.stops)return {base:v.base,stops:v.stops.map(function(s){return [s[0],s[1]*1.7];})};return v;}
function startOl(){
 css(OL+'ol.css');
 load(OL+'dist/ol.js',function(){load('https://cdn.jsdelivr.net/npm/ol-mapbox-style@12.3.5/dist/olms.js',function(){
  // cam (tilt + heading) > zm (zoom easing) > box (OL map, slides under the car); the car sits outside, fixed
  var R=70,box=document.getElementById('map'),cam=document.createElement('div'),zm=document.createElement('div');
  document.body.insertBefore(cam,box);cam.appendChild(zm);zm.appendChild(box);
  cam.style.cssText='position:absolute;transform-origin:50% 50%;will-change:transform';
  zm.style.cssText='position:absolute;left:0;top:0;width:100%;height:100%;transform-origin:50% 50%;will-change:transform';
  box.style.cssText='position:absolute;left:0;top:0;width:100%;height:100%;will-change:transform';
  var carEl=document.createElement('div');carEl.className='car';carEl.innerHTML=CAR;
  carEl.style.cssText='position:absolute;transform-origin:50% 50%;will-change:transform';document.body.appendChild(carEl);
  function size(){var W=innerWidth,H=innerHeight,S=Math.ceil(Math.sqrt(W*W+H*H)*1.25)+2*(R+40);
   cam.style.width=S+'px';cam.style.height=S+'px';cam.style.left=((W-S)/2)+'px';cam.style.top=(H*0.68-S/2)+'px';
   carEl.style.left=(W/2-32)+'px';carEl.style.top=(H*0.68-32)+'px';
   document.body.style.perspective='950px';document.body.style.perspectiveOrigin='50% '+(-H*0.15)+'px';}
  size();
  var view=new ol.View({center:ol.proj.fromLonLat([126.978,37.5665]),zoom:17});
  // pixelRatio 1: the emulator's CPU draws vector tiles slowly; whole zoom levels keep the tile cache useful
  var m=new ol.Map({target:box,view:view,controls:[],interactions:[],pixelRatio:1});
  addEventListener('resize',function(){size();m.updateSize();});
  fetch(STYLE).then(function(r){return r.json();}).then(function(st){
   st.layers.forEach(function(l){var y=l.layout,pt=l.paint||{},c=TUNE[l.id];
    if(c)for(var k in c)pt[k]=c[k];l.paint=pt;
    if(!y||!y['text-field'])return;
    y['text-field']=['coalesce',['get','name:ko'],['get','name']];y['text-size']=bigger(y['text-size']||12);});
   return olms.apply(m,st);
  }).catch(function(e){MCB.log('ol style failed: '+e);ENG=null;m.setTarget(null);document.body.appendChild(box);cam.remove();carEl.remove();
   box.innerHTML='';box.style.cssText='';startLeaflet();});
  function ll(p){return ol.proj.fromLonLat([p[1],p[0]]);}
  function lineStyle(c,w){return new ol.style.Style({stroke:new ol.style.Stroke({color:c,width:w,lineCap:'round',lineJoin:'round'})});}
  var cas=new ol.Feature(),done=new ol.Feature(),ahead=new ol.Feature(),tr=new ol.Feature(),dest=new ol.Feature();
  cas.setStyle(lineStyle('#fff',24));done.setStyle(lineStyle(DONE,14));ahead.setStyle(lineStyle(BLUE,14));
  tr.setStyle(lineStyle('rgba(43,108,246,.45)',8));
  dest.setStyle(new ol.style.Style({image:new ol.style.Circle({radius:11,fill:new ol.style.Fill({color:'#E5484D'}),stroke:new ol.style.Stroke({color:'#fff',width:4})})}));
  m.addLayer(new ol.layer.Vector({source:new ol.source.Vector({features:[tr,cas,done,ahead,dest]}),zIndex:100}));
  function geom(g){return g&&g.length>1?new ol.geom.LineString(g.map(ll)):null;}
  function split(prog){if(!route||prog==null)return;var s=prog*total,i=0,p=at(s);while(i<cum.length-1&&cum[i+1]<s)i++;
   done.setGeometry(geom(route.slice(0,i+1).concat([p])));ahead.setGeometry(geom([p].concat(route.slice(i+1))));}
  // C: view centre (map units); mv: the transition in flight (x,y = car offset from C in px, rz camera, cr car)
  var C=null,zi=17,lastRe=0,mv=null;
  function cur(t){var f=Math.max(0,Math.min(1,(t-mv.t0)/mv.dur)),o={};for(var k in mv.b)o[k]=mv.a[k]+(mv.b[k]-mv.a[k])*f;return o;}
  function apply(o,dur){var tr=dur?'transform '+Math.round(dur)+'ms linear':'none',tilt='rotateX('+(threeD?52:0)+'deg) ';
   box.style.transition=cam.style.transition=carEl.style.transition=tr;
   box.style.transform='translate('+(-o.x).toFixed(1)+'px,'+(-o.y).toFixed(1)+'px)';
   cam.style.transform=tilt+'rotateZ('+(-o.rz).toFixed(2)+'deg)';
   carEl.style.transform=tilt+'rotateZ('+o.cr.toFixed(1)+'deg)';}
  function wrap(from,to){return from+((((to-from)%360)+540)%360-180);}
  var zoomIn=0,zoomTimer=0,lastProg=null;
  // redraw around where the car is shown (it drifted off-centre, the zoom band changed, or the route colours are stale)
  function recenter(z,prog,ease){var t=now(),c=cur(t),r=view.getResolution(),C0=C,Cn=[C0[0]+c.x*r,C0[1]-c.y*r];
   view.setCenter(Cn);if(z!==zi){zi=z;view.setZoom(zi);}split(prog);m.renderSync();
   var r2=view.getResolution(),t2=now(),c2=cur(t2),b=mv.b;C=Cn;lastRe=t2;
   function to(o){return {x:(C0[0]+o.x*r-Cn[0])/r2,y:(Cn[1]-(C0[1]-o.y*r))/r2,rz:o.rz,cr:o.cr};}
   if(r2!==r){zm.style.transition='none';zm.style.transform=ease?'scale('+(r2/r)+')':'none';void zm.offsetWidth;
    if(ease){zm.style.transition='transform 700ms ease-in-out';zm.style.transform='none';}}
   // same picture in the new frame, then keep gliding toward the same point
   var a=to(c2),tb=to(b),left=mv.t0+mv.dur-t2;apply(a,0);void box.offsetWidth;
   mv={t0:t2,dur:Math.max(1,left),a:a,b:tb};if(left>1)apply(tb,left);}
  ENG={reset:function(){C=null;},
   tilt:function(){if(mv){var o=cur(now());mv={t0:now(),dur:1,a:o,b:o};apply(o,0);}},
   move:function(lat,rzT,crT,dur,lv,prog,instant){var t=now(),Pw=ll(lat),zt=[17,16,15][lv];lastProg=prog;
    if(instant||!C||!mv){clearTimeout(zoomTimer);zoomIn=0;C=Pw;zi=zt;view.setZoom(zi);view.setCenter(C);split(prog);m.renderSync();lastRe=now();
     var o={x:0,y:0,rz:rzT,cr:crT};mv={t0:now(),dur:1,a:o,b:o};zm.style.transition='none';zm.style.transform='none';apply(o,0);return;}
    // zooming in: first enlarge the current picture, then redraw at the new level (a sharper copy of the same view);
    // zooming out redraws at once (it covers more ground) and shrinks from the old size
    if(zt>zi&&!zoomIn){zoomIn=zt;zm.style.transition='transform 700ms ease-in-out';zm.style.transform='scale('+Math.pow(2,zt-zi)+')';
     zoomTimer=setTimeout(function(){recenter(zoomIn,lastProg,false);zoomIn=0;},720);}
    var c=cur(t);if(zt<zi||(zoomIn===0&&zt===zi&&(Math.hypot(c.x,c.y)>R||(t-lastRe>4000&&Math.hypot(c.x,c.y)>16))))recenter(zoomIn||zt,prog,true);
    t=now();c=cur(t);var r=view.getResolution();
    var b={x:(Pw[0]-C[0])/r,y:-(Pw[1]-C[1])/r,rz:wrap(c.rz,rzT),cr:wrap(c.cr,crT)};
    mv={t0:t,dur:dur,a:c,b:b};apply(b,dur);},
   route:function(g){cas.setGeometry(geom(g));done.setGeometry(null);ahead.setGeometry(geom(g));
    dest.setGeometry(g?new ol.geom.Point(ll(g[g.length-1])):null);},
   trail:function(t){tr.setGeometry(route?null:geom(t));}};
  ENG.route(route);ENG.trail(trail);
  if(have&&route)push(sTgt,0,true);
  MCB.engine('openlayers');
 },function(){MCB.log('olms failed to load');startLeaflet();});},function(){MCB.log('ol failed to load');startLeaflet();});
}
// Last resort when the vector engines can't load: OSM raster tiles in the same tilted stage.
function startLeaflet(){
 css(LF+'css');
 load(LF+'js',function(){
  var st=document.createElement('style');
  st.textContent='#map{position:absolute;transform-origin:50% 50%;background:#F3F4F7}.leaflet-tile-pane{filter:grayscale(1) brightness(1.07) contrast(.9)}'+
   '.leaflet-container{background:#F3F4F7}body{perspective:950px}';
  document.head.appendChild(st);
  var box=document.getElementById('map'),W=0,H=0,S=0;
  function size(){W=innerWidth;H=innerHeight;S=Math.ceil(Math.sqrt(W*W+H*H)*1.9);
   box.style.width=S+'px';box.style.height=S+'px';box.style.left=((W-S)/2)+'px';box.style.top=(H*0.68-S/2)+'px';
   document.body.style.perspectiveOrigin='50% '+(H*0.3)+'px';}
  size();
  var m=L.map('map',{zoomControl:false,attributionControl:false,zoomSnap:1,renderer:L.svg({padding:0.5}),fadeAnimation:false}).setView([37.5665,126.978],17);
  addEventListener('resize',function(){size();m.invalidateSize(false);});
  // one zoom level down at double size: street names stay readable from the driver's seat
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,tileSize:512,zoomOffset:-1,keepBuffer:4}).addTo(m);
  var car=L.marker([37.5665,126.978],{interactive:false,icon:L.divIcon({className:'',html:'<div class="car">'+CAR+'</div>',iconSize:[64,64],iconAnchor:[32,32]})});
  var cas=L.polyline([],{color:'#fff',weight:22,lineCap:'round',lineJoin:'round'}).addTo(m),
   done=L.polyline([],{color:DONE,weight:13,lineCap:'round',lineJoin:'round'}).addTo(m),
   ahead=L.polyline([],{color:BLUE,weight:13,lineCap:'round',lineJoin:'round'}).addTo(m),
   tr=L.polyline([],{color:BLUE,weight:8,opacity:0.45}).addTo(m);
  var placed=false,lastSplit=0,z0=17;
  ENG={fps:30,draw:function(p,cb,camb,z,prog,lv){
    z0=[17,16,15][lv];
    car.setLatLng(p);if(!placed){car.addTo(m);placed=true;}
    m.setView(p,z0,{animate:false});
    box.style.transform='rotateX('+(threeD?54:0)+'deg) rotateZ('+(-camb)+'deg)';
    var e=car.getElement();if(e)e.firstChild.style.transform='rotate('+cb+'deg)';
    var t=now();
    if(route&&prog!=null&&t-lastSplit>200){lastSplit=t;var s=prog*total,i=0;while(i<cum.length-1&&cum[i+1]<s)i++;
     done.setLatLngs(route.slice(0,i+1).concat([p]));ahead.setLatLngs([p].concat(route.slice(i+1)));}},
   route:function(g){cas.setLatLngs(g||[]);done.setLatLngs([]);ahead.setLatLngs(g||[]);},
   trail:function(t){tr.setLatLngs(route?[]:t);}};
  ENG.route(route);ENG.trail(trail);
  MCB.engine('leaflet');
 },function(){MCB.engine('none');});
}
(function boot(){
 requestAnimationFrame(frame);
 if(!webgl()){MCB.log('WebGL unavailable');startOl();return;}
 css(GL+'css');
 load(GL+'js',startGl,function(){MCB.log('maplibre script failed to load');startOl();});
})();
</script></body></html>
""".trimIndent()
    }
}

/** Big white maneuver arrow for the turn-by-turn card, drawn per turn type. */
class ManeuverView(context: Context) : View(context) {
    enum class Kind { STRAIGHT, LEFT, RIGHT, KEEP_L, KEEP_R, UTURN, ARRIVE;
        fun isLeft() = this == LEFT || this == KEEP_L
        fun isTurn() = this == LEFT || this == RIGHT || this == KEEP_L || this == KEEP_R
    }

    var kind = Kind.STRAIGHT
        set(value) { if (field != value) { field = value; invalidate() } }

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = Color.WHITE
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val dim = Paint(line).apply { color = Color.parseColor("#4A505B") }
    private val path = Path()
    private val arc = RectF()

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        c.save()
        c.translate((width - s) / 2, (height - s) / 2)
        line.strokeWidth = s * 0.12f
        dim.strokeWidth = s * 0.12f
        when (kind) {
            Kind.STRAIGHT -> arrow(c, s, floatArrayOf(0.5f, 0.92f, 0.5f, 0.3f))
            Kind.LEFT -> arrow(c, s, floatArrayOf(0.62f, 0.92f, 0.62f, 0.5f, 0.32f, 0.5f))
            Kind.RIGHT -> arrow(c, s, floatArrayOf(0.38f, 0.92f, 0.38f, 0.5f, 0.68f, 0.5f))
            Kind.KEEP_L -> {
                c.drawLine(s * 0.55f, s * 0.6f, s * 0.7f, s * 0.2f, dim)
                arrow(c, s, floatArrayOf(0.55f, 0.92f, 0.55f, 0.62f, 0.36f, 0.34f))
            }
            Kind.KEEP_R -> {
                c.drawLine(s * 0.45f, s * 0.6f, s * 0.3f, s * 0.2f, dim)
                arrow(c, s, floatArrayOf(0.45f, 0.92f, 0.45f, 0.62f, 0.64f, 0.34f))
            }
            Kind.UTURN -> {
                path.reset()
                path.moveTo(s * 0.66f, s * 0.92f)
                path.lineTo(s * 0.66f, s * 0.42f)
                arc.set(s * 0.3f, s * 0.24f, s * 0.66f, s * 0.6f)
                path.arcTo(arc, 0f, -180f, false)
                path.lineTo(s * 0.3f, s * 0.6f)
                c.drawPath(path, line)
                head(c, s, s * 0.3f, s * 0.76f, 90f)
            }
            Kind.ARRIVE -> {
                path.reset()
                val cx = s * 0.5f
                path.moveTo(cx, s * 0.92f)
                path.cubicTo(s * 0.2f, s * 0.55f, s * 0.18f, s * 0.1f, cx, s * 0.1f)
                path.cubicTo(s * 0.82f, s * 0.1f, s * 0.8f, s * 0.55f, cx, s * 0.92f)
                c.drawPath(path, fill)
                fill.color = Color.parseColor("#111317")
                c.drawCircle(cx, s * 0.4f, s * 0.12f, fill)
                fill.color = Color.WHITE
            }
        }
        c.restore()
    }

    /** Polyline through [pts] (x,y pairs in 0..1) with an arrowhead at the last point. */
    private fun arrow(c: Canvas, s: Float, pts: FloatArray) {
        val n = pts.size / 2
        val x1 = pts[2 * n - 2] * s; val y1 = pts[2 * n - 1] * s
        val x0 = pts[2 * n - 4] * s; val y0 = pts[2 * n - 3] * s
        val ang = Math.toDegrees(kotlin.math.atan2((y1 - y0).toDouble(), (x1 - x0).toDouble())).toFloat()
        // stop the shaft short of the tip so the round cap does not poke through the head
        val back = s * 0.12f
        val len = kotlin.math.hypot(x1 - x0, y1 - y0)
        path.reset()
        path.moveTo(pts[0] * s, pts[1] * s)
        for (i in 1 until n - 1) path.lineTo(pts[2 * i] * s, pts[2 * i + 1] * s)
        path.lineTo(x1 - (x1 - x0) / len * back, y1 - (y1 - y0) / len * back)
        c.drawPath(path, line)
        head(c, s, x1, y1, ang)
    }

    private fun head(c: Canvas, s: Float, x: Float, y: Float, angleDeg: Float) {
        c.save()
        c.rotate(angleDeg, x, y)
        path.reset()
        path.moveTo(x + s * 0.04f, y)
        path.lineTo(x - s * 0.2f, y - s * 0.17f)
        path.lineTo(x - s * 0.2f, y + s * 0.17f)
        path.close()
        c.drawPath(path, fill)
        c.restore()
    }

    companion object {
        fun kindOf(type: String?): Kind {
            val t = type ?: return Kind.STRAIGHT
            return when {
                t.contains("ARRIVE") || t.contains("SERVICEAREA") || t.contains("RESTAREA") -> Kind.ARRIVE
                t.contains("UTURN") -> Kind.UTURN
                t == "TURN_LEFT" || t == "TURN_SHARP_L" -> Kind.LEFT
                t == "TURN_RIGHT" || t == "TURN_SHARP_R" -> Kind.RIGHT
                t.endsWith("_L") -> Kind.KEEP_L
                t.endsWith("_R") -> Kind.KEEP_R
                else -> Kind.STRAIGHT
            }
        }
    }
}
