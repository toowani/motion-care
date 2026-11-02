package ai.motioncare.core

import kotlin.math.tan

/** Wrap a degree difference to (-180, 180]. */
fun wrap180(d: Double): Double {
    val r = ((d + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
    return if (r == -180.0) 180.0 else r
}

/** Wrap a compass bearing to [0, 360). */
fun wrap360(d: Double): Double = ((d % 360.0) + 360.0) % 360.0

/**
 * Heading that leads the GPS bearing by using the steering wheel angle (read-only).
 *
 * GPS bearing comes from consecutive fixes, so it lags a turn by a second or two; the wheel moves
 * before the car does. Kinematic bicycle model: road-wheel angle = wheel / [ratio],
 * yaw rate = v·tan(δ) / [wheelbase]. Each step predicts with that rate, then pulls toward
 * the GPS bearing with gain [k] (complementary filter) so model errors don't accumulate.
 *
 * Sign convention follows AOSP PERF_STEERING_ANGLE: positive = right turn = bearing increases.
 */
class SteeringYaw(
    private val ratio: Double = 14.5,
    private val wheelbase: Double = 2.9,
    private val k: Double = 0.2,
) {
    private var heading: Double? = null

    /** One step of [dt] s; returns the heading in degrees [0, 360). */
    fun step(speed: Double, gpsBearing: Double, steeringDeg: Double?, dt: Double): Double {
        // no wheel signal: fall back to GPS and restart from it once the signal returns
        if (steeringDeg == null) {
            heading = null
            return wrap360(gpsBearing)
        }
        val h0 = heading ?: return wrap360(gpsBearing).also { heading = it }
        // nearly stopped: GPS bearing is noise and the car barely yaws, so hold
        if (speed < 1.0) return h0

        val delta = Math.toRadians(steeringDeg / ratio)
        val yawDegPerS = Math.toDegrees(speed * tan(delta) / wheelbase)
        var h = h0 + yawDegPerS * dt
        // GPS bearing is only meaningful once the car covers a few metres per fix
        if (speed > 3.0) h += k * wrap180(gpsBearing - h)
        return wrap360(h).also { heading = it }
    }
}
