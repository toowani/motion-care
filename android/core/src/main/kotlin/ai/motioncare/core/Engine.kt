package ai.motioncare.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Port of sim/engine.py. The numbers must match shared/test_vectors.csv.
 *
 * Formulas (team page numbering):
 *  1  a_x = dv/dt, a_y = v * yaw rate (EMA on both), low-frequency = a - mean(last 10 s)
 *  2  dose D = sqrt(sum (wX ax^2 + wY ay^2) dt), index I = RMS over last 60 s
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
        // GPS bearing noise is multiplied by v in a_y, so the yaw rate gets the same EMA
        val w = wrapPi(th - theta) / dt
        theta = th
        omega = p.speedEmaAlpha * w + (1 - p.speedEmaAlpha) * omega
        val ay = if (vNew >= p.minSpeedForHeading) vNew * omega else 0.0
        return ax to ay
    }
}

/** Removes the slow (< 0.1 Hz) part by subtracting the mean of the last [window] samples. */
class LowFreqFilter(private val window: Int) {
    private val buf = ArrayDeque<Double>()

    fun step(a: Double): Double {
        buf.addLast(a)
        if (buf.size > window) buf.removeFirst()
        return a - buf.sum() / buf.size
    }
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
        return sqrt(window.sum() / window.size) to sqrt(doseSq)
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

/** Speaks once per upcoming turn: on a turn-signal rising edge or when the turn is within v*(say+prepare). */
class CueScheduler(p: Params) {
    private val lead = p.cueSayS + p.cuePrepareS
    private var prevSignal = TurnSignal.OFF
    private var prevNextM = -1.0
    private var armed = true

    fun step(speed: Double, signal: TurnSignal, nextM: Double, nextLeft: Boolean): Action? {
        // a new turn ahead (distance jumped up or guidance ended) re-arms the cue
        if (signal == TurnSignal.OFF && (nextM < 0 || nextM > prevNextM + 20)) armed = true
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
    private val lfx = LowFreqFilter((p.lowFreqWindowS * p.sampleHz).toInt())
    private val lfy = LowFreqFilter((p.lowFreqWindowS * p.sampleHz).toInt())
    private val index = SicknessIndex(p)
    private val cabin = CabinFactor(p)
    private val alerts = AlertMachine(p)
    private val cues = CueScheduler(p)
    private val dt = 1.0 / p.sampleHz

    fun step(s: Sample): Output {
        val (rawX, rawY) = motion.step(s.speed, s.bearing, dt)
        val ax = lfx.step(rawX)
        val ay = lfy.step(rawY)
        val (idx, dose) = index.step(ax, ay, dt)
        val m = cabin.step(s.cabinTemp, s.recirc, s.windowOpen, dt)
        val r = alerts.step(s.t, idx, m, dt)
        val staleAir = s.recirc || s.cabinTemp > p.cabinT0 + 0.5
        val actions = r.actions.filter { it != Action.HVAC_FRESH || staleAir }.toMutableList()
        cues.step(s.speed, s.turnSignal, s.nextTurnM, s.nextTurnLeft)?.let { actions += it }
        return Output(ax, ay, idx, dose, m, r.state, r.thC, r.thH, actions)
    }
}
