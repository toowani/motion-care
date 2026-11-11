package ai.motioncare.app

import ai.motioncare.app.theme.Mood
import ai.motioncare.app.theme.MoodThemes
import ai.motioncare.core.Action
import ai.motioncare.core.Engine
import ai.motioncare.core.FamilyMonitor
import ai.motioncare.core.Output
import ai.motioncare.core.Params
import ai.motioncare.core.Profile
import ai.motioncare.core.ProfileLearning
import ai.motioncare.core.Rating
import ai.motioncare.core.Sample
import ai.motioncare.core.ScenarioCsv
import ai.motioncare.core.sicknessProbability
import ai.pleos.playground.vehicle.Vehicle
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log

private const val TAG = "MotionCare"
/** A live trip ends after the car has been still this long; then the post-trip question is asked. */
private const val TRIP_END_STILL_S = 60.0
/** Below this cumulative dose a "멀미 났어" report is not used to learn a child's threshold. */
private const val MIN_LEARN_DOSE = 5.0
private const val HISTORY = 900
/** One 멀미 완화 press per this long; the car is still opening windows and changing the air. */
private const val RELIEF_DEBOUNCE_MS = 20_000L
/** On-screen (or card) line for the driver advice; the voice says it in full ([Voice.phrase]). */
private const val DRIVER_LINE = "브레이크 부드럽게"
/**
 * Family mode and its prompts are parked (team decision): no 가족 모드 button, no rear-seat
 * question, no post-trip question. The logic stays; set true to bring the prompts back with the UI
 * in [ai.motioncare.app.later].
 */
private const val FAMILY_UI = false

/**
 * Runs the trip in the background so the car keeps getting fresh air and the 멀미 완화 help while
 * the navigation is in front. The engine still plans turn cues; the app no longer plays them.
 * The screen ([MainActivity]) attaches a [TripUi] when visible; without
 * one, questions and events go to heads-up cards over the navigation ([Notifier]).
 */
class TripService : Service() {
    /** What the home screen shows; all calls arrive on the main thread. */
    interface TripUi {
        fun onReset()
        fun onSample(s: Sample, o: Output, risk: Risk)
        fun onFamily(on: Boolean, names: List<String>)
        fun onFamilyProbabilities(values: List<Pair<String, Double>>)
        fun onAction(msg: String)
        fun onNote(msg: String)
        fun onPrompt(question: String, answers: List<String>, answer: (Int) -> Unit)
        fun onHidePrompt()
        /** [reliefs]: 멀미 완화 presses; [peak]: the strongest shake the face showed this trip. */
        fun onSummary(minutes: Int, reliefs: Int, airs: Int, peak: Mood)
        /** Replay scenario name (null in live mode) and metres along its route per second of the CSV. */
        fun onRoute(name: String?, track: DoubleArray?) {}
    }

    /**
     * Sickness probability (formula 6) of the passenger who matters: the most sensitive child in
     * family mode, otherwise the passenger's own learnt threshold (tau0 until 멀미 완화 is pressed). [children] has one value per child.
     */
    data class Risk(val p: Double, val children: List<Pair<String, Double>> = emptyList())

    inner class LocalBinder : Binder() { val service get() = this@TripService }

    private val binder = LocalBinder()
    private lateinit var params: Params
    private lateinit var settings: Settings
    private lateinit var profiles: ProfileStore
    private lateinit var notifier: Notifier
    private lateinit var voice: Voice
    private lateinit var vehicle: Vehicle
    private lateinit var hvac: HvacController
    private lateinit var window: WindowController
    private lateinit var navi: NaviGuide
    private var ui: TripUi? = null
    /** False when the service could not become a foreground service; the screen then unbinds. */
    var ready = false
        private set

    private var source: SignalSource? = null
    private var live: PleosSignalSource? = null
    var familyMode = false
        private set
    private var familySuggested = false
    private var family: FamilyMonitor? = null
    private var taus: List<Pair<String, Double>> = emptyList()
    /** Threshold of the single default passenger (see [ProfileStore.loadSelf]). */
    private var selfTau = 0.0

