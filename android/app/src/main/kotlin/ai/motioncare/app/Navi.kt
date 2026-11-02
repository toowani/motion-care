package ai.motioncare.app

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.graphics.Rect
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

    /** What the map screen shows in live mode: next maneuver (TurnType name) and what is left to the destination. */
    data class Guidance(
        val turnType: String?, val turnM: Double?, val description: String?,
        val remainM: Int?, val remainS: Int?, val destName: String?,
    )

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
    @Volatile private var tbt: TBTInfo? = null
    @Volatile private var driving: DrivingInfo? = null
    private var loggedDriving = false

    /** Null while no route is being guided. */
    val guidance: Guidance?
        get() {
            val t = tbt
            val d = driving?.destination
            if (t == null && d == null) return null
            return Guidance(t?.type?.name, t?.distance, t?.description, d?.distance, d?.duration, d?.name)
        }

    private val listener = object : NaviHelperEventListener {
        override fun onTBTInfo(list: List<TBTInfo>) {
            val first = list.firstOrNull { it.type.name.contains("_L") || it.type.name.contains("_R") || it.type.name.endsWith("LEFT") || it.type.name.endsWith("RIGHT") }
            val turn = first?.let { NextTurn(it.distance, isLeft(it.type.name)) }
            val prev = nextTurn
            // log once per turn: a new turn shows up as the distance jumping back up
            if (turn != null && (prev == null || turn.distanceM > prev.distanceM + 50)) {
                main.post { log("내비 다음 회전: ${if (turn.left) "왼쪽" else "오른쪽"} %.0f m".format(turn.distanceM)) }
            }
            nextTurn = turn
            tbt = list.firstOrNull()
        }
        override fun onDrivingInfo(info: DrivingInfo) {
            if (!loggedDriving) { loggedDriving = true; Log.i(TAG, "driving info: ${info.destination}") }
            driving = info
        }
        override fun onRouteEnded() { nextTurn = null; tbt = null; driving = null }
        override fun onRouteCancelled() { nextTurn = null; tbt = null; driving = null }
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
            navi.initialize({ main.post { ready = true; log("NaviHelper 연결됨"); pendingRegion?.let { updateOccupiedRegion(it) } } },
                { e -> main.post { log("NaviHelper 연결 실패: ${e.message}") } })
            main.post(poll)
        }.onFailure { Log.w(TAG, "navi", it) }
    }

    /** Starts route guidance to a destination (demo and QA: the emulator cannot type a Korean search). */
    fun routeTo(lat: Double, lon: Double, name: String) {
        runCatching {
            navi.requestRoute(RouteInfo(lat, lon, null, null, name, "", "", "", RouteOption.RECOMMENDED, "", false))
            log("경로 요청: $name")
        }.onFailure { log("경로 요청 실패: ${it.message}") }
    }

    /** Family mode on the road: ask the navigation for its "comfortable" route (fewer sharp turns). */
    fun preferComfortableRoute() {
        if (!ready) return
        runCatching {
            navi.changeRouteOption(RouteOption.COMFORTABLE)
            log("경로 옵션: 편안한 길")
        }.onFailure { log("경로 옵션 변경 실패: ${it.message}") }
    }

    /** Adds the closest demo rest stop as the first waypoint. */
    /** Adds the rest stop nearest to the car; false when the position is unknown or the navigation refuses. */
    fun addRestStop(lat: Double?, lon: Double?): Boolean {
        if (lat == null || lon == null || !lat.isFinite() || !lon.isFinite()) { log("위치를 몰라 휴게소를 넣지 못했어요"); return false }
        val stop = restStops.minBy { distanceKm(lat, lon, it.lat, it.lon) }
        return runCatching {
            navi.addWaypoint(RequestWaypointInfo(stop.lat, stop.lon, stop.name, WaypointIndex.FIRST, "", "", "", ""))
            log("경유지 추가 요청: ${stop.name}")
        }.onFailure { log("경유지 추가 실패: ${it.message}") }.isSuccess
    }

    /**
     * Tells the Pleos navigation which part of the screen our window covers (empty = none), so it can
     * keep the car visible beside us. Private SDK API: may be ignored or refused on this build.
     */
    fun updateOccupiedRegion(rect: Rect) {
        // keep the latest rect so it is sent once NaviHelper connects; a call before that would be lost
        pendingRegion = Rect(rect)
        if (!ready || regionRefused) return
        val r = runCatching { navi.getInternalApi().updateOccupiedRegion(rect) }
        Log.i(TAG, "updateOccupiedRegion $rect -> ${r.exceptionOrNull()?.let { "failed: $it" } ?: "ok"}")
        // the Pleos build answers SecurityException for non-system apps; asking again will not change that
        if (r.exceptionOrNull() is SecurityException) regionRefused = true
    }
    private var regionRefused = false
    private var pendingRegion: Rect? = null

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
