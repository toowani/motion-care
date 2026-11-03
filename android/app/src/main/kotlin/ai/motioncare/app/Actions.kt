package ai.motioncare.app

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import android.util.Log
import kotlin.math.roundToInt

private const val TAG = "MotionCare"

/**
 * Closes the loop on the cabin: fresh air + a cooler set point through the Pleos HVAC API.
 * Needs CONTROL_CAR_CLIMATE. Success is reported only when the car confirms the new temperature;
 * with the climate off nothing is changed and the driver is asked to open a window instead.
 * During a replay the driver's own settings are saved first and put back by [restore].
 */
class HvacController(private val vehicle: Vehicle, private val log: (String) -> Unit) {
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private val area = SeatArea(SeatValue.ROW_1_LEFT)
    // the back seat is cooled through both front zones; only the driver's was set before
    private val passenger = SeatArea(SeatValue.ROW_1_RIGHT)
    private var savedTempPassenger: Float? = null
    private var savedRecirc: Pair<SeatArea, Boolean>? = null
    private var savedTemp: Float? = null

    /** [onDone] / [onDenied] arrive on the main thread, exactly one of them per call. */
    fun freshAir(targetC: Float, remember: Boolean, onDone: () -> Unit, onDenied: (String) -> Unit) {
        var settled = false
        val done: () -> Unit = { main.post { if (!settled) { settled = true; onDone() } } }
        val deny: (String) -> Unit = { msg -> main.post { if (!settled) { settled = true; onDenied(msg) } } }
        runCatching {
            val hvac = vehicle.getHVAC()
            val go = {
                runCatching { apply(targetC, remember, done) { e -> log("공조 온도 설정 실패: ${e.message}"); deny("창문을 조금 열면 좋아요") } }
                    .onFailure { Log.w(TAG, "hvac", it); deny("창문을 조금 열면 좋아요") }
            }
            // power is a per-zone property on some cars (the emulator: whole row): ask the car which area to read
            hvac.checkHvacPowerCapabilities({ areas: List<SeatArea> ->
                hvac.getHvacPowerState(areas.firstOrNull() ?: area, { on ->
                    if (on == false) { log("공조가 꺼져 있어 바꾸지 않았어요"); deny("공조 꺼짐 · 창문 조금") } else go()
                }, { e -> Log.w(TAG, "hvac power state", e); go() })
            }, { e -> Log.w(TAG, "hvac power caps", e); go() })
        }.onFailure { Log.w(TAG, "hvac", it); deny("창문을 조금 열면 좋아요") }
    }

    private fun apply(targetC: Float, remember: Boolean, done: () -> Unit, failed: (Exception) -> Unit) {
        val hvac = vehicle.getHVAC()
        // recirculation is not a per-seat property on every car: use the area the car reports
        hvac.checkHvacRecirculationCapabilities({ areas: List<SeatArea> ->
            val target = areas.firstOrNull() ?: area
            val set = { hvac.setHvacRecirculation(target, false, { _, on -> log("공조: 내기순환 ${if (on == true) "켬" else "끔(외기)"}") },
                { e -> log("공조 외기 전환 실패: ${e.message}") }) }
            if (remember && savedRecirc == null) hvac.getHvacRecirculationState(target, { v -> main.post { if (savedRecirc == null && v != null) savedRecirc = target to v }; set() }, { set() })
            else set()
        }, { e -> log("내기순환 지원 영역 확인 실패: ${e.message}") })
        val setTemp = {
            hvac.setHvacTemperature(area, targetC, TemperatureUnit.CELSIUS, { _, c -> log("공조: 설정 온도 $c°C"); done() }, failed)
            hvac.setHvacTemperature(passenger, targetC, TemperatureUnit.CELSIUS, { _, _ -> }, { e -> Log.w(TAG, "passenger temp", e) })
        }
        if (remember && savedTempPassenger == null) hvac.getHvacTemperature(passenger, TemperatureUnit.CELSIUS, { v -> main.post { if (savedTempPassenger == null) savedTempPassenger = v } }, { })
        if (remember && savedTemp == null) hvac.getHvacTemperature(area, TemperatureUnit.CELSIUS, { v -> main.post { if (savedTemp == null) savedTemp = v }; setTemp() }, { setTemp() })
        else setTemp()
    }

