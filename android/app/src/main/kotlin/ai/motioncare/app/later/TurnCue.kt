package ai.motioncare.app.later

import ai.motioncare.app.NaviGuide
import ai.motioncare.app.Settings
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.control.LightSwitch
import android.util.Log

private const val TAG = "MotionCare"
/** The navigation announces a turn itself within roughly this distance. */
private const val NAVI_GUIDES_WITHIN_M = 300.0

/**
 * Parked: how a turn cue used to sound. Turn cues should not talk over the navigation or wear
 * people out: while the navigation is guiding a turn it already announces it, so only a tone
 * remains; otherwise [Settings.cueStyle] decides between a short tone, the theme's sentence or silence.
 */
class TurnCueSound(private val settings: Settings, private val navi: NaviGuide, private val say: (String) -> Unit) {
    private val tone by lazy { runCatching { android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 70) }.getOrNull() }

    fun play(line: String) {
        val naviSpeaks = navi.nextTurn?.let { it.distanceM in 0.0..NAVI_GUIDES_WITHIN_M } == true
        when {
            settings.cueStyle == "silent" -> Unit
            settings.cueStyle == "voice" && !naviSpeaks -> say(line)
            else -> chime()
        }
    }

    private fun chime() {
        if (!settings.voiceOn) return
        val played = runCatching { tone?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 180) }.getOrNull() == true
        Log.i(TAG, "turn cue tone played=$played")
    }

    fun release() { runCatching { tone?.release() } }
}

/**
 * Parked: turn cue by light. The ambient light on the turning side glows amber and the other side
 * dims for a few seconds, then everything returns to how the driver had it. Cars that expose the
 * whole cabin as one ambient zone (the Pleos emulator does) get two amber pulses instead.
 * Kept soft on purpose: a bright cabin at night makes the road outside harder to see, so the cue
 * stays at about 60 % brightness and the "off" beat only falls back to the driver's own level.
 * Areas and brightness range come from the car's capabilities.
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
