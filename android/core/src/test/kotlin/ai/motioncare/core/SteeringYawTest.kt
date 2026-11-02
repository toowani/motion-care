package ai.motioncare.core

import kotlin.math.tan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SteeringYawTest {
    @Test
    fun straightDrivingKeepsGpsHeading() {
        val yaw = SteeringYaw()
        repeat(30) { assertEquals(90.0, yaw.step(15.0, 90.0, steeringDeg = 0.0, dt = 1.0), 1e-9) }
    }

    @Test
    fun steeringTurnsBeforeGpsCatchesUp() {
        val yaw = SteeringYaw()
        yaw.step(10.0, 0.0, 0.0, 1.0)
        // 145° at the wheel → 10° at the road wheels; GPS still says 0° (it lags the turn)
        val rate = Math.toDegrees(10.0 * tan(Math.toRadians(10.0)) / 2.9)   // ≈ 34.8 °/s
        val h1 = yaw.step(10.0, 0.0, 145.0, 1.0)
        assertEquals(rate * (1 - 0.2), h1, 1e-9)
        val h2 = yaw.step(10.0, 0.0, 145.0, 1.0)
        assertTrue(h2 > h1, "keeps turning right while GPS lags: $h1 → $h2")
        // when the wheel straightens and GPS stays put, the filter converges back to GPS
        var h = h2
        repeat(60) { h = yaw.step(10.0, 0.0, 0.0, 1.0) }
        assertEquals(0.0, wrap180(h), 0.01)
    }

    @Test
    fun wrapsAcrossNorth() {
        val yaw = SteeringYaw()
        yaw.step(10.0, 350.0, 0.0, 1.0)
        val h = yaw.step(10.0, 355.0, 145.0, 1.0)
        assertTrue(h in 0.0..60.0, "right turn from 350° passes north: $h")
        // a left turn from 5° goes below 0 and wraps to the 300s
        val left = SteeringYaw()
        left.step(10.0, 5.0, 0.0, 1.0)
        val hl = left.step(10.0, 5.0, -145.0, 1.0)
        assertTrue(hl in 300.0..360.0, "left turn from 5° wraps: $hl")
        assertEquals(20.0, wrap180(10.0 - 350.0), 1e-12)
    }

    @Test
    fun nullSteeringReturnsGpsBearing() {
        val yaw = SteeringYaw()
        assertEquals(123.0, yaw.step(10.0, 123.0, null, 1.0), 1e-12)
        yaw.step(10.0, 123.0, 145.0, 1.0)
        assertEquals(200.0, yaw.step(10.0, 200.0, null, 1.0), 1e-12)
    }

    @Test
    fun holdsHeadingWhenNearlyStopped() {
        val yaw = SteeringYaw()
        yaw.step(10.0, 45.0, 0.0, 1.0)
        assertEquals(45.0, yaw.step(0.5, 270.0, 300.0, 1.0), 1e-12)
    }
}
