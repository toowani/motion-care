package ai.motioncare.app

import ai.motioncare.app.theme.MoodTheme
import ai.motioncare.app.theme.MoodThemes
import ai.motioncare.app.ui.HomeView
import ai.motioncare.app.ui.SettingsPanel
import ai.motioncare.core.Action
import ai.motioncare.core.Engine
import ai.motioncare.core.FamilyMonitor
import ai.motioncare.core.Params
import ai.motioncare.core.Profile
import ai.motioncare.core.ProfileLearning
import ai.motioncare.core.Rating
import ai.motioncare.core.Sample
import ai.motioncare.core.ScenarioCsv
import ai.motioncare.core.sicknessProbability
import ai.pleos.playground.vehicle.Vehicle
import android.app.Activity
import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout

private const val TAG = "MotionCare"
private const val PERMISSIONS_REQUEST = 1
/** A live trip ends after the car has been still this long; then the post-trip question is asked. */
private const val TRIP_END_STILL_S = 60.0
/** Below this cumulative dose a "멀미 났어" report is not used to learn a child's threshold. */
private const val MIN_LEARN_DOSE = 5.0

/**
 * Home screen + settings. The app follows the car's live signals from launch; the demo panel can
 * replay simulated scenarios instead. The engine decides and the car acts — turn cues by voice and
 * light, fresh air, rest-stop waypoint — and in family mode it follows the most sensitive
 * passenger and learns from one post-trip question per child.
 */
class MainActivity : Activity(), HomeView.Callbacks {
    private lateinit var params: Params
    private lateinit var settings: Settings
    private lateinit var home: HomeView
    private lateinit var panel: SettingsPanel
    private lateinit var voice: Voice
    private lateinit var vehicle: Vehicle
    private lateinit var hvac: HvacController
    private lateinit var lightCue: LightCue
    private lateinit var navi: NaviGuide
    private lateinit var profiles: ProfileStore
    private var theme: MoodTheme = MoodThemes.byId(null)
    private var car: Car? = null

    private var source: SignalSource? = null
    private var live: PleosSignalSource? = null
    private var familyMode = false
    private var familySuggested = false
    // family state follows the toggle and profile edits immediately, also mid-trip
    private var family: FamilyMonitor? = null
    private var taus: List<Pair<String, Double>> = emptyList()

    // per-trip state for the post-trip question and the summary card
    private var tripMaxDose = 0.0
    private var lastDose = 0.0
    private val reportedDose = mutableMapOf<String, Double>()
    private var tripRan = false
    private var lastFreshAt = Double.NEGATIVE_INFINITY
    private var cueCount = 0
    private var airCount = 0
    private var peakLevel = 0f
    private var moved = false
    private var stillSince: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        params = Params.fromJson(assets.open("params.json").bufferedReader().readText())
        settings = Settings(this)
        profiles = ProfileStore(this)
        theme = MoodThemes.byId(settings.themeId)
        home = HomeView(this, this)
        val note: (String) -> Unit = { msg -> runOnUiThread { home.note(msg) } }
        voice = Voice(this, note).also { it.start() }
        vehicle = Vehicle(this).also { runCatching { it.initialize() } }
        hvac = HvacController(vehicle, note)
        lightCue = LightCue(vehicle, note).also { it.start() }
        navi = NaviGuide(this, note).also { it.start() }