    // per-trip state
    private var tripMaxDose = 0.0
    private var lastDose = 0.0
    private val reportedDose = mutableMapOf<String, Double>()
    private var tripRan = false
    private var lastFreshAt = Double.NEGATIVE_INFINITY
    private var reliefCount = 0
    private var lastReliefAt = Long.MIN_VALUE / 2
    private var learnedThisTrip = false
    private var airCount = 0
    private var peakMood = Mood.CALM
    /** Seconds of this trip spent moving (live) or played (replay); a parked car does not count. */
    private var driveS = 0.0
    private var moved = false
    private var stillSince: Double? = null
    private var mood = Mood.CALM

    // what an attaching screen needs to catch up
    private val history = ArrayDeque<Triple<Sample, Output, Risk>>()
    private val notes = ArrayDeque<String>()
    private var lastProbabilities: List<Pair<String, Double>> = emptyList()
    private var lastSummary = arrayOf(0, 0, 0)

    /** The finished trip, kept on screen while its post-trip questions are open. */
    private class Snapshot(val last: Triple<Sample, Output, Risk>?, val summary: Array<Int>, val peak: Mood, val kids: List<Pair<String, Double>>)
    private var held: Snapshot? = null
    /** Set when the family alert fires, so the fresh-air line that follows names the child. */
    private var familyFirst: String? = null
    private var pending: Pending? = null
    /** Recreated per trip: its dose only grows, so a new live trip must not inherit the last one's. */
    private lateinit var engine: Engine
    private val queued = ArrayDeque<Pending>()
    private var promptSeq = 0
    private var routeName: String? = null
    private var routeTrack: DoubleArray? = null

    /** Live navigation guidance for the map screen (null in replay or without a route). */
    val naviGuidance: NaviGuide.Guidance? get() = if (live != null && ::navi.isInitialized) navi.guidance else null

    private enum class Kind { TRIP, POST_TRIP }

    /** [voice]: asked aloud when the question shows (not while it waits in the queue). */
    private class Pending(
        val id: Int, val kind: Kind, val question: String, val answers: List<String>, val voice: String?,
        val onExpire: () -> Unit, val onAnswer: (Int) -> Unit,
    )

    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    /** A question shown as a card expires with the card; the screen keeps it until answered. */
    private val expire = Runnable { pending?.let { p -> if (ui == null) { Log.i(TAG, "question expired: ${p.question}"); dismiss(p); p.onExpire(); showNext() } } }
    private var lastSample: Sample? = null
    private var cabinNow: SimulatedCabin? = null

