package ai.motioncare.app

import ai.motioncare.core.Action
import ai.motioncare.core.Engine
import ai.motioncare.core.Params
import ai.motioncare.core.ScenarioCsv
import ai.pleos.playground.vehicle.Vehicle
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout

/**
 * Demo shell: pick a simulated scenario (replay) or live Pleos signals, then the engine
 * decides and the car acts — turn cues, fresh air, rest-stop suggestion.
 */
class MainActivity : Activity() {
    private lateinit var params: Params
    private lateinit var dashboard: DashboardView
    private lateinit var speaker: Speaker
    private lateinit var vehicle: Vehicle
    private lateinit var hvac: HvacController
    private var source: SignalSource? = null

    private val scenarios = listOf(
        "B 굽은 길 · 거칠게" to "B_winding_rough.csv",
        "B 굽은 길 · 부드럽게" to "B_winding_smooth.csv",
        "A 도심 · 거칠게" to "A_city_rough.csv",
        "D 가족 귀성" to "D_family.csv",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        params = Params.fromJson(assets.open("params.json").bufferedReader().readText())
        speaker = Speaker(this)
        vehicle = Vehicle(this).also { runCatching { it.initialize() } }
        dashboard = DashboardView(this)
        hvac = HvacController(vehicle) { msg -> runOnUiThread { dashboard.note(msg) } }

        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(16, 16, 16, 8) }
        scenarios.forEach { (label, file) -> bar.addView(button(label) { startReplay(file) }) }
        bar.addView(button("실차 신호 (Pleos)") { startLive() })
        bar.addView(button("정지") { stopSource() })

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(bar, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(dashboard, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        })
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun startReplay(file: String) {
        stopSource()
        val samples = ScenarioCsv.parse(assets.open(file).bufferedReader().readText())
        val cabin = SimulatedCabin(params.cabinT0)
        run(ReplaySignalSource(samples, speedup = 4, cabin = cabin), cabin)
        dashboard.note("재생 시작: $file (4배속)")
    }

    private fun startLive() {
        // location and vehicle speed are runtime ("dangerous") permissions on AAOS
        val needed = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, "android.car.permission.CAR_SPEED")
            .filter { checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), 1)
        stopSource()
        run(PleosSignalSource(this, vehicle), cabin = null)
        dashboard.note("실차 신호 수신 시작")
    }

    private fun run(src: SignalSource, cabin: SimulatedCabin?) {
        val engine = Engine(params)
        dashboard.reset()
        source = src
        src.start { sample ->
            val out = engine.step(sample)
            dashboard.add(sample, out)
            for (a in out.actions) {
                speaker.say(Speaker.phrase(a))
                dashboard.note("t=${sample.t.toInt()}s  ${Speaker.phrase(a)}")
                if (a == Action.HVAC_FRESH) {
                    cabin?.freshAir(sample)
                    hvac.freshAir(params.cabinT0.toFloat()) { speaker.say("창문을 조금 열어 볼까요?") }
                }
            }
        }
    }

    private fun stopSource() {
        source?.stop()
        source = null
    }

    override fun onDestroy() {
        stopSource()
        speaker.shutdown()
        runCatching { vehicle.release() }
        super.onDestroy()
    }
}