    /** Puts back what a replay changed (no-op when nothing was saved). */
    fun restore() {
        val r = savedRecirc
        val t = savedTemp
        val tp = savedTempPassenger
        savedRecirc = null
        savedTemp = null
        savedTempPassenger = null
        if (r == null && t == null && tp == null) return
        runCatching {
            val hvac = vehicle.getHVAC()
            r?.let { (a, on) -> hvac.setHvacRecirculation(a, on, { _, _ -> }, { e -> Log.w(TAG, "recirc restore", e) }) }
            t?.let { hvac.setHvacTemperature(area, it, TemperatureUnit.CELSIUS, { _, _ -> }, { e -> Log.w(TAG, "temp restore", e) }) }
            tp?.let { hvac.setHvacTemperature(passenger, it, TemperatureUnit.CELSIUS, { _, _ -> }, { e -> Log.w(TAG, "passenger temp restore", e) }) }
            log("공조: 원래 설정으로")
        }.onFailure { Log.w(TAG, "hvac restore", it) }
    }
}

/**
 * Opens the rear windows a little through the Pleos Window API (needs CONTROL_CAR_WINDOWS). The car
 * reports which windows it can move and their range; only second-row windows are used, and a window
 * already open further is left alone. Exactly one of [onDone] / [onDenied] runs, on the main thread:
 * denied when the car has no such window, refuses, or does not answer within a few seconds.
 */
class WindowController(private val vehicle: Vehicle, private val log: (String) -> Unit) {
    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    fun openRearALittle(onDone: () -> Unit, onDenied: () -> Unit) {
        var settled = false
        val done = { main.post { if (!settled) { settled = true; onDone() } } }
        val deny = { why: String -> main.post { if (!settled) { settled = true; log("창문: $why"); onDenied() } } }
        main.postDelayed({ deny("차 응답 없음") }, ANSWER_TIMEOUT_MS)
        runCatching {
            val w = vehicle.getWindow()
            w.checkWindowPositionCapabilities({ _, caps: Map<WindowArea, AreaBounds<Int>> ->
                Log.i(TAG, "window caps: " + caps.entries.joinToString { (a, b) -> a.values.joinToString("+") + " ${b.minValue}..${b.maxValue}" })
                val rear = caps.filterKeys { a -> a.values.isNotEmpty() && a.values.all { it == WindowValue.ROW_2_LEFT || it == WindowValue.ROW_2_RIGHT } }
                if (rear.isEmpty()) { deny("뒷창문 제어 없음"); return@checkWindowPositionCapabilities }
                var waiting = rear.size
                var moved = 0
                val settle = { ok: Boolean, why: String? -> main.post { if (ok) moved++; if (--waiting == 0) { if (moved > 0) done() else deny(why ?: "거절됨") } } }
                rear.forEach { (a, b) ->
                    val lo = b.minValue
                    val hi = b.maxValue
                    val target = lo + ((hi - lo) * OPEN_FRACTION).roundToInt().coerceAtLeast(1)
                    val set = {
                        w.setWindowPosition(a, target, { area, v -> Log.i(TAG, "window ${area.values.joinToString("+")} -> $v"); settle(true, null) },
                            { e -> Log.w(TAG, "window set", e); settle(false, e.message) })
                    }
                    w.getCurrentWindowPosition(a, { cur -> if (cur != null && cur >= target) settle(true, null) else set() }, { set() })
                }
            }, { e -> deny("지원 확인 실패 ${e.message}") })
        }.onFailure { Log.w(TAG, "window", it); deny("창문 API 없음") }
    }

    private companion object {
        /** "A little": a fifth of the travel lets air in without wind noise or a child's arm out. */
        const val OPEN_FRACTION = 0.2
        const val ANSWER_TIMEOUT_MS = 4000L
    }
}