    override fun onCreate() {
        super.onCreate()
        params = Params.fromJson(assets.open("params.json").bufferedReader().readText())
        settings = Settings(this)
        profiles = ProfileStore(this)
        notifier = Notifier(this)
        val ok = runCatching {
            val n = notifier.ongoing("주행을 지켜보고 있어요")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(Notifier.ID_ONGOING, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            else startForeground(Notifier.ID_ONGOING, n)
        }.onFailure { Log.w(TAG, "foreground", it) }.isSuccess
        if (!ok) { stopSelf(); return }
        voice = Voice(this, ::note).also { it.start() }
        vehicle = Vehicle(this).also { runCatching { it.initialize() } }
        hvac = HvacController(vehicle, ::note)
        window = WindowController(vehicle, ::note)
        navi = NaviGuide(this, ::note).also { it.start() }
        ready = true
        startLive()   // follow the car from the start; demo replays can replace this
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_ANSWER) {
            val p = pending
            val id = intent.getIntExtra(EXTRA_PROMPT, -1)
            if (p != null && p.id == id) answer(p, intent.getIntExtra(EXTRA_ANSWER, -1))
            else Log.i(TAG, "late answer for prompt $id ignored")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        stopSource()
        if (ready) {
            voice.release()
            navi.release()
            runCatching { vehicle.release() }
        }
        if (::notifier.isInitialized) notifier.cancelPrompt()
        super.onDestroy()
    }

    // ---- screen ----

    /** Called when the home screen becomes visible: replays the trip so far and takes over prompts. */
    fun attach(screen: TripUi) {
        ui = screen
        resync(screen)
        notes.reversed().forEach { screen.onNote(it) }
        pending?.let { p -> main.removeCallbacks(expire); notifier.cancelPrompt(); screen.onPrompt(p.question, p.answers) { i -> answer(p, i) } }
    }

    /** Brings [screen] up to date: the held trip while post-trip questions are open, otherwise the live one. */
    private fun resync(screen: TripUi) {
        screen.onReset()
        screen.onFamily(familyMode, profiles.load().map { it.name })
        val h = held
        if (h != null) {
            h.last?.let { (s, o, r) -> screen.onSample(s, o, r) }
            if (h.kids.isNotEmpty()) screen.onFamilyProbabilities(h.kids)
            screen.onSummary(h.summary[0], h.summary[1], h.summary[2], h.peak)
            return
        }
        screen.onRoute(routeName, routeTrack)
        history.forEach { (s, o, r) -> screen.onSample(s, o, r) }
        if (lastProbabilities.isNotEmpty()) screen.onFamilyProbabilities(lastProbabilities)
        screen.onSummary(lastSummary[0], lastSummary[1], lastSummary[2], peakMood)
    }

    /** The post-trip questions are done (or expired): the screen follows the live trip again. */
    private fun releaseHeld() {
        if (held == null) return
        held = null
        // resync() resets the screen, which hides the prompt: an open question comes back
        ui?.let { s -> resync(s); pending?.let { p -> s.onPrompt(p.question, p.answers) { i -> answer(p, i) } } }
    }

    fun detach(screen: TripUi) {
        if (ui !== screen) return
        ui = null
        // an unanswered question on screen follows the driver to the navigation as a card
        pending?.let { p -> notifier.prompt(theme(), mood, p.question, p.answers, p.id); restartExpiry() }
    }

    private fun restartExpiry() {
        main.removeCallbacks(expire)
        main.postDelayed(expire, Notifier.PROMPT_TIMEOUT_MS)
    }

    private fun note(msg: String) {
        notes.addFirst(msg)
        while (notes.size > 6) notes.removeLast()
        ui?.onNote(msg) ?: Log.i(TAG, "note: $msg")
    }

    private fun theme() = MoodThemes.byId(settings.themeId)

    /** On screen: the action line. Away: a heads-up card over the navigation. */
    private fun action(msg: String, card: Boolean = true) {
        val screen = ui
        if (screen != null) screen.onAction(msg) else if (card) notifier.event(theme(), mood, "멀미 케어 · ${theme().label(mood)}", msg)
    }

    /** One question at a time; on screen as large buttons, otherwise as a card with answer buttons. */
    private fun ask(
        question: String, answers: List<String>, kind: Kind = Kind.TRIP, voiceText: String? = null,
        onExpire: () -> Unit = {}, onAnswer: (Int) -> Unit,
    ): Pending {
        val p = Pending(++promptSeq, kind, question, answers, voiceText, onExpire, onAnswer)
        // one question at a time: later ones wait (e.g. the post-trip chain behind a rear-seat question)
        if (pending != null) queued.addLast(p) else show(p)
        return p
    }

    private fun show(p: Pending) {
        pending = p
        val screen = ui
        if (screen != null) screen.onPrompt(p.question, p.answers) { i -> answer(p, i) }
        else { notifier.prompt(theme(), mood, p.question, p.answers, p.id); restartExpiry() }
        // unclear speech leaves the question waiting for a button
        if (p.voice != null && settings.voiceOn) voice.ask(p.voice, noVoice = {}) { heard ->
            when {
                Voice.isYes(heard) -> answer(p, 0)
                Voice.isNo(heard) -> answer(p, 1)
            }
        }
    }

    /** Takes [p] off the screen, the card and the microphone without answering it. */
    private fun dismiss(p: Pending) {
        if (pending !== p) return
        pending = null
        main.removeCallbacks(expire)
        notifier.cancelPrompt()
        ui?.onHidePrompt()
        voice.cancelAsk()
    }

    private fun answer(p: Pending, index: Int) {
        if (pending !== p || index !in p.answers.indices) return
        dismiss(p)
        p.onAnswer(index)
        showNext()
    }

    private fun showNext() { if (pending == null) queued.removeFirstOrNull()?.let { show(it) } }

    /** The trip is over or replaced: its questions (rear seat, who is sick) no longer apply. */
    private fun clearTripPrompts() {
        queued.removeAll { it.kind == Kind.TRIP }
        pending?.takeIf { it.kind == Kind.TRIP }?.let { dismiss(it) }
        showNext()
    }

    private fun say(text: String) { if (settings.voiceOn) voice.say(text) }

    /** Voice question plus buttons (screen or card); whichever answers first wins. */
    private fun askYesNo(question: String, onYes: () -> Unit) {
        ask(question, listOf("응", "아니"), voiceText = question) { i -> note(if (i == 0) "답: 응" else "답: 아니"); if (i == 0) onYes() }
    }

    // ---- controls from the screen ----

    fun onSettingsChanged() {
        rebuildFamily()
        ui?.onFamily(familyMode, profiles.load().map { it.name })
    }

    fun toggleFamily() {
        if (!ready) return
        familyMode = !familyMode
        rebuildFamily()
        ui?.onFamily(familyMode, profiles.load().map { it.name })
        note(if (familyMode) "가족 모드: 가장 약한 탑승자 기준으로 미리 대응" else "가족 모드 끔")
        if (familyMode && live != null) navi.preferComfortableRoute()
    }

    /**
     * Replays a scenario. [fromS] skips ahead (the engine still runs silently over the skipped part,
     * so dose and probability match a full run); null uses the scenario's demo start.
     */
    fun startScenario(file: String, fromS: Double? = null, speed: Float = 4f) {
        if (!ready) return
        val name = file.removeSuffix(".csv")
        val csv = "$name.csv"
        stopSource()
        clearTripPrompts()
        val text = assets.open(csv).bufferedReader().readText()
        val samples = ScenarioCsv.parse(text)
        val from = (fromS ?: DEMO_START_S[name] ?: 0.0).coerceIn(0.0, samples.lastOrNull()?.t ?: 0.0)
        routeName = name
        // the map indexes the track by playback second, so it starts where playback starts
        routeTrack = RouteTrack.fromCsv(text)?.let { tr -> tr.copyOfRange(from.toInt().coerceAtMost(tr.size - 1), tr.size) }
        val cabin = SimulatedCabin(params.cabinT0)
        if (FAMILY_UI && name.startsWith("D_") && !familyMode) toggleFamily()
        // a new demo replaces the held trip on screen; its post-trip question (if any) stays
        held = null
        val src = ReplaySignalSource(samples, speed, cabin, from)
        run(src, cabin, src.warmup)
        note("재생 시작: $name" + (if (from > 0) " ${from.toInt()}초부터" else "") + " (${"%.0f".format(speed)}배속)")
    }

    fun startLive() {
        if (!ready) return
        stopSource()
        clearTripPrompts()
        val src = PleosSignalSource(this, vehicle, { navi.nextTurn }) { occupied -> onRearSeats(occupied) }
        routeName = null
        routeTrack = null
        live = src
        run(src, cabin = null)
        note("실차 신호 수신 시작")
    }

    /** Ends the demo trip (post-trip question in family mode) and goes back to following the car. */
    fun stopTrip() {
        if (!ready) return
        stopSource()
        clearTripPrompts()
        finishTrip()
        startLive()
    }

    /** "멀미 났어": records the moment for the child who feels sick (asks who when there are several). */
    fun reportSick() {
        if (!ready) return
        val kids = profiles.load()
        when {
            kids.isEmpty() -> action("설정에서 아이를 먼저 등록해 주세요", card = false)
            kids.size == 1 -> recordSick(kids[0].name)
            else -> ask("누가 멀미가 나요?", kids.map { it.name }) { i -> recordSick(kids[i].name) }
        }
    }

    /**
     * "멀미 완화", at once: the rear windows open a little (or the passenger is asked to open one),
     * fresh air and a cooler set point, one short spoken line. The first press of a trip also records
     * this dose as the passenger's own feedback point. The rest-stop offer that followed is parked in
     * [ai.motioncare.app.later.RestStopOffer].
     */
    fun relief() {
        if (!ready) return
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - lastReliefAt < RELIEF_DEBOUNCE_MS) { action("환기 중이에요", card = false); return }
        lastReliefAt = nowMs
        reliefCount++
        lastSummary = arrayOf(lastSummary[0], reliefCount, airCount)
        ui?.onSummary(lastSummary[0], reliefCount, airCount, peakMood)
        action("창문 열고 환기해요", card = false)
        note("멀미 완화 ${reliefCount}번째")
        learnFromRelief()
        val trip = source
        window.openRearALittle(onDone = {
            say("창문 열고 환기할게요. 먼 곳을 보세요.")
            if (source === trip) action("창문 열고 환기했어요", card = false)
        }, onDenied = {
            say("환기할게요. 창문을 조금 열어 주세요. 먼 곳을 보세요.")
            if (source === trip) action("창문을 조금 열어 주세요", card = false)
        })
        if (settings.autoClimateOn) {
            hvac.freshAir(params.cabinT0.toFloat(), remember = trip != null && trip !== live, onDone = {
                if (source === trip) {
                    airCount++
                    lastSample?.let { s -> lastFreshAt = s.t; cabinNow?.freshAir(s) }
                    lastSummary = arrayOf(lastSummary[0], reliefCount, airCount)
                    ui?.onSummary(lastSummary[0], reliefCount, airCount, peakMood)
                }
            }, onDenied = { msg -> Log.i(TAG, "relief: climate not changed ($msg)") })
        }
    }

