package ai.motioncare.app

import ai.motioncare.core.Action
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import android.util.Log

private const val TAG = "MotionCare"

/**
 * Closes the loop on the cabin: fresh air + a cooler set point through the Pleos HVAC API.
 * Needs CONTROL_CAR_CLIMATE; on failure the app falls back to asking the driver by voice.
 */
class HvacController(private val vehicle: Vehicle, private val log: (String) -> Unit) {
    private val area = SeatArea(SeatValue.ROW_1_LEFT)

    fun freshAir(targetC: Float = 22f, onDenied: () -> Unit) {
        runCatching {
            val hvac = vehicle.getHVAC()
            // recirculation is not a per-seat property on every car: use the area the car reports
            hvac.checkHvacRecirculationCapabilities({ areas: List<SeatArea> ->
                val target = areas.firstOrNull() ?: area
                hvac.setHvacRecirculation(target, false, { _, on -> log("공조: 내기순환 ${if (on == true) "켬" else "끔(외기)"}") },
                    { e -> log("공조 외기 전환 실패: ${e.message}"); onDenied() })
            }, { e -> log("내기순환 지원 영역 확인 실패: ${e.message}"); onDenied() })
            hvac.setHvacTemperature(area, targetC, TemperatureUnit.CELSIUS, { _, c -> log("공조: 설정 온도 $c°C") },
                { e -> log("공조 온도 설정 실패: ${e.message}") })
        }.onFailure { Log.w(TAG, "hvac", it); onDenied() }
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
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private var areas: List<SeatArea> = emptyList()
    private val bounds = mutableMapOf<SeatArea, IntRange>()
    private val saved = mutableMapOf<SeatArea, Pair<Int?, Int?>>()
    private var busy = false

    fun start() {
        runCatching {
            val light = vehicle.getLight()
            light.checkAmbientLightBrightnessColorCapabilities({ _, list: List<SeatArea> ->
                areas = list
                main.post { log("무드등 영역: " + list.joinToString { a -> a.values.joinToString("+") }) }
            }, { e -> main.post { log("무드등 색 지원 확인 실패: ${e.message}") } })
            light.checkAmbientLightBrightnessCapabilities({ _, map ->
                map.forEach { (a, b) -> bounds[a] = (b.minValue ?: 0)..(b.maxValue ?: 100) }
            }, { e -> Log.w(TAG, "ambient brightness caps", e) })
        }.onFailure { Log.w(TAG, "light", it) }
    }

    fun cue(left: Boolean, holdMs: Long = 3000) {
        if (busy || areas.isEmpty()) return
        busy = true
        val light = vehicle.getLight()
        val side = if (left) "LEFT" else "RIGHT"
        val split = areas.partition { a -> a.values.all { v -> v.name.endsWith(side) } }
        val directional = split.first.isNotEmpty() && split.second.isNotEmpty()
        val (turning, other) = if (directional) split else areas to emptyList()
        // remember the driver's setting first, then light the turning side and dim the other
        for (a in areas) {
            light.getAmbientLightBrightnessColor(a, { c -> saved[a] = c to saved[a]?.second }, { })
            light.getAmbientLightBrightness(a, { b -> saved[a] = saved[a]?.first to b }, { })
        }
        val glow = {
            turning.forEach { a ->
                light.setAmbientLightSwitch(a, ai.pleos.playground.vehicle.constant.control.LightSwitch.ON, { _, _ -> }, { })
                light.setAmbientLightBrightnessColor(a, CUE_COLOR, { _, _ -> }, { e -> log("무드등 색 실패: ${e.message}") })
                light.setAmbientLightBrightness(a, soft(a), { _, _ -> }, { e -> log("무드등 밝기 실패: ${e.message}") })
            }
            other.forEach { a -> light.setAmbientLightBrightness(a, (saved[a]?.second ?: soft(a)) / 2, { _, _ -> }, { }) }
        }
        main.postDelayed({ glow(); log(if (directional) "무드등: ${if (left) "왼쪽" else "오른쪽"} 켬" else "무드등: 호박색 두 번 깜빡임") }, 200)
        if (!directional) {
            main.postDelayed({ turning.forEach { a -> light.setAmbientLightBrightness(a, saved[a]?.second ?: (bounds[a]?.first ?: 0), { _, _ -> }, { }) } }, 900)
            main.postDelayed(glow, 1600)
        }
        main.postDelayed({ restore(); busy = false }, holdMs)
    }

    /** About 60 % of the car's range: visible to a child in the back, not glaring for the driver. */
    private fun soft(a: SeatArea): Int {
        val r = bounds[a] ?: 0..100
        return r.first + (r.last - r.first) * SOFT_PERCENT / 100
    }

    private fun restore() {
        val light = vehicle.getLight()
        saved.forEach { (a, s) ->
            s.first?.let { light.setAmbientLightBrightnessColor(a, it, { _, _ -> }, { }) }
            s.second?.let { light.setAmbientLightBrightness(a, it, { _, _ -> }, { }) }
        }
        log("무드등: 원래대로")
    }

    companion object {
        const val CUE_COLOR = 0xFFFFA000.toInt()   // amber
        const val SOFT_PERCENT = 60
    }
}
