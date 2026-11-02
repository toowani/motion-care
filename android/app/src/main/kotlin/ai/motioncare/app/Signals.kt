package ai.motioncare.app

import ai.motioncare.core.Sample
import ai.motioncare.core.SteeringYaw
import ai.motioncare.core.TurnSignal
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.listener.HvacRecirculationStateListener
import ai.pleos.playground.vehicle.listener.HvacTemperatureListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import android.annotation.SuppressLint
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.util.Log

private const val TAG = "MotionCare"
private const val STEERING_STALE_MS = 3_000L
/** VHAL SEAT_OCCUPANCY: 0 unknown, 1 vacant, 2 occupied. */
private const val SEAT_OCCUPIED = 2
private const val SEAT_VACANT = 1

/**
 * The Vehicle 2.3.2 SDK reports seat occupancy as `parseInt(v) == 1` (javap of SeatOccupancyListener's
 * caller), but VHAL 1 means VACANT, so its Boolean is inverted: true = empty seat. Checked in the
 * Virtual Console: "Occupied" publishes 2 (SDK false), "Empty" publishes 1 (SDK true).
 */
fun isOccupiedFromSdk(value: Boolean?): Boolean? = value?.let { !it }

/** Where 1 Hz samples come from: the car (Pleos SDK) or a recorded/simulated scenario. */
interface SignalSource {
    fun start(onSample: (Sample) -> Unit)
    fun stop()
}

/**
 * Stands in for the cabin during replay: after the app switches to fresh air the cabin
 * stops recirculating and the temperature relaxes toward the set point (time constant 120 s),
 * the same model as sim/prototype.py CabinSim.
 */
class SimulatedCabin(private val setPoint: Double = 22.0) {
    private var temp: Double? = null

    fun apply(s: Sample): Sample {
        val t = temp ?: return s
        val next = t + (setPoint - t) / 120.0
        temp = next
        return s.copy(cabinTemp = next, recirc = false)
    }

    fun freshAir(current: Sample) {
        if (temp == null && (current.recirc || current.cabinTemp > setPoint + 0.5)) temp = current.cabinTemp
    }
}

/**
 * Replays a scenario CSV ([speedup] = 4 plays a 6-minute drive in 90 s). Samples before [fromS]
 * are not played but kept in [warmup] so the engine can catch up silently; all times are rebased
 * so playback starts at t = 0.
 */
class ReplaySignalSource(
    samples: List<Sample>,
    speedup: Float,
    private val cabin: SimulatedCabin,
    fromS: Double = 0.0,
) : SignalSource {
    private val handler = Handler(Looper.getMainLooper())
    private val periodMs = 1000.0 / speedup
    private val rebased = samples.map { it.copy(t = it.t - fromS) }
    val warmup: List<Sample> = rebased.filter { it.t < 0 }
    private val play = rebased.filter { it.t >= 0 }
    private var i = 0
    private var running = false

    override fun start(onSample: (Sample) -> Unit) {
        running = true
        val t0 = android.os.SystemClock.uptimeMillis()
        val tick = object : Runnable {
            override fun run() {
                if (!running || i >= play.size) return
                val s = cabin.apply(play[i++])
                if (BuildConfig.DEBUG) Log.d(TAG, "replay t=%.0f v=%.1f".format(s.t, s.speed))
                onSample(s)
                // fixed timetable instead of chained delays: no drift, so the map sees an even beat
                handler.postAtTime(this, t0 + (i * periodMs).toLong())
            }
        }
        handler.post(tick)
    }

    override fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }
}

/**
 * Live signals on the Pleos Connect emulator or a car:
 * turn signal, HVAC and rear-seat occupancy from the Pleos Vehicle SDK, next turn from NaviHelper,
 * speed from CarPropertyManager, heading from the platform location provider (GPX playback on the emulator)
 * sharpened by the steering wheel angle (read only), which leads the GPS bearing into a turn.
 */
