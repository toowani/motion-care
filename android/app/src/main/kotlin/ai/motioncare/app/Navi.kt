package ai.motioncare.app

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.cos
import kotlin.math.hypot

private const val TAG = "MotionCare"

/**
 * NaviHelper bridge: next-turn guidance for cues (live mode) and adding a rest stop as a waypoint.
 * NaviHelper has no POI search, so the rest stops below are demo coordinates; a real service
 * would query a POI API for service areas ahead on the route.
 */
class NaviGuide(context: Context, private val log: (String) -> Unit) {
    data class NextTurn(val distanceM: Double, val left: Boolean)

    private data class RestStop(val name: String, val lat: Double, val lon: Double)

    private val restStops = listOf(
        RestStop("서울만남의광장휴게소", 37.4598, 127.0430),
        RestStop("기흥휴게소", 37.2386, 127.1101),
        RestStop("안성휴게소", 37.0137, 127.2155),
    )

    private val main = Handler(Looper.getMainLooper())
    private val navi = NaviHelper(context)
    private var ready = false

    @Volatile var nextTurn: NextTurn? = null
        private set

    private val listener = object : NaviHelperEventListener {
        override fun onTBTInfo(list: List<TBTInfo>) {
            val first = list.firstOrNull { it.type.name.contains("_L") || it.type.name.contains("_R") || it.type.name.endsWith("LEFT") || it.type.name.endsWith("RIGHT") }
            nextTurn = first?.let { NextTurn(it.distance, isLeft(it.type.name)) }
        }
        override fun onRouteEnded() { nextTurn = null }
        override fun onError(error: NaviErrorCode) { Log.w(TAG, "navi error $error") }
    }

    private val poll = object : Runnable {
        override fun run() {
            if (ready) runCatching { navi.getTBTInfo() }
            main.postDelayed(this, 1000)
        }
    }

    fun start() {
        runCatching {
            navi.addListener(listener)
            navi.initialize({ ready = true; main.post { log("NaviHelper 연결됨") } },
                { e -> main.post { log("NaviHelper 연결 실패: ${e.message}") } })
            main.post(poll)
        }.onFailure { Log.w(TAG, "navi", it) }
    }

    /** Adds the closest demo rest stop as the first waypoint. */
    fun addRestStop(lat: Double?, lon: Double?) {
        val here = if (lat != null && lon != null) lat to lon else 37.5 to 127.0
        val stop = restStops.minBy { distanceKm(here.first, here.second, it.lat, it.lon) }
        runCatching {
            navi.addWaypoint(RequestWaypointInfo(stop.lat, stop.lon, stop.name, WaypointIndex.FIRST, "", "", "", ""))
            log("경유지 추가 요청: ${stop.name}")
        }.onFailure { log("경유지 추가 실패: ${it.message}") }
    }

    fun release() {
        main.removeCallbacksAndMessages(null)
        runCatching { navi.removeListener(listener); navi.release() }
    }

    private fun isLeft(type: String) = type.endsWith("LEFT") || type.endsWith("_L") || type.contains("_L_") || type == "TURN_UTURN"

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dy = (lat2 - lat1) * 111.2
        val dx = (lon2 - lon1) * 111.2 * cos(Math.toRadians(lat1))
        return hypot(dx, dy)
    }
}