    /** One feedback point per trip: the dose at the first press, unless almost nothing has moved yet. */
    private fun learnFromRelief() {
        if (learnedThisTrip) return
        if (lastDose < MIN_LEARN_DOSE) { Log.i(TAG, "relief: dose %.1f too low to learn from".format(lastDose)); return }
        learnedThisTrip = true
        val me = ProfileLearning.update(params, profiles.loadSelf(), Rating.SLIGHT, lastDose, tripMaxDose)
        profiles.saveSelf(me)
        rebuildFamily()
        note("완화 기록: 누적량 %.1f · 기준 %.1f".format(lastDose, me.threshold(params)))
    }

    /** QA: requests a route so NaviHelper delivers next turns. */
    fun routeTo(lat: Double, lon: Double, name: String) { if (ready) navi.routeTo(lat, lon, name) }

    /** Screen region our window occupies, for the Pleos navigation (empty Rect clears it). */
    fun reportOccupiedRegion(rect: android.graphics.Rect) { if (ready) navi.updateOccupiedRegion(rect) }

    // ---- trip ----

    private fun rebuildFamily() {
        val kids = if (familyMode) profiles.load() else emptyList()
        family = if (familyMode && kids.isNotEmpty()) FamilyMonitor(params, kids) else null
        taus = kids.map { it.name to it.threshold(params) }
        selfTau = profiles.loadSelf().threshold(params)
    }

