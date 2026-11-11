package ai.motioncare.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Port of sim/engine.py. The numbers must match shared/test_vectors.csv.
 *
 * Formulas (team page numbering):
 *  1  a_x = dv/dt, a_y = v * yaw rate, then a band filter shaped like the lateral motion-sickness
 *     weighting (Donohew & Griffin 2004: flat 0.0315-0.25 Hz, falling above): 2nd-order Butterworth
 *     high-pass at 0.0315 Hz (removes steady acceleration and long curves) + 2nd-order low-pass
 *     (0.27 Hz, Q 0.95, one zero at -0.35). 1 Hz samples hold up to 0.5 Hz (Nyquist), where the
 *     whole path is about -12 dB; sim/freq_response.py measures it
 *  2  dose D = sqrt(sum (wX ax^2 + wY ay^2) dt), index I = RMS over last 60 s (zero-padded window)
 *  3  cabin factor m = 1 - gT*max(0, T - T0) - gV*min(1, t_recirc/3600), floor 0.6
 *  4  cue distance = v * (say + prepare)
 *  5  personal threshold tau = (n*meanD + n0*tau0) / (n + n0), P = sigmoid((D - tau*m)/s)
 */

enum class TurnSignal { OFF, LEFT, RIGHT }

enum class AlertState { NORMAL, CAUTION, HIGH }

enum class Action { TTS_DRIVER, HVAC_FRESH, SUGGEST_REST_STOP, CUE_LEFT, CUE_RIGHT }

/** One 1 Hz sample, the same columns as the sim/scenarios CSV files. */
data class Sample(
    val t: Double,
    val speed: Double,            // m/s
    val bearing: Double,          // compass degrees, clockwise from north
    val turnSignal: TurnSignal,
    val nextTurnM: Double,        // -1 when no turn guidance
    val nextTurnLeft: Boolean,
    val cabinTemp: Double,        // °C
    val recirc: Boolean,
    val windowOpen: Boolean,
    // position only feeds the map screen; NaN when unknown (the engine never reads it)
    val lat: Double = Double.NaN,
    val lon: Double = Double.NaN,
)

data class Output(
    val ax: Double,
    val ay: Double,
    val index: Double,
    val dose: Double,
    val m: Double,
    val state: AlertState,
    val thetaCaution: Double,
    val thetaHigh: Double,
    val actions: List<Action>,
)

/** Wrap an angle difference to (-pi, pi]. */
fun wrapPi(a: Double): Double {
    val twoPi = 2 * PI
    val r = ((a + PI) % twoPi + twoPi) % twoPi - PI
    return if (r == -PI) PI else r
}

class MotionEstimator(private val p: Params) {
    private var v: Double? = null
    private var theta = 0.0
    private var omega = 0.0

    fun step(speed: Double, bearingDeg: Double, dt: Double): Pair<Double, Double> {
        val th = Math.toRadians(bearingDeg)
        val prev = v
        if (prev == null) {
            v = speed
            theta = th
            return 0.0 to 0.0
        }
        val vNew = p.speedEmaAlpha * speed + (1 - p.speedEmaAlpha) * prev
        v = vNew
        val ax = (vNew - prev) / dt
        // optional EMA on speed and yaw rate (speedEmaAlpha 1 = off). It used to be 0.5, a 1st-order
        // low-pass at ~0.1 Hz that cut 0.1-0.25 Hz by 3-9 dB; BandFilter's low-pass now does the
        // smoothing of GPS bearing noise (which v multiplies in a_y)
        val w = wrapPi(th - theta) / dt
        theta = th
        omega = p.speedEmaAlpha * w + (1 - p.speedEmaAlpha) * omega
        val ay = if (vNew >= p.minSpeedForHeading) vNew * omega else 0.0
        return ax to ay
    }
}

