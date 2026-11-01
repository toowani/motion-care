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
