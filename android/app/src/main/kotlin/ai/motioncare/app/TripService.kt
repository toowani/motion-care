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
import android.util.Log

private const val TAG = "MotionCare"
/** A live trip ends after the car has been still this long; then the post-trip question is asked. */
private const val TRIP_END_STILL_S = 60.0
/** Below this cumulative dose a "멀미 났어" report is not used to learn a child's threshold. */
private const val MIN_LEARN_DOSE = 5.0
private const val HISTORY = 900

/**
 * Runs the trip in the background so the car keeps getting cues, fresh air and family care while
 * the navigation is in front. The screen ([MainActivity]) attaches a [TripUi] when visible; without
 * one, questions and events go to heads-up cards over the navigation ([Notifier]).
 */
class TripService : Service() {
    /** What the home screen shows; all calls arrive on the main thread. */
    interface TripUi {
        fun onReset()
        fun onSample(s: Sample, o: Output)
        fun onFamily(on: Boolean, names: List<String>)
        fun onFamilyProbabilities(values: List<Pair<String, Double>>)
        fun onAction(msg: String)
        fun onNote(msg: String)
        fun onPrompt(question: String, answers: List<String>, answer: (Int) -> Unit)
        fun onHidePrompt()
        fun onSummary(minutes: Int, cues: Int, airs: Int, peak: Float)
    }

    inner class LocalBinder : Binder() { val service get() = this@TripService }

    private val binder = LocalBinder()
    private lateinit var params: Params
    private lateinit var settings: Settings
    private lateinit var profiles: ProfileStore
    private lateinit var notifier: Notifier
    private lateinit var voice: Voice
    private lateinit var vehicle: Vehicle
    private lateinit var hvac: HvacController
    private lateinit var lightCue: LightCue
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

    // per-trip state
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
    private var mood = Mood.CALM

    // what an attaching screen needs to catch up
    private val history = ArrayDeque<Pair<Sample, Output>>()
    private val notes = ArrayDeque<String>()
    private var lastProbabilities: List<Pair<String, Double>> = emptyList()
    private var lastSummary = arrayOf(0, 0, 0)
    private var pending: Pending? = null
    private val queued = ArrayDeque<Pending>()
    private var promptSeq = 0