/** y = b0 x + b1 x1 + b2 x2 - a1 y1 - a2 y2 (direct form I, zero initial state). */
class Biquad(
    private val b0: Double, private val b1: Double, private val b2: Double,
    private val a1: Double, private val a2: Double,
) {
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun step(x: Double): Double {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = x
        y2 = y1
        y1 = y
        return y
    }

    companion object {
        /** 2nd-order high-pass (bilinear transform, pre-warped: -3 dB at [f0] for q = 1/sqrt(2)). */
        fun highPass(f0: Double, q: Double, fs: Double): Biquad {
            val w0 = 2 * PI * f0 / fs
            val c = cos(w0)
            val al = sin(w0) / (2 * q)
            val a0 = 1 + al
            return Biquad((1 + c) / 2 / a0, -(1 + c) / a0, (1 + c) / 2 / a0, -2 * c / a0, (1 - al) / a0)
        }

        /**
         * 2nd-order low-pass: analog poles (natural frequency [fn], quality [q]) mapped by z = exp(sT),
         * one zero at z = -[zero], unity gain at 0 Hz. No forced zero at Nyquist (unlike the bilinear
         * transform), so 0.5 Hz keeps about -8 dB here and the whole path about -12 dB.
         */
        fun lowPass(fn: Double, q: Double, zero: Double, fs: Double): Biquad {
            val zeta = 1 / (2 * q)
            val wn = 2 * PI * fn / fs
            val r = exp(-zeta * wn)
            val th = wn * sqrt(1 - zeta * zeta)
            val a1 = -2 * r * cos(th)
            val a2 = r * r
            val g = (1 + a1 + a2) / (1 + zero)
            return Biquad(g, g * zero, 0.0, a1, a2)
        }
    }
}

/** Motion-sickness band for one axis: high-pass (steady part out) then low-pass. O(1) state. */
class BandFilter(p: Params) {
    private val hp = Biquad.highPass(p.bandHighPassHz, p.bandHighPassQ, p.sampleHz)
    private val lp = Biquad.lowPass(p.bandLowPassHz, p.bandLowPassQ, p.bandLowPassZero, p.sampleHz)

    fun step(a: Double): Double = lp.step(hp.step(a))
}

class SicknessIndex(private val p: Params) {
    private val window = ArrayDeque<Double>()
    private val size = (p.indexWindowS * p.sampleHz).toInt()
    private var doseSq = 0.0

    fun step(ax: Double, ay: Double, dt: Double): Pair<Double, Double> {
        val e = p.wX * ax * ax + p.wY * ay * ay
        window.addLast(e)
        if (window.size > size) window.removeFirst()
        doseSq += e * dt
        // fixed denominator (zero-padded window): dividing by window.size let the first few
        // samples of a ride inflate I into a false early alert
        return sqrt(window.sum() / size) to sqrt(doseSq)
    }
}

class CabinFactor(private val p: Params) {
    private var recircS = 0.0

    fun step(temp: Double, recirc: Boolean, windowOpen: Boolean, dt: Double): Double {
        recircS = if (recirc && !windowOpen) recircS + dt else 0.0
        val m = 1 - p.gammaT * max(0.0, temp - p.cabinT0) -
            p.gammaV * min(1.0, recircS / p.recircSaturationS)
        return max(p.cabinFactorFloor, m)
    }
}

/** Hysteresis on the index: enter after enterHoldS above theta*m, leave below exitRatio*theta*m. */
class AlertMachine(private val p: Params) {
    var state = AlertState.NORMAL
        private set
    private var aboveC = 0.0
    private var aboveH = 0.0
    private var lastTts = -1e9
    private var highS = 0.0

    data class Result(val state: AlertState, val actions: List<Action>, val thC: Double, val thH: Double)

    fun step(t: Double, index: Double, m: Double, dt: Double): Result {
        val thC = p.thetaCaution * m
        val thH = p.thetaHigh * m
        aboveC = if (index > thC) aboveC + dt else 0.0
        aboveH = if (index > thH) aboveH + dt else 0.0
        val prev = state
        if (state == AlertState.NORMAL && aboveC >= p.enterHoldS) state = AlertState.CAUTION
        if (state != AlertState.HIGH && aboveH >= p.enterHoldS) state = AlertState.HIGH
        if (state == AlertState.HIGH && index < p.exitRatio * thH) state = AlertState.CAUTION
        if (state == AlertState.CAUTION && index < p.exitRatio * thC) state = AlertState.NORMAL
        highS = if (state == AlertState.HIGH) highS + dt else 0.0
        val actions = mutableListOf<Action>()
        if (state != prev && state != AlertState.NORMAL) {
            if (t - lastTts >= p.ttsCooldownS) {
                actions += Action.TTS_DRIVER
                lastTts = t
            }
            actions += Action.HVAC_FRESH
        }
        if (state == AlertState.HIGH && abs(highS - p.highHoldForRestStopS) < dt / 2) {
            actions += Action.SUGGEST_REST_STOP
        }
        return Result(state, actions, thC, thH)
    }
}