        panel = SettingsPanel(this, settings, profiles, params,
            onThemeChanged = { t -> theme = t; home.setTheme(t) },
            onChanged = { rebuildFamily(); applySettings() },
            onClose = { closeSettings() })
        setContentView(FrameLayout(this).apply {
            addView(home, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(panel, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        })
        home.setTheme(theme)
        applySettings()
        if (settings.onboarded) panel.visibility = View.GONE else onSettingsShown(first = true)
        watchDrivingRestrictions()
        onLive()   // follow the car from launch; demo replays can replace this

        // debug/QA: adb shell am start -n ai.motioncare.app/.MainActivity --es route_to "37.3947,127.1112,판교역"
        if (BuildConfig.DEBUG) intent.getStringExtra("route_to")?.split(",")?.takeIf { it.size == 3 }?.let { (lat, lon, name) ->
            home.postDelayed({ navi.routeTo(lat.toDouble(), lon.toDouble(), name) }, 3000)
        }
        // debug/QA: --es theme toon opens with that theme; --ez skip_welcome true skips the welcome
        if (BuildConfig.DEBUG) {
            intent.getStringExtra("theme")?.let { id -> settings.themeId = id; theme = MoodThemes.byId(id); home.setTheme(theme) }
            if (intent.getBooleanExtra("skip_welcome", false)) closeSettings()
        }
    }

    /** Locks theme/profile editing while the car says setup or typing is not allowed (moving). */
    private fun watchDrivingRestrictions() {
        runCatching {
            val c = Car.createCar(this)
            car = c
            val mgr = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            val apply = { r: CarUxRestrictions? ->
                val blocked = r != null && (r.activeRestrictions and
                    (CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP or CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD)) != 0
                runOnUiThread { panel.setRestricted(blocked) }
            }
            mgr.registerListener { r -> apply(r) }
            apply(mgr.currentCarUxRestrictions)
        }.onFailure { Log.w(TAG, "ux restrictions", it) }
    }

    private fun applySettings() {
        home.setDemoPanel(settings.demoPanelOn)
        home.setFamily(familyMode, profiles.load().map { it.name })
    }

    /** Family monitor and per-child thresholds from the current profiles and toggle. */
    private fun rebuildFamily() {
        val kids = if (familyMode) profiles.load() else emptyList()
        family = if (familyMode && kids.isNotEmpty()) FamilyMonitor(params, kids) else null
        taus = kids.map { it.name to it.threshold(params) }
    }

    override fun onSettings() = onSettingsShown(first = false)

    private fun onSettingsShown(first: Boolean) {
        panel.showWelcome(first)
        panel.visibility = View.VISIBLE
        home.visibility = View.INVISIBLE   // stops the animated icon and graph from drawing underneath
    }

    private fun closeSettings() {
        settings.onboarded = true
        panel.visibility = View.GONE
        home.visibility = View.VISIBLE
        applySettings()
    }

    @Deprecated("Activity back handling")
    override fun onBackPressed() {
        if (panel.visibility == View.VISIBLE) closeSettings() else @Suppress("DEPRECATION") super.onBackPressed()
    }

    private fun say(text: String) { if (settings.voiceOn) voice.say(text) }

    /** What the car says for an action; turn cues use the theme's voice when it has one. */
    private fun spoken(a: Action) = when (a) {
        Action.CUE_LEFT -> theme.cueLine(true)
        Action.CUE_RIGHT -> theme.cueLine(false)
        else -> null
    } ?: Voice.phrase(a)

    override fun onFamilyToggle() = toggleFamily()

    private fun toggleFamily() {
        familyMode = !familyMode
        rebuildFamily()
        applySettings()
        home.note(if (familyMode) "가족 모드: 가장 약한 탑승자 기준으로 미리 대응" else "가족 모드 끔")
        if (familyMode && live != null) navi.preferComfortableRoute()
    }

    override fun onScenario(file: String) {
        stopSource()
        val samples = ScenarioCsv.parse(assets.open(file).bufferedReader().readText())
        val cabin = SimulatedCabin(params.cabinT0)
        if (file.startsWith("D_") && !familyMode) toggleFamily()
        run(ReplaySignalSource(samples, speedup = 4, cabin = cabin), cabin)
        home.note("재생 시작: $file (4배속)")
    }

    override fun onLive() {
        // location, vehicle speed and microphone are runtime ("dangerous") permissions on AAOS;
        // start only once they are granted, otherwise the listeners would stay silent
        val needed = arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION, "android.car.permission.CAR_SPEED", android.Manifest.permission.RECORD_AUDIO)
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) {
            requestPermissions(needed.toTypedArray(), PERMISSIONS_REQUEST)
            return
        }
        stopSource()
        val src = PleosSignalSource(this, vehicle, { navi.nextTurn }) { suggestFamilyMode() }
        live = src
        run(src, cabin = null)
        home.note("실차 신호 수신 시작")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSIONS_REQUEST) return
        // speed/location are required; the microphone is optional (answers fall back to buttons)
        val required = setOf(android.Manifest.permission.ACCESS_FINE_LOCATION, "android.car.permission.CAR_SPEED")
        val missing = permissions.indices.any { permissions[it] in required && grantResults[it] != PackageManager.PERMISSION_GRANTED }
        if (missing) home.showAction("속도·위치 권한이 있어야 차 신호를 읽을 수 있어요") else if (source == null) onLive()
    }

    override fun onStopTrip() = stopTrip()

    private fun suggestFamilyMode() {
        if (familyMode || familySuggested) return
        familySuggested = true
        askYesNo("뒷좌석에 탑승자가 있어요. 가족 모드를 켤까요?") { if (!familyMode) toggleFamily() }
    }

    /**
     * Asks by voice and shows large answer buttons in the home screen at the same time; whichever
     * answers first wins. No system dialog while driving.
     */
    private fun askYesNo(question: String, onYes: () -> Unit) {
        var answered = false
        val yes = { if (!answered) { answered = true; home.hidePrompt(); onYes() } }
        home.prompt(question, listOf("응" to { home.note("답: 응 (화면)"); yes() }, "아니" to { answered = true; home.note("답: 아니 (화면)") }))
        if (settings.voiceOn) voice.ask(question, noVoice = {}) { answer ->
            if (Voice.isYes(answer)) yes() else { answered = true; home.hidePrompt() }
        }
    }

    private fun run(src: SignalSource, cabin: SimulatedCabin?) {
        val engine = Engine(params)
        rebuildFamily()
        home.dashboard.reset()
        home.resetTrip()
        startTripState()
        source = src
        src.start { sample ->
            val out = engine.step(sample)
            lastDose = out.dose
            tripMaxDose = maxOf(tripMaxDose, out.dose)
            home.dashboard.add(sample, out)
            home.update(sample, out, sample.nextTurnM)
            if (out.thetaHigh > 0) peakLevel = maxOf(peakLevel, (out.index / out.thetaHigh).toFloat())
            if (taus.isNotEmpty()) home.setFamilyProbabilities(taus.map { (n, tau) -> n to sicknessProbability(params, out.dose, tau, out.m) })
            val actions = out.actions.toMutableList()
            val fam = family
            fam?.step(out.dose, out.m)?.takeIf { it.alert }?.let {
                say("${fam.weakest}에게는 벌써 흔들림이 많아요. 바람 넣고 천천히 갈게요.")
                home.showAction("${fam.weakest} 기준으로 먼저 대응해요")
                home.note("t=${sample.t.toInt()}s 가족 모드: ${fam.weakest} 기준선 도달 (확률 %.2f)".format(it.probability))
                if (Action.HVAC_FRESH !in actions) actions += Action.HVAC_FRESH
            }
            // the engine re-sends fresh air on every escalation; the cabin is already being aired, so don't repeat it
            if (Action.HVAC_FRESH in actions) {
                if (sample.t - lastFreshAt < params.ttsCooldownS) actions -= Action.HVAC_FRESH else lastFreshAt = sample.t
            }
            for (a in actions) act(a, sample, cabin)
            home.updateSummary((sample.t / 60).toInt(), cueCount, airCount, peakLevel)
            if (src === live) detectTripEnd(sample.t, sample.speed)
        }
    }

    private fun act(a: Action, sample: Sample, cabin: SimulatedCabin?) {
        when (a) {
            Action.SUGGEST_REST_STOP -> askYesNo(Voice.phrase(a)) { navi.addRestStop(live?.lat, live?.lon); home.showAction("휴게소를 경유지로 넣었어요") }
            else -> say(spoken(a))
        }
        if (a == Action.CUE_LEFT || a == Action.CUE_RIGHT) {
            cueCount++
            home.showAction(if (a == Action.CUE_LEFT) "곧 왼쪽으로 돌아요" else "곧 오른쪽으로 돌아요")
            if (settings.lightCueOn) lightCue.cue(left = a == Action.CUE_LEFT)
        }
        home.note("t=${sample.t.toInt()}s  ${spoken(a)}")
        if (a == Action.HVAC_FRESH) {
            if (settings.autoClimateOn) {
                airCount++
                cabin?.freshAir(sample)
                hvac.freshAir(params.cabinT0.toFloat()) { say("창문을 조금 열어 볼까요?") }
                home.showAction("바깥 공기로 바꾸고 22°C로 맞췄어요")
            } else {
                say("창문을 조금 열어 볼까요?")
                home.showAction("창문을 조금 열면 좋아요")
            }
        }
    }

    private fun startTripState() {
        tripMaxDose = 0.0
        lastDose = 0.0
        reportedDose.clear()
        tripRan = true
        lastFreshAt = Double.NEGATIVE_INFINITY
        cueCount = 0; airCount = 0; peakLevel = 0f
        moved = false; stillSince = null
    }

    /** Live mode has no stop button: after driving, a minute parked ends the trip. */
    private fun detectTripEnd(t: Double, speed: Double) {
        if (speed > 3.0) { moved = true; stillSince = null; return }
        if (!moved || speed > 0.5) return
        val since = stillSince ?: t.also { stillSince = it }
        if (t - since >= TRIP_END_STILL_S) {
            finishTrip()
            startTripState()
        }
    }

    /** "멀미 났어": records the moment for the child who feels sick (asks who when there are several). */
    override fun onReportSick() {
        val kids = profiles.load()
        when {
            kids.isEmpty() -> home.showAction("설정에서 아이를 먼저 등록해 주세요")
            kids.size == 1 -> recordSick(kids[0].name)
            else -> home.prompt("누가 멀미가 나요?", kids.map { k -> k.name to { recordSick(k.name) } })
        }
    }

    private fun recordSick(who: String) {
        // with almost no motion yet the dose says nothing about this child's threshold: don't learn from it
        if (lastDose < MIN_LEARN_DOSE) {
            home.showAction("아직 흔들림이 거의 없었어요. 창문을 열고 바람을 쐬어 주세요")
            home.note("멀미 기록 건너뜀: $who, 누적량 %.1f".format(lastDose))
            return
        }
        if (familyMode && tripRan) {
            reportedDose[who] = lastDose   // applied with the post-trip question
        } else {
            val list = profiles.load()
            profiles.save(list.map { p -> if (p.name == who) ProfileLearning.update(params, p, Rating.SLIGHT, lastDose, tripMaxDose) else p })
            rebuildFamily()
        }
        home.showAction("기록했어요. $who 기준을 맞출게요")
        home.note("멀미 기록: $who, 누적량 %.1f".format(lastDose))
    }

    /** Stops a replay or live trip; in family mode asks the single post-trip question per child. */
    private fun stopTrip() {
        stopSource()
        home.resetTrip()
        finishTrip()
    }

    private fun finishTrip() {
        val wasRunning = tripRan
        tripRan = false
        if (!familyMode || !wasRunning) return
        askNext(profiles.load(), 0, mutableListOf(), tripMaxDose, reportedDose.toMap())
    }

    private fun askNext(list: List<Profile>, i: Int, updated: MutableList<Profile>, maxDose: Double, reported: Map<String, Double>) {
        if (i >= list.size) {
            profiles.save(updated)
            rebuildFamily()
            home.note("기준선 갱신: " + updated.joinToString { "${it.name} %.1f".format(it.threshold(params)) })
            home.showAction("고마워요. 아이별 기준을 맞췄어요")
            return
        }
        val p = list[i]
        val answer = { r: Rating ->
            updated += ProfileLearning.update(params, p, r, reported[p.name], maxDose)
            askNext(list, i + 1, updated, maxDose, reported)
        }
        home.prompt("오늘 ${p.name} 괜찮았어요?", listOf(
            "괜찮음" to { answer(Rating.FINE) },
            "살짝" to { answer(Rating.SLIGHT) },
            "심함" to { answer(Rating.BAD) },
        ))
    }

    private fun stopSource() {
        source?.stop()
        source = null
        live = null
    }

    override fun onDestroy() {
        stopSource()
        voice.release()
        navi.release()
        runCatching { vehicle.release() }
        runCatching { car?.disconnect() }
        super.onDestroy()
    }
}