    private class Pending(val id: Int, val question: String, val answers: List<String>, val onAnswer: (Int) -> Unit)

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
        lightCue = LightCue(vehicle, ::note).also { it.start() }
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
        }
        return START_STICKY
    }

    override fun onDestroy() {
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
        screen.onReset()
        history.forEach { (s, o) -> screen.onSample(s, o) }
        notes.reversed().forEach { screen.onNote(it) }
        screen.onFamily(familyMode, profiles.load().map { it.name })
        if (lastProbabilities.isNotEmpty()) screen.onFamilyProbabilities(lastProbabilities)
        screen.onSummary(lastSummary[0], lastSummary[1], lastSummary[2], peakLevel)
        pending?.let { p -> notifier.cancelPrompt(); screen.onPrompt(p.question, p.answers) { i -> answer(p, i) } }
    }

    fun detach(screen: TripUi) {
        if (ui !== screen) return
        ui = null
        // an unanswered question on screen follows the driver to the navigation as a card
        pending?.let { p -> notifier.prompt(theme(), mood, p.question, p.answers, p.id) }
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
    private fun ask(question: String, answers: List<String>, onAnswer: (Int) -> Unit): Pending {
        val p = Pending(++promptSeq, question, answers, onAnswer)
        // one question at a time: later ones wait (e.g. the post-trip chain behind a rest-stop question)
        if (pending != null) queued.addLast(p) else show(p)
        return p
    }

    private fun show(p: Pending) {
        pending = p
        val screen = ui
        if (screen != null) screen.onPrompt(p.question, p.answers) { i -> answer(p, i) }
        else notifier.prompt(theme(), mood, p.question, p.answers, p.id)
    }

    private fun answer(p: Pending, index: Int) {
        if (pending !== p || index !in p.answers.indices) return
        pending = null
        notifier.cancelPrompt()
        ui?.onHidePrompt()
        p.onAnswer(index)
        if (pending == null) queued.removeFirstOrNull()?.let { show(it) }
    }

    private fun say(text: String) { if (settings.voiceOn) voice.say(text) }

    /** Voice question plus buttons (screen or card); whichever answers first wins. */
    private fun askYesNo(question: String, onYes: () -> Unit) {
        val p = ask(question, listOf("응", "아니")) { i -> note(if (i == 0) "답: 응" else "답: 아니"); if (i == 0) onYes() }
        // unclear speech leaves the question waiting for a button
        if (settings.voiceOn) voice.ask(question, noVoice = {}) { heard ->
            when {
                Voice.isYes(heard) -> answer(p, 0)
                Voice.isNo(heard) -> answer(p, 1)
            }
        }
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

    fun startScenario(file: String) {
        if (!ready) return
        stopSource()
        val samples = ScenarioCsv.parse(assets.open(file).bufferedReader().readText())
        val cabin = SimulatedCabin(params.cabinT0)
        if (file.startsWith("D_") && !familyMode) toggleFamily()
        run(ReplaySignalSource(samples, speedup = 4, cabin = cabin), cabin)
        note("재생 시작: $file (4배속)")
    }

    fun startLive() {
        if (!ready) return
        stopSource()
        val src = PleosSignalSource(this, vehicle, { navi.nextTurn }) { suggestFamilyMode() }
        live = src
        run(src, cabin = null)
        note("실차 신호 수신 시작")
    }

    /** Ends the demo trip (post-trip question in family mode) and goes back to following the car. */
    fun stopTrip() {
        if (!ready) return
        stopSource()
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

    /** QA: requests a route so NaviHelper delivers next turns. */
    fun routeTo(lat: Double, lon: Double, name: String) { if (ready) navi.routeTo(lat, lon, name) }

    // ---- trip ----

    private fun rebuildFamily() {
        val kids = if (familyMode) profiles.load() else emptyList()
        family = if (familyMode && kids.isNotEmpty()) FamilyMonitor(params, kids) else null
        taus = kids.map { it.name to it.threshold(params) }
    }

    private fun suggestFamilyMode() {
        if (familyMode || familySuggested) return
        familySuggested = true
        askYesNo("뒷좌석에 탑승자가 있어요. 가족 모드를 켤까요?") { if (!familyMode) toggleFamily() }
    }

    private fun run(src: SignalSource, cabin: SimulatedCabin?) {
        val engine = Engine(params)
        rebuildFamily()
        history.clear()
        ui?.onReset()
        // a waiting question (e.g. post-trip) survives the screen reset
        pending?.let { p -> ui?.onPrompt(p.question, p.answers) { i -> answer(p, i) } }
        startTripState()
        source = src
        src.start { sample ->
            val out = engine.step(sample)
            lastDose = out.dose
            tripMaxDose = maxOf(tripMaxDose, out.dose)
            mood = Mood.of(out.state)
            history.addLast(sample to out)
            while (history.size > HISTORY) history.removeFirst()
            ui?.onSample(sample, out)
            if (out.thetaHigh > 0) peakLevel = maxOf(peakLevel, (out.index / out.thetaHigh).toFloat())
            if (taus.isNotEmpty()) {
                lastProbabilities = taus.map { (n, tau) -> n to sicknessProbability(params, out.dose, tau, out.m) }
                ui?.onFamilyProbabilities(lastProbabilities)
            }
            val actions = out.actions.toMutableList()
            val fam = family
            fam?.step(out.dose, out.m)?.takeIf { it.alert }?.let {
                say("${fam.weakest}에게는 벌써 흔들림이 많아요. 바람 넣고 천천히 갈게요.")
                action("${fam.weakest} 기준으로 먼저 대응해요")
                note("t=${sample.t.toInt()}s 가족 모드: ${fam.weakest} 기준선 도달 (확률 %.2f)".format(it.probability))
                if (Action.HVAC_FRESH !in actions) actions += Action.HVAC_FRESH
            }
            // the engine re-sends fresh air on every escalation; the cabin is already being aired, so don't repeat it
            if (Action.HVAC_FRESH in actions) {
                if (sample.t - lastFreshAt < params.ttsCooldownS) actions -= Action.HVAC_FRESH else lastFreshAt = sample.t
            }
            for (a in actions) act(a, sample, cabin)
            lastSummary = arrayOf((sample.t / 60).toInt(), cueCount, airCount)
            ui?.onSummary(lastSummary[0], cueCount, airCount, peakLevel)
            if (src === live) detectTripEnd(sample.t, sample.speed)
        }
        notifier.updateOngoing(if (src === live) "주행을 지켜보고 있어요" else "시연 시나리오 재생 중")
    }

    /** What the car says for an action; turn cues use the theme's voice when it has one. */
    private fun spoken(a: Action) = when (a) {
        Action.CUE_LEFT -> theme().cueLine(true)
        Action.CUE_RIGHT -> theme().cueLine(false)
        else -> null
    } ?: Voice.phrase(a)

    private fun act(a: Action, sample: Sample, cabin: SimulatedCabin?) {
        when (a) {
            Action.SUGGEST_REST_STOP -> askYesNo(Voice.phrase(a)) { navi.addRestStop(live?.lat, live?.lon); action("휴게소를 경유지로 넣었어요") }
            else -> say(spoken(a))
        }
        if (a == Action.CUE_LEFT || a == Action.CUE_RIGHT) {
            cueCount++
            action(spoken(a))
            if (settings.lightCueOn) lightCue.cue(left = a == Action.CUE_LEFT)
        }
        note("t=${sample.t.toInt()}s  ${spoken(a)}")
        if (a == Action.HVAC_FRESH) {
            if (settings.autoClimateOn) {
                airCount++
                cabin?.freshAir(sample)
                hvac.freshAir(params.cabinT0.toFloat()) { say("창문을 조금 열어 볼까요?") }
                action("바깥 공기로 바꾸고 22°C로 맞췄어요")
            } else {
                say("창문을 조금 열어 볼까요?")
                action("창문을 조금 열면 좋아요")
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
            finishTrip()
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
        askNext(profiles.load(), 0, mutableListOf(), tripMaxDose, reportedDose.toMap())
    }

    private fun askNext(list: List<Profile>, i: Int, updated: MutableList<Profile>, maxDose: Double, reported: Map<String, Double>) {
        if (i >= list.size) {
            profiles.save(updated)
            rebuildFamily()
            note("기준선 갱신: " + updated.joinToString { "${it.name} %.1f".format(it.threshold(params)) })
            action("고마워요. 아이별 기준을 맞췄어요", card = false)
            return
        }
        val p = list[i]
        ask("오늘 ${p.name} 괜찮았어요?", listOf("괜찮음", "살짝", "심함")) { a ->
            val r = when (a) { 0 -> Rating.FINE; 1 -> Rating.SLIGHT; else -> Rating.BAD }
            updated += ProfileLearning.update(params, p, r, reported[p.name], maxDose)
            askNext(list, i + 1, updated, maxDose, reported)
        }
    }

    private fun stopSource() {
        source?.stop()
        source = null
        live = null
    }

    companion object {
        const val ACTION_ANSWER = "ai.motioncare.app.ANSWER"
        const val EXTRA_PROMPT = "prompt"
        const val EXTRA_ANSWER = "answer"
    }
}