/**
 * Speaks once per upcoming turn: on a turn-signal rising edge or when the turn is within v*(say+prepare).
 * A red light right before the turn (stopped >= 3 s with the turn < 40 m ahead) allows one more
 * cue on departure, so the passenger hears it a few seconds before the turn, not 40 s early.
 */
class CueScheduler(p: Params) {
    private val lead = p.cueSayS + p.cuePrepareS
    private var prevSignal = TurnSignal.OFF
    private var prevNextM = -1.0
    private var armed = true
    private var stoppedS = 0.0
    private var recued = false

    fun step(speed: Double, signal: TurnSignal, nextM: Double, nextLeft: Boolean, dt: Double = 1.0): Action? {
        // a new turn ahead (distance jumped up or guidance ended) re-arms the cue,
        // whatever the indicator is doing (a lingering signal must not swallow the next cue)
        if (nextM < 0 || nextM > prevNextM + 20) {
            armed = true
            recued = false
        }
        if (speed < STOP_SPEED) {
            stoppedS += dt
        } else {
            if (stoppedS >= STOP_HOLD_S && nextM >= 0 && nextM < STOP_NEAR_M && !recued) {
                armed = true
                recued = true
            }
            stoppedS = 0.0
        }
        val rising = signal != TurnSignal.OFF && prevSignal == TurnSignal.OFF
        val near = nextM >= 0 && nextM <= max(15.0, speed * lead)
        var cue: Action? = null
        if (armed && (rising || near)) {
            cue = when (signal) {
                TurnSignal.LEFT -> Action.CUE_LEFT
                TurnSignal.RIGHT -> Action.CUE_RIGHT
                TurnSignal.OFF -> if (nextLeft) Action.CUE_LEFT else Action.CUE_RIGHT
            }
            armed = false
        }
        prevSignal = signal
        prevNextM = nextM
        return cue
    }

    private companion object {
        const val STOP_SPEED = 0.5      // m/s
        const val STOP_HOLD_S = 3.0
        const val STOP_NEAR_M = 40.0
    }
}

fun personalThreshold(p: Params, onsetDoses: List<Double>): Double {
    val n = onsetDoses.size
    val mean = if (n > 0) onsetDoses.average() else 0.0
    return (n * mean + p.tauPriorN * p.tau0) / (n + p.tauPriorN)
}

fun sicknessProbability(p: Params, dose: Double, tau: Double, m: Double = 1.0): Double =
    1 / (1 + exp(-(dose - tau * max(p.cabinFactorFloor, m)) / p.tauSlope))

/** Family mode uses the most sensitive passenger on board. */
fun familyThreshold(taus: Collection<Double>): Double = taus.min()

class Engine(private val p: Params) {
    private val motion = MotionEstimator(p)
    private val lfx = BandFilter(p)
    private val lfy = BandFilter(p)
    private val index = SicknessIndex(p)
    private val cabin = CabinFactor(p)
    private val alerts = AlertMachine(p)
    private val cues = CueScheduler(p)
    private val dt = 1.0 / p.sampleHz
    private var lastSpeed = 0.0
    private var lastBearing = 0.0
    private var lastCabinTemp = p.cabinT0

    fun step(raw: Sample): Output {
        // sensor dropouts arrive as NaN: hold the last finite value (cabin falls back to T0)
        if (raw.speed.isFinite()) lastSpeed = raw.speed
        if (raw.bearing.isFinite()) lastBearing = raw.bearing
        if (raw.cabinTemp.isFinite()) lastCabinTemp = raw.cabinTemp
        val s = raw.copy(
            speed = lastSpeed, bearing = lastBearing, cabinTemp = lastCabinTemp,
            nextTurnM = if (raw.nextTurnM.isFinite()) raw.nextTurnM else -1.0,
        )
        val (rawX, rawY) = motion.step(s.speed, s.bearing, dt)
        val ax = lfx.step(rawX)
        val ay = lfy.step(rawY)
        val (idx, dose) = index.step(ax, ay, dt)
        val m = cabin.step(s.cabinTemp, s.recirc, s.windowOpen, dt)
        val r = alerts.step(s.t, idx, m, dt)
        val staleAir = s.recirc || s.cabinTemp > p.cabinT0 + 0.5
        val actions = r.actions.filter { it != Action.HVAC_FRESH || staleAir }.toMutableList()
        cues.step(s.speed, s.turnSignal, s.nextTurnM, s.nextTurnLeft, dt)?.let { actions += it }
        return Output(ax, ay, idx, dose, m, r.state, r.thC, r.thH, actions)
    }
}
