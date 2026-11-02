package ai.motioncare.app

import ai.motioncare.core.Action
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.control.LightSwitch
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import android.util.Log

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
 * Turn cue by light: the ambient light on the turning side glows amber and the other side dims
 * for a few seconds, then everything returns to how the driver had it. Cars that expose the whole
 * cabin as one ambient zone (the Pleos emulator does) get two amber pulses instead.
 * Kept soft on purpose: a bright cabin at night makes the road outside harder to see, so the cue
 * stays at about 60 % brightness and the "off" beat only falls back to the driver's own level. Light reaches children
 * who cannot follow a spoken cue yet (Diels et al. 2023: predictive ambient lighting helped as
 * part of a combined package). Areas and brightness range come from the car's capabilities.
 */
class LightCue(private val vehicle: Vehicle, private val log: (String) -> Unit) {
    private class Saved(var color: Int? = null, var brightness: Int? = null, var switch: LightSwitch? = null)

    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private var areas: List<SeatArea> = emptyList()
    private val bounds = mutableMapOf<SeatArea, IntRange>()
    /** Written on the main thread only. */
    private val saved = mutableMapOf<SeatArea, Saved>()
    private var busy = false

    fun start() {
        runCatching {
            val light = vehicle.getLight()
            light.checkAmbientLightBrightnessColorCapabilities({ _, list: List<SeatArea> ->
                main.post { areas = list; log("무드등 영역: " + list.joinToString { a -> a.values.joinToString("+") }) }
            }, { e -> main.post { log("무드등 색 지원 확인 실패: ${e.message}") } })
            light.checkAmbientLightBrightnessCapabilities({ _, map ->
                main.post { map.forEach { (a, b) -> bounds[a] = (b.minValue ?: 0)..(b.maxValue ?: 100) } }
            }, { e -> Log.w(TAG, "ambient brightness caps", e) })
        }.onFailure { Log.w(TAG, "light", it) }
    }

    fun cue(left: Boolean, holdMs: Long = 3000) {
        if (busy || areas.isEmpty()) return
        busy = true
        runCatching {
            val light = vehicle.getLight()
            val side = if (left) "LEFT" else "RIGHT"
            val split = areas.partition { a -> a.values.all { v -> v.name.endsWith(side) } }
            val directional = split.first.isNotEmpty() && split.second.isNotEmpty()
            val (turning, other) = if (directional) split else areas to emptyList()
            // remember the driver's setting first; the glow starts once all of it is known (or after 200 ms)
            saved.clear()
            areas.forEach { saved[it] = Saved() }
            var waiting = areas.size * 3
            var started = false
            val go = Runnable {
                if (started || !busy) return@Runnable
                started = true
                play(turning, other, directional, left, holdMs)
            }
            val arrived = { block: () -> Unit -> main.post { block(); if (--waiting == 0) go.run() } }
            for (a in areas) {
                light.getAmbientLightBrightnessColor(a, { c -> arrived { saved[a]?.color = c } }, { arrived {} })
                light.getAmbientLightBrightness(a, { b -> arrived { saved[a]?.brightness = b } }, { arrived {} })
                light.getAmbientLightSwitch(a, { sw -> arrived { saved[a]?.switch = sw } }, { arrived {} })
            }
            main.postDelayed(go, 200)
        }.onFailure { Log.w(TAG, "light cue", it); busy = false }
    }

    private fun play(turning: List<SeatArea>, other: List<SeatArea>, directional: Boolean, left: Boolean, holdMs: Long) {
        val light = vehicle.getLight()
        val glow: () -> Unit = {
            runCatching {
                turning.forEach { a ->
                    light.setAmbientLightSwitch(a, LightSwitch.ON, { _, _ -> }, { })
                    light.setAmbientLightBrightnessColor(a, CUE_COLOR, { _, _ -> }, { e -> log("무드등 색 실패: ${e.message}") })
                    light.setAmbientLightBrightness(a, soft(a), { _, _ -> }, { e -> log("무드등 밝기 실패: ${e.message}") })
                }
                other.forEach { a -> light.setAmbientLightBrightness(a, (saved[a]?.brightness ?: soft(a)) / 2, { _, _ -> }, { }) }
            }.onFailure { Log.w(TAG, "light glow", it) }
        }
        glow()
        log(if (directional) "무드등: ${if (left) "왼쪽" else "오른쪽"} 켬" else "무드등: 호박색 두 번 깜빡임")
        if (!directional) {
            main.postDelayed({ runCatching { turning.forEach { a -> light.setAmbientLightBrightness(a, saved[a]?.brightness ?: (bounds[a]?.first ?: 0), { _, _ -> }, { }) } } }, 700)
            main.postDelayed(glow, 1400)
        }
        main.postDelayed({ restore() }, holdMs - 200)
    }

    /** About 60 % of the car's range: visible to a child in the back, not glaring for the driver. */
    private fun soft(a: SeatArea): Int {
        val r = bounds[a] ?: 0..100
        return r.first + (r.last - r.first) * SOFT_PERCENT / 100
    }

    private fun restore() {
        if (!busy) return
        busy = false
        runCatching {
            val light = vehicle.getLight()
            saved.forEach { (a, s) ->
                s.color?.let { light.setAmbientLightBrightnessColor(a, it, { _, _ -> }, { }) }
                s.brightness?.let { light.setAmbientLightBrightness(a, it, { _, _ -> }, { }) }
                s.switch?.let { light.setAmbientLightSwitch(a, it, { _, _ -> }, { }) }
            }
            log("무드등: 원래대로")
        }.onFailure { Log.w(TAG, "light restore", it) }
    }

    /** Puts the lights back at once if a cue is running and drops pending steps; call before the vehicle is released. */
    fun release() {
        restore()
        main.removeCallbacksAndMessages(null)
    }

    companion object {
        const val CUE_COLOR = 0xFFFFA000.toInt()   // amber
        const val SOFT_PERCENT = 60
    }
}
