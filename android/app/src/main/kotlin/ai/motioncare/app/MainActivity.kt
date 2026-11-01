package ai.motioncare.app

import ai.motioncare.core.Action
import ai.motioncare.core.Engine
import ai.motioncare.core.FamilyMonitor
import ai.motioncare.core.Params
import ai.motioncare.core.Profile
import ai.motioncare.core.ProfileLearning
import ai.motioncare.core.Rating
import ai.motioncare.core.ScenarioCsv
import ai.pleos.playground.vehicle.Vehicle
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout

/**
 * Demo shell: pick a simulated scenario (replay) or live Pleos signals; the engine decides and
 * the car acts — turn cues, fresh air, rest-stop waypoint — and in family mode it follows the
 * most sensitive passenger and learns from the post-trip question.
 */
class MainActivity : Activity() {
    private lateinit var params: Params
    private lateinit var dashboard: DashboardView
    private lateinit var voice: Voice
    private lateinit var vehicle: Vehicle
    private lateinit var hvac: HvacController
    private lateinit var navi: NaviGuide
    private lateinit var profiles: ProfileStore
    private lateinit var familyButton: Button

    private var source: SignalSource? = null
    private var live: PleosSignalSource? = null
    private var familyMode = false
    private var familySuggested = false

    // per-trip state for the post-trip question
    private var tripMaxDose = 0.0
    private var lastDose = 0.0
    private val reportedDose = mutableMapOf<String, Double>()
    private var tripRan = false
    private var lastFreshAt = Double.NEGATIVE_INFINITY

    private val scenarios = listOf(
        "B 굽은 길 · 거칠게" to "B_winding_rough.csv",
        "B 굽은 길 · 부드럽게" to "B_winding_smooth.csv",
        "A 도심 · 거칠게" to "A_city_rough.csv",
        "D 가족 귀성" to "D_family.csv",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        params = Params.fromJson(assets.open("params.json").bufferedReader().readText())
        dashboard = DashboardView(this)
        val note: (String) -> Unit = { msg -> runOnUiThread { dashboard.note(msg) } }
        voice = Voice(this, note).also { it.start() }
        vehicle = Vehicle(this).also { runCatching { it.initialize() } }
        hvac = HvacController(vehicle, note)
        navi = NaviGuide(this, note).also { it.start() }
        profiles = ProfileStore(this)

        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(16, 16, 16, 8) }
        scenarios.forEach { (label, file) -> bar.addView(button(label) { startReplay(file) }) }
        bar.addView(button("실차 신호 (Pleos)") { startLive() })
        familyButton = button(familyLabel()) { toggleFamily() }
        bar.addView(familyButton)
        bar.addView(button("멀미 났어") { reportSick() })
        bar.addView(button("정지") { stopTrip() })