    /** Asked once per service; only someone leaving the back and someone (new) sitting down asks again. */
    private var rearLeft = false

    private fun onRearSeats(occupied: Boolean) {
        Log.i(TAG, "rear seats occupied=$occupied")
        if (!occupied) { if (familySuggested) rearLeft = true; return }
        if (rearLeft) { familySuggested = false; rearLeft = false }
        if (FAMILY_UI) suggestFamilyMode()
    }

    private fun suggestFamilyMode() {
        if (familyMode || familySuggested) return
        familySuggested = true
        askYesNo("뒷좌석에 탑승자가 있어요. 가족 모드를 켤까요?") { if (!familyMode) toggleFamily() }
    }

    private fun run(src: SignalSource, cabin: SimulatedCabin?, warmup: List<Sample> = emptyList()) {
        engine = Engine(params)
        rebuildFamily()
        history.clear()
        cabinNow = cabin
        lastSample = null
        if (held == null) {
            ui?.onReset()
            // a waiting question survives the screen reset
            pending?.let { p -> ui?.onPrompt(p.question, p.answers) { i -> answer(p, i) } }
        }
        // the demo bar shows the new source even while a finished trip is held on screen
        ui?.onRoute(routeName, routeTrack)
        startTripState()
        warmUp(warmup, cabin)
        source = src
        src.start { sample ->
            lastSample = sample
            val out = engine.step(sample)
            lastDose = out.dose
            tripMaxDose = maxOf(tripMaxDose, out.dose)
            // probabilities are sanitised once here, so no screen ever sees NaN or more than 100 %
            val kids = taus.map { (n, tau) -> n to safeP(sicknessProbability(params, out.dose, tau, out.m)) }
            // the weakest child has the lowest tau, hence the highest probability
            val risk = Risk(kids.maxOfOrNull { it.second } ?: safeP(sicknessProbability(params, out.dose, selfTau, out.m)), kids)
            mood = Mood.shown(out.state, risk.p)
            peakMood = maxOf(peakMood, mood)
            // driving again: the finished trip gives the screen back to the live one (its questions stay open)
            if (held != null && src === live && sample.speed > 3.0) releaseHeld()
            history.addLast(Triple(sample, out, risk))
            while (history.size > HISTORY) history.removeFirst()
            val screen = ui.takeIf { held == null }
            screen?.onSample(sample, out, risk)
            if (kids.isNotEmpty()) {
                lastProbabilities = kids
                screen?.onFamilyProbabilities(lastProbabilities)
            }
            val actions = out.actions.toMutableList()
            val fam = family
            fam?.step(out.dose, out.m)?.takeIf { it.alert }?.let {
                say("${fam.weakest}에게는 벌써 흔들림이 많아요. 바람 넣고 천천히 갈게요.")
                action("${fam.weakest} 기준으로 먼저 대응해요")
                familyFirst = fam.weakest
                note("t=${sample.t.toInt()}s 가족 모드: ${fam.weakest} 기준선 도달 (확률 %.2f)".format(it.probability))
                if (Action.HVAC_FRESH !in actions) actions += Action.HVAC_FRESH
            }
            // the engine re-sends fresh air on every escalation; the cabin is already being aired, so don't repeat it
            if (Action.HVAC_FRESH in actions) {
                if (sample.t - lastFreshAt < params.ttsCooldownS) actions -= Action.HVAC_FRESH else lastFreshAt = sample.t
            }
            for (a in actions) act(a, sample, cabin)
            // live: only time on the move counts (a parked car with the app open is not a 47-minute drive)
            if (src !== live || sample.speed > 3.0 / 3.6) driveS += 1.0
            lastSummary = arrayOf((driveS / 60).toInt(), reliefCount, airCount)
            screen?.onSummary(lastSummary[0], reliefCount, airCount, peakMood)
            if (src === live) detectTripEnd(sample.t, sample.speed)
        }
        notifier.updateOngoing(if (src === live) "주행을 지켜보고 있어요" else "시연 시나리오 재생 중")
    }

