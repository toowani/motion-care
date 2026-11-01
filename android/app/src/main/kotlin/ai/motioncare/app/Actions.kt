package ai.motioncare.app

import ai.motioncare.core.Action
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

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
 * Voice output. Uses the platform TextToSpeech for now; swap in the Pleos TextToSpeech SDK
 * once the project's SDK access is approved (it is "Coming Soon" in Playground).
 */
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) tts.language = Locale.KOREAN
    }

    fun say(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    fun shutdown() = tts.shutdown()

    companion object {
        fun phrase(a: Action): String = when (a) {
            Action.CUE_LEFT -> "곧 왼쪽으로 돌아요."
            Action.CUE_RIGHT -> "곧 오른쪽으로 돌아요."
            Action.TTS_DRIVER -> "뒷좌석이 흔들리고 있어요. 조금만 부드럽게 가 볼까요?"
            Action.HVAC_FRESH -> "바깥 공기로 바꾸고 온도를 조금 낮출게요."
            Action.SUGGEST_REST_STOP -> "흔들림이 계속돼요. 가까운 휴게소에 들를까요?"
        }
    }
}