        // debug/QA: adb shell am start -n ai.motioncare.app/.MainActivity --es route_to "37.3947,127.1112,판교역"
        if (BuildConfig.DEBUG) intent.getStringExtra("route_to")?.split(",")?.takeIf { it.size == 3 }?.let { (lat, lon, name) ->
            dashboard.postDelayed({ navi.routeTo(lat.toDouble(), lon.toDouble(), name) }, 3000)
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            // the Pleos home shows apps in a narrower side panel too, so the buttons must scroll rather than clip
            addView(HorizontalScrollView(this@MainActivity).apply { addView(bar) }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(dashboard, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        })
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun familyLabel() = if (familyMode) "가족 모드 켬 · " + profiles.load().joinToString { it.name } else "가족 모드 끔"

    private fun toggleFamily() {
        familyMode = !familyMode
        familyButton.text = familyLabel()
        dashboard.note(if (familyMode) "가족 모드: 가장 약한 탑승자 기준으로 미리 대응" else "가족 모드 끔")
        if (familyMode && live != null) navi.preferComfortableRoute()
    }

    private fun startReplay(file: String) {
        stopSource()
        val samples = ScenarioCsv.parse(assets.open(file).bufferedReader().readText())
        val cabin = SimulatedCabin(params.cabinT0)
        if (file.startsWith("D_") && !familyMode) toggleFamily()
        run(ReplaySignalSource(samples, speedup = 4, cabin = cabin), cabin)
        dashboard.note("재생 시작: $file (4배속)")
    }

    private fun startLive() {
        // location, vehicle speed and microphone are runtime ("dangerous") permissions on AAOS
        val needed = arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION, "android.car.permission.CAR_SPEED", android.Manifest.permission.RECORD_AUDIO)
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), 1)
        stopSource()
        val src = PleosSignalSource(this, vehicle, { navi.nextTurn }) { suggestFamilyMode() }
        live = src
        run(src, cabin = null)
        dashboard.note("실차 신호 수신 시작")
    }

    private fun suggestFamilyMode() {
        if (familyMode || familySuggested) return
        familySuggested = true
        askYesNo("뒷좌석에 탑승자가 있어요. 가족 모드를 켤까요?") { if (!familyMode) toggleFamily() }
    }

    /** Voice yes/no; falls back to on-screen buttons when speech recognition is unavailable. */
    private fun askYesNo(question: String, onYes: () -> Unit) {
        val noVoice = {
            AlertDialog.Builder(this)
                .setTitle(question)
                .setPositiveButton("응") { _, _ -> dashboard.note("답: 응 (화면)"); onYes() }
                .setNegativeButton("아니") { _, _ -> dashboard.note("답: 아니 (화면)") }
                .show()
            Unit
        }
        voice.ask(question, noVoice) { answer -> if (Voice.isYes(answer)) onYes() }
    }

    private fun run(src: SignalSource, cabin: SimulatedCabin?) {
        val engine = Engine(params)
        val family = if (familyMode) FamilyMonitor(params, profiles.load()) else null
        dashboard.reset()
        tripMaxDose = 0.0
        lastDose = 0.0
        reportedDose.clear()
        tripRan = true
        lastFreshAt = Double.NEGATIVE_INFINITY
        source = src
        src.start { sample ->
            val out = engine.step(sample)
            lastDose = out.dose
            tripMaxDose = maxOf(tripMaxDose, out.dose)
            dashboard.add(sample, out)
            val actions = out.actions.toMutableList()
            family?.step(out.dose, out.m)?.takeIf { it.alert }?.let {
                voice.say("${family.weakest}에게는 벌써 흔들림이 많아요. 바람 넣고 천천히 갈게요.")
                dashboard.note("t=${sample.t.toInt()}s 가족 모드: ${family.weakest} 기준선 도달 (확률 %.2f)".format(it.probability))
                if (Action.HVAC_FRESH !in actions) actions += Action.HVAC_FRESH
            }
            // the engine re-sends fresh air on every escalation; the cabin is already being aired, so don't repeat it
            if (Action.HVAC_FRESH in actions) {
                if (sample.t - lastFreshAt < params.ttsCooldownS) actions -= Action.HVAC_FRESH else lastFreshAt = sample.t
            }
            for (a in actions) {
                when (a) {
                    Action.SUGGEST_REST_STOP -> askYesNo(Voice.phrase(a)) { navi.addRestStop(live?.lat, live?.lon) }
                    else -> voice.say(Voice.phrase(a))
                }
                dashboard.note("t=${sample.t.toInt()}s  ${Voice.phrase(a)}")
                if (a == Action.HVAC_FRESH) {
                    cabin?.freshAir(sample)
                    hvac.freshAir(params.cabinT0.toFloat()) { voice.say("창문을 조금 열어 볼까요?") }
                }
            }
        }
    }

    /** "멀미 났어": records the current dose for the most sensitive passenger on board. */
    private fun reportSick() {
        val who = FamilyMonitor(params, profiles.load()).weakest ?: return
        reportedDose[who] = lastDose
        dashboard.note("멀미 기록: $who, 누적량 %.1f".format(lastDose))
    }

    /** Stops the trip; in family mode asks the single post-trip question per passenger. */
    private fun stopTrip() {
        stopSource()
        if (!familyMode || !tripRan) return
        tripRan = false
        askNext(profiles.load(), 0, mutableListOf())
    }

    private fun askNext(list: List<Profile>, i: Int, updated: MutableList<Profile>) {
        if (i >= list.size) {
            profiles.save(updated)
            dashboard.note("기준선 갱신: " + updated.joinToString { "${it.name} %.1f".format(it.threshold(params)) })
            return
        }
        val p = list[i]
        val answer = { r: Rating ->
            updated += ProfileLearning.update(params, p, r, reportedDose[p.name], tripMaxDose)
            askNext(list, i + 1, updated)
        }
        AlertDialog.Builder(this)
            .setTitle("오늘 ${p.name} 괜찮았어요?")
            .setPositiveButton("괜찮음") { _, _ -> answer(Rating.FINE) }
            .setNeutralButton("살짝") { _, _ -> answer(Rating.SLIGHT) }
            .setNegativeButton("심함") { _, _ -> answer(Rating.BAD) }
            .setCancelable(false)
            .show()
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
        super.onDestroy()
    }
}