    /**
     * Turn cues (CUE_LEFT/RIGHT) are hidden from the app by team decision: the engine still plans
     * them (and the sim checks them), but no tone, voice, light, bubble or count follows. The parked
     * players are in [ai.motioncare.app.later]. The rest-stop question (SUGGEST_REST_STOP) is parked
     * the same way ([ai.motioncare.app.later.RestStopOffer]).
     */
    private fun act(a: Action, sample: Sample, cabin: SimulatedCabin?) {
        if (a == Action.CUE_LEFT || a == Action.CUE_RIGHT) { Log.i(TAG, "t=${sample.t.toInt()}s engine cue $a (not played)"); return }
        if (a == Action.SUGGEST_REST_STOP) { Log.i(TAG, "t=${sample.t.toInt()}s engine rest stop (not offered)"); return }
        say(Voice.phrase(a))
        note("t=${sample.t.toInt()}s  ${Voice.phrase(a)}")
        if (a == Action.TTS_DRIVER) action(DRIVER_LINE)
        if (a == Action.HVAC_FRESH) {
            if (settings.autoClimateOn) {
                // counted and announced only once the car confirms; with the climate off, ask for a window
                val trip = source
                hvac.freshAir(params.cabinT0.toFloat(), remember = cabin != null, onDone = {
                    if (source === trip) {
                        airCount++
                        cabin?.freshAir(sample)
                        // the family line came a moment earlier; keep its point instead of overwriting it
                        val who = familyFirst.also { familyFirst = null }
                        action(if (who != null) "$who 먼저 · 바깥 공기 %.0f°C".format(params.cabinT0) else "바깥 공기로 바꾸고 %.0f°C로 맞췄어요".format(params.cabinT0))
                    }
                }, onDenied = { msg -> if (source === trip) { say("창문을 조금 열어 볼까요?"); action(msg) } })
            } else {
                say("창문을 조금 열어 볼까요?")
                action("창문을 조금 열면 좋아요")
            }
        }
    }

    /** Runs the engine over skipped replay samples: no screen, voice, cards or car actions. */
    private fun warmUp(samples: List<Sample>, cabin: SimulatedCabin?) {
        val fam = family
        for (raw in samples) {
            val sample = cabin?.apply(raw) ?: raw
            val out = engine.step(sample)
            lastDose = out.dose
            tripMaxDose = maxOf(tripMaxDose, out.dose)
            val alert = fam?.step(out.dose, out.m)?.alert == true
            if (Action.HVAC_FRESH in out.actions || alert) {
                if (sample.t - lastFreshAt >= params.ttsCooldownS) { lastFreshAt = sample.t; cabin?.freshAir(sample) }
            }
        }
        if (samples.isNotEmpty()) Log.i(TAG, "warm-up: ${samples.size} samples, dose %.1f".format(lastDose))
    }

