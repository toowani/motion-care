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

/** Replays a scenario CSV in real time ([speedup] = 4 plays a 6-minute drive in 90 s). */
class ReplaySignalSource(
    private val samples: List<Sample>,
    private val speedup: Int,
    private val cabin: SimulatedCabin,
) : SignalSource {
    private val handler = Handler(Looper.getMainLooper())
    private var i = 0
    private var running = false

    override fun start(onSample: (Sample) -> Unit) {
        running = true
        val tick = object : Runnable {
            override fun run() {
                if (!running || i >= samples.size) return
                onSample(cabin.apply(samples[i++]))
                handler.postDelayed(this, 1000L / speedup)
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
    // one listener per seat: the SDK keeps only the last area registered for a given listener object
    private val rearSeats = listOf(SeatArea(SeatValue.ROW_2_LEFT), SeatArea(SeatValue.ROW_2_RIGHT)).associateWith {
        object : SeatOccupancyListener {
            override fun onSeatOccupancyUpdated(value: Boolean?) { if (value == true) handler.post { onRearOccupied(true) } }
            override fun onFailed(e: Exception) { Log.w(TAG, "seat occupancy", e) }
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
            // a rear passenger on board is the cue to suggest family mode
            rearSeats.forEach { (seat, l) -> vehicle.getSeat().registerSeatOccupancy(seat, l) }
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
            car =Car.createCar(context).also { c ->
                val mgr = c.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                mgr.registerCallback(speedCallback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL)
            }
        }.onFailure { Log.w(TAG, "CarPropertyManager", it) }
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