class PleosSignalSource(
    private val context: Context,
    private val vehicle: Vehicle,
    private val nextTurn: () -> NaviGuide.NextTurn?,
    /** true: someone sat down in the back (or was there from the start); false: the back seats emptied. */
    private val onRearOccupied: (Boolean) -> Unit,
) : SignalSource {
    private val handler = Handler(Looper.getMainLooper())
    private val driverSeat = SeatArea(SeatValue.ROW_1_LEFT)
    private var recircArea = driverSeat
    private var car: Car? = null
    private var t = 0.0

    @Volatile private var carSpeed = 0.0
    @Volatile private var gpsSpeed = 0.0
    private var lastFix: Location? = null
    @Volatile private var bearing = 0.0
    // null until the SDK reports one (no steering sensor → SteeringYaw passes the GPS bearing through)
    @Volatile private var steeringDeg: Double? = null
    @Volatile private var steeringAt = 0L
    private var yaw = SteeringYaw()
    @Volatile private var signal = TurnSignal.OFF
    @Volatile private var cabinTemp = 22.0
    @Volatile private var recirc = false
    @Volatile var lat: Double? = null
        private set
    @Volatile var lon: Double? = null
        private set
    private var props: CarPropertyManager? = null
    // rear seats: raw VHAL reads every 2 s; the SDK listener is the fallback when they are not allowed
    private val rearAreas = intArrayOf(android.car.VehicleAreaSeat.SEAT_ROW_2_LEFT, android.car.VehicleAreaSeat.SEAT_ROW_2_CENTER, android.car.VehicleAreaSeat.SEAT_ROW_2_RIGHT)
    private var rawSeats = true
    private val seatLast = mutableMapOf<Int, Boolean>()
    private val seatRun = mutableMapOf<Int, Int>()
    private val boarded = mutableSetOf<Int>()
    // one listener per seat: the SDK keeps only the last area registered for a given listener object
    private val rearSeats = listOf(SeatValue.ROW_2_LEFT to rearAreas[0], SeatValue.ROW_2_RIGHT to rearAreas[2]).associate { (v, area) ->
        SeatArea(v) to object : SeatOccupancyListener {
            override fun onSeatOccupancyUpdated(value: Boolean?) {
                Log.i(TAG, "seat $area SDK value=$value")
                handler.post { seatRead(area, isOccupiedFromSdk(value)) }
            }
            override fun onFailed(e: Exception) { Log.w(TAG, "seat occupancy", e) }
        }
    }

    /**
     * Debounced boarding: the first read after registration only sets a baseline; vacant → occupied
     * counts at once, an occupant already there needs a second occupied read in a row.
     */
    private fun seatRead(area: Int, occupied: Boolean?) {
        if (occupied == null) return
        val prev = seatLast.put(area, occupied)
        seatRun[area] = if (occupied) (seatRun[area] ?: 0) + 1 else 0
        if (!occupied && prev == true && boarded.remove(area) && boarded.isEmpty()) onRearOccupied(false)
        if (occupied && area !in boarded && (prev == false || (seatRun[area] ?: 0) >= 2)) {
            val first = boarded.isEmpty()
            boarded += area
            if (first) onRearOccupied(true)
        }
    }

    private fun pollSeats() {
        val mgr = props ?: return
        if (!rawSeats) return
        for (area in rearAreas) {
            if (!rawSeats) return
            val raw = runCatching { mgr.getIntProperty(VehiclePropertyIds.SEAT_OCCUPANCY, area) }
                .onFailure { e ->
                    if (e is SecurityException) {
                        Log.w(TAG, "raw seat occupancy not allowed (${e.message}), using the SDK listener")
                        rawSeats = false
                        runCatching { rearSeats.forEach { (seat, l) -> vehicle.getSeat().registerSeatOccupancy(seat, l) } }
                    }
                }.getOrNull() ?: continue
            if (seatLast[area] == null || seatLast[area] != (raw == SEAT_OCCUPIED)) Log.i(TAG, "seat $area raw=$raw")
            seatRead(area, when (raw) { SEAT_OCCUPIED -> true; SEAT_VACANT -> false; else -> null })
        }
    }

    private val turnListener = object : TurnSignalStateListener {
        override fun onTurnSignalStateUpdated(state: TurnSignalValue?) {
            signal = when (state) {
                TurnSignalValue.LEFT -> TurnSignal.LEFT
                TurnSignalValue.RIGHT -> TurnSignal.RIGHT
                else -> TurnSignal.OFF
            }
        }
        override fun onFailed(e: Exception) { Log.w(TAG, "turn signal", e) }
    }
    private val steeringListener = object : SteeringWheelAngleListener {
        // Pleos reports a right turn as negative (Virtual Console +540° right arrives as -540), while
        // SteeringYaw expects right = positive (bearing increases), so flip the sign here
        override fun onSteeringWheelAngleUpdated(value: Float?) { value?.let { if (steeringDeg == null) Log.i(TAG, "steering angle received: $it"); steeringDeg = -it.toDouble(); steeringAt = android.os.SystemClock.elapsedRealtime() } }
        override fun onFailed(e: Exception) { Log.w(TAG, "steering angle", e) }
    }
    private val tempListener = object : HvacTemperatureListener {
        override fun onHvacTemperatureUpdated(value: Float?) { value?.let { cabinTemp = it.toDouble() } }
        override fun onFailed(e: Exception) { Log.w(TAG, "cabin temperature", e) }
    }
    private val recircListener = object : HvacRecirculationStateListener {
        override fun onHvacAutoRecirculationStateUpdated(value: Boolean?) { value?.let { recirc = it } }
        override fun onFailed(e: Exception) { Log.w(TAG, "recirculation", e) }
    }
    private val speedCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) { (value.value as? Float)?.let { carSpeed = it.toDouble() } }
        override fun onErrorEvent(propId: Int, zone: Int) { Log.w(TAG, "speed error $propId") }
    }
    private val locationListener = LocationListener { loc: Location ->
        lat = loc.latitude
        lon = loc.longitude
        // GPX playback / geo fix may omit bearing and speed, so derive them from consecutive fixes
        val prev = lastFix
        val moved = prev?.distanceTo(loc)?.toDouble() ?: 0.0
        val dtS = prev?.let { (loc.elapsedRealtimeNanos - it.elapsedRealtimeNanos) / 1e9 } ?: 0.0
        if (dtS > 0.2 && moved / dtS > 70.0) {   // > 250 km/h: a GPS jump (re-acquired fix), not motion
            lastFix = loc
            return@LocationListener
        }
        // (the emulator's geo fix reports speed 0 / bearing 0 explicitly, so only trust them while moving)
        val reportsMotion = loc.hasSpeed() && loc.speed > 0.1f
        bearing = when {
            reportsMotion && loc.hasBearing() -> loc.bearing.toDouble()
            moved > 2.0 -> ((prev!!.bearingTo(loc) + 360f) % 360f).toDouble()
            else -> bearing
        }
        gpsSpeed = when {
            reportsMotion -> loc.speed.toDouble()
            dtS > 0.2 -> moved / dtS
            else -> gpsSpeed
        }
        lastFix = loc
    }

    /** Vehicle speed when the car reports motion, otherwise the location provider's. */
    private val speed get() = if (carSpeed > 0.1) carSpeed else gpsSpeed

    @SuppressLint("MissingPermission")
    override fun start(onSample: (Sample) -> Unit) {
        runCatching {
            vehicle.getTurnSignal().registerTurnSignalState(turnListener)
            val hvac = vehicle.getHVAC()
            hvac.registerHvacTemperatureCurrent(driverSeat, tempListener)
            hvac.checkHvacRecirculationCapabilities({ areas: List<SeatArea> ->
                val a = areas.firstOrNull() ?: driverSeat
                recircArea = a
                hvac.registerHvacRecirculationState(a, recircListener)
                hvac.getHvacRecirculationState(a, { recirc = it == true }, { Log.w(TAG, "recirc get", it) })
            }, { Log.w(TAG, "recirc capability", it) })
        }.onFailure { Log.w(TAG, "Pleos listeners", it) }
        // separate block: a car without the steering property must not lose the listeners above
        yaw = SteeringYaw()
        steeringDeg = null
        runCatching { vehicle.getSteeringWheel().registerSteeringWheelAngle(steeringListener) }
            .onFailure { Log.w(TAG, "steering listener", it) }
        runCatching {
            car = Car.createCar(context).also { c ->
                val mgr = c.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                props = mgr
                mgr.registerCallback(speedCallback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL)
            }
        }.onFailure { Log.w(TAG, "CarPropertyManager", it) }
        // a rear passenger on board is the cue to suggest family mode
        seatLast.clear(); seatRun.clear(); boarded.clear()
        if (props == null) { rawSeats = false; runCatching { rearSeats.forEach { (seat, l) -> vehicle.getSeat().registerSeatOccupancy(seat, l) } } }
        runCatching {
            val lm = context.getSystemService(LocationManager::class.java)
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locationListener, Looper.getMainLooper())
        }.onFailure { Log.w(TAG, "location", it) }

        val tick = object : Runnable {
            override fun run() {
                val turn = nextTurn()
                val v = speed
                // a steering value older than a few seconds means the sensor went quiet: fall back to GPS
                val steer = steeringDeg?.takeIf { android.os.SystemClock.elapsedRealtime() - steeringAt < STEERING_STALE_MS }
                val heading = yaw.step(v, bearing, steer, dt = 1.0)
                onSample(Sample(t, v, heading, signal, turn?.distanceM ?: -1.0, turn?.left ?: false, cabinTemp, recirc, false,
                    lat = lat ?: Double.NaN, lon = lon ?: Double.NaN))
                if (t.toInt() % 2 == 0) pollSeats()
                t += 1.0
                handler.postDelayed(this, 1000L)
            }
        }
        handler.post(tick)
    }

    override fun stop() {
        handler.removeCallbacksAndMessages(null)
        runCatching {
            vehicle.getTurnSignal().unregisterTurnSignalState(turnListener)
            rearSeats.forEach { (seat, l) -> vehicle.getSeat().unregisterSeatOccupancy(seat, l) }
            vehicle.getHVAC().unregisterHvacTemperatureCurrent(driverSeat, tempListener)
            vehicle.getHVAC().unregisterHvacRecirculationState(recircArea, recircListener)
        }
        runCatching { vehicle.getSteeringWheel().unregisterSteeringWheelAngle(steeringListener) }
        runCatching { car?.disconnect() }
        runCatching { context.getSystemService(LocationManager::class.java).removeUpdates(locationListener) }
    }
}