    private fun startTripState() {
        tripMaxDose = 0.0
        lastDose = 0.0
        reportedDose.clear()
        tripRan = true
        lastFreshAt = Double.NEGATIVE_INFINITY
        reliefCount = 0; airCount = 0; peakMood = Mood.CALM; driveS = 0.0
        learnedThisTrip = false
        moved = false; stillSince = null
        mood = Mood.CALM
        lastProbabilities = emptyList()
        lastSummary = arrayOf(0, 0, 0)
    }

    /** Live mode has no stop button: after driving, a minute parked ends the trip. */
    private fun detectTripEnd(t: Double, speed: Double) {
        if (speed > 3.0) { moved = true; stillSince = null; return }
        if (!moved || speed > 0.5) return
        val since = stillSince ?: t.also { stillSince = it }
        if (t - since >= TRIP_END_STILL_S) {
            clearTripPrompts()
            finishTrip()
            engine = Engine(params)
            rebuildFamily()
            history.clear()
            if (held == null) {
                ui?.onReset()
                ui?.onRoute(routeName, routeTrack)
                pending?.let { p -> ui?.onPrompt(p.question, p.answers) { i -> answer(p, i) } }
            }
            startTripState()
        }
    }

    private fun recordSick(who: String) {
        // with almost no motion yet the dose says nothing about this child's threshold: don't learn from it
        if (lastDose < MIN_LEARN_DOSE) {
            action("아직 흔들림이 거의 없었어요. 창문을 열고 바람을 쐬어 주세요", card = false)
            note("멀미 기록 건너뜀: $who, 누적량 %.1f".format(lastDose))
            return
        }
        if (familyMode && tripRan) {
            reportedDose[who] = lastDose   // applied with the post-trip question
        } else {
            val list = profiles.load()
            profiles.save(list.map { p -> if (p.name == who) ProfileLearning.update(params, p, Rating.SLIGHT, lastDose, tripMaxDose) else p })
            rebuildFamily()
        }
        action("기록했어요. $who 기준을 맞출게요", card = false)
        note("멀미 기록: $who, 누적량 %.1f".format(lastDose))
    }

    private fun finishTrip() {
        val wasRunning = tripRan
        tripRan = false
        if (!familyMode || !wasRunning) return
        // one post-trip chain at a time: a second would save over the first one's learning
        if (held != null || pending?.kind == Kind.POST_TRIP || queued.any { it.kind == Kind.POST_TRIP }) return
        held = Snapshot(history.lastOrNull(), lastSummary.copyOf(), peakMood, lastProbabilities)
        askNext(profiles.load(), 0, mutableListOf(), tripMaxDose, reportedDose.toMap())
    }

    private fun askNext(list: List<Profile>, i: Int, updated: MutableList<Profile>, maxDose: Double, reported: Map<String, Double>) {
        if (i >= list.size) {
            profiles.save(updated)
            rebuildFamily()
            note("기준선 갱신: " + updated.joinToString { "${it.name} %.1f".format(it.threshold(params)) })
            // release first: it resets the screen, which would wipe the thank-you line
            releaseHeld()
            action("고마워요. 아이별 기준을 맞췄어요", card = false)
            return
        }
        val p = list[i]
        ask("오늘 ${p.name} 괜찮았어요?", listOf("괜찮음", "살짝", "심함"), Kind.POST_TRIP, onExpire = { releaseHeld() }) { a ->
            val r = when (a) { 0 -> Rating.FINE; 1 -> Rating.SLIGHT; else -> Rating.BAD }
            updated += ProfileLearning.update(params, p, r, reported[p.name], maxDose)
            askNext(list, i + 1, updated, maxDose, reported)
        }
    }

    private fun safeP(p: Double) = if (p.isFinite()) p.coerceIn(0.0, 1.0) else 0.0

    private fun stopSource() {
        // a replay borrowed the driver's climate settings: give them back
        if (::hvac.isInitialized && source != null && source !== live) hvac.restore()
        source?.stop()
        source = null
        live = null
    }

    companion object {
        /** Demo buttons start where the scenario gets going (D_family idles for its first 14 minutes). */
        private val DEMO_START_S = mapOf("D_family" to 870.0)
        const val ACTION_ANSWER = "ai.motioncare.app.ANSWER"
        const val EXTRA_PROMPT = "prompt"
        const val EXTRA_ANSWER = "answer"
    }
}
