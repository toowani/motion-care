package ai.motioncare.core

import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EngineTest {
    private val shared = File(System.getProperty("sharedDir") ?: "../../shared")
    private val params = Params.fromJson(File(shared, "params.json").readText())

    /**
     * STEP 5 gate: the Kotlin engine reproduces the Python prototype on shared/test_vectors.csv.
     * The file holds several scenario segments, each from t=0; the engine is reset per scenario.
     */
    @Test
    fun matchesPythonTestVectors() {
        val lines = File(shared, "test_vectors.csv").readLines().filter { it.isNotBlank() }
        val col = lines.first().split(',').withIndex().associate { (i, n) -> n to i }
        var engine = Engine(params)
        var scenario = ""
        val perScenario = linkedMapOf<String, Int>()
        var maxErr = 0.0
        for (line in lines.drop(1)) {
            val f = line.split(',')
            fun s(n: String) = f[col.getValue(n)]
            fun d(n: String) = s(n).toDouble()
            if (s("scenario") != scenario) {
                scenario = s("scenario")
                engine = Engine(params)
            }
            perScenario[scenario] = (perScenario[scenario] ?: 0) + 1
            val sample = Sample(
                t = d("t"), speed = d("speed"), bearing = d("bearing"),
                turnSignal = TurnSignal.valueOf(s("turn_signal")),
                nextTurnM = d("next_turn_m"), nextTurnLeft = s("next_turn_dir") == "L",
                cabinTemp = d("cabin_temp"), recirc = s("recirc") == "1", windowOpen = s("window_open") == "1",
            )
            val out = engine.step(sample)
            val at = "$scenario t=${s("t")}"
            for ((name, value) in listOf("ax" to out.ax, "ay" to out.ay, "index" to out.index, "dose" to out.dose, "m" to out.m)) {
                val err = abs(value - d(name))
                maxErr = maxOf(maxErr, err)
                assertTrue(err < 1e-5, "$at $name: kotlin $value vs python ${d(name)}")
            }
            assertEquals(s("state"), out.state.name, "state at $at")
            assertEquals(s("actions"), out.actions.joinToString("|") { it.name }, "actions at $at")
        }
        assertTrue(perScenario.size >= 3, "expected several scenario segments, got ${perScenario.keys}")
        println("matched ${lines.size - 1} samples $perScenario, max abs error $maxErr")
    }

    private fun scenario(name: String) =
        ScenarioCsv.parse(File(shared, "../sim/scenarios/$name.csv").readText())

    private fun cueTimes(samples: List<Sample>): List<Pair<Double, Action>> {
        val engine = Engine(params)
        return samples.flatMap { s ->
            engine.step(s).actions.filter { it == Action.CUE_LEFT || it == Action.CUE_RIGHT }.map { s.t to it }
        }
    }

    /** Guidance points (next_turn_m segments) that received at least one cue. */
    private fun cuedTurns(samples: List<Sample>, cues: List<Pair<Double, Action>>): Int {
        var turn = 0
        var prev = -1.0
        val turnAt = samples.associate { s ->
            if (s.nextTurnM >= 0 && (prev < 0 || s.nextTurnM > prev + 20)) turn++
            prev = s.nextTurnM
            s.t to turn
        }
        return cues.map { turnAt.getValue(it.first) }.toSet().size
    }

    @Test
    fun lingeringTurnSignalKeepsLaterCues() {
        val base = scenario("A_city_rough")
        // the driver leaves the indicator on for 3 s after each turn
        val lingering = base.toMutableList()
        for (i in 1 until base.size) {
            if (base[i - 1].turnSignal != TurnSignal.OFF && base[i].turnSignal == TurnSignal.OFF) {
                for (k in i until minOf(base.size, i + 3)) {
                    if (base[k].turnSignal == TurnSignal.OFF) lingering[k] = base[k].copy(turnSignal = base[i - 1].turnSignal)
                }
            }
        }
        val expected = cueTimes(base)
        val got = cueTimes(lingering)
        assertEquals(4, cuedTurns(base, expected))
        assertEquals(4, cuedTurns(lingering, got), "cues with a lingering signal: $got")
        assertEquals(expected, got)
    }

    /** Approach a right turn at 10 m/s, wait [waitS] s at a red light 12 m before it, then go. */
    private fun redLightRide(waitS: Int): List<Sample> {
        val out = mutableListOf<Sample>()
        var t = 0.0
        var dist = 200.0
        fun add(v: Double) {
            out += Sample(t, v, 90.0, TurnSignal.OFF, maxOf(0.0, dist).let { if (dist < -8) -1.0 else it }, false, 22.0, false, false)
            t += 1.0
            dist -= v
        }
        while (dist > 40) add(10.0)
        for (v in listOf(8.0, 6.0, 4.0, 2.0)) if (dist - v >= 12) add(v)
        dist = 12.0
        repeat(waitS) { add(0.0) }
        for (v in listOf(2.0, 4.0, 6.0, 8.0, 10.0, 10.0, 10.0)) add(v)
        return out
    }

    @Test
    fun redLightAllowsOneMoreCueOnDeparture() {
        val ride = redLightRide(30)
        val cues = cueTimes(ride)
        assertEquals(2, cues.size, "cues $cues")
        val departure = ride.first { it.t > 0 && it.speed > 0 && ride[(it.t - 1).toInt()].speed == 0.0 }.t
        val passed = ride.first { it.nextTurnM == 0.0 }.t
        assertTrue(cues[1].first >= departure, "second cue after departure: $cues")
        assertTrue(passed - cues[1].first in 2.0..10.0, "second cue ${passed - cues[1].first} s before the turn")
        // a short hold (under 3 s) is traffic, not a red light: no extra cue
        assertEquals(1, cueTimes(redLightRide(2)).size)
    }

    @Test
    fun nanInputsHoldLastFiniteValue() {
        val ride = scenario("A_city_rough").take(120)
        val holes = ride.mapIndexed { i, s ->
            if (i in 40..44) s.copy(speed = Double.NaN, bearing = Double.NaN, cabinTemp = Double.NaN) else s
        }
        val filled = ride.mapIndexed { i, s ->
            if (i in 40..44) s.copy(speed = ride[39].speed, bearing = ride[39].bearing, cabinTemp = ride[39].cabinTemp) else s
        }
        val a = Engine(params)
        val b = Engine(params)
        for ((x, y) in holes.zip(filled)) {
            val oa = a.step(x)
            val ob = b.step(y)
            assertTrue(oa.index.isFinite() && oa.dose.isFinite() && oa.m.isFinite(), "finite at t=${x.t}")
            assertEquals(ob, oa, "t=${x.t}")
        }
        // NaN before any finite value: speed/bearing 0, cabin at T0
        val first = Engine(params).step(ride[0].copy(speed = Double.NaN, bearing = Double.NaN, cabinTemp = Double.NaN, nextTurnM = Double.NaN))
        assertEquals(1.0, first.m, 1e-12)
        assertTrue(first.index == 0.0 && first.dose == 0.0)
    }

    @Test
    fun wrapPiKeepsShortestTurn() {
        assertEquals(Math.toRadians(20.0), wrapPi(Math.toRadians(10.0 - 350.0)), 1e-12)
        assertEquals(Math.PI, wrapPi(-Math.PI), 1e-12)
    }

    @Test
    fun cabinFactorExample() {
        // 26 °C after 40 min of recirculation → m ≈ 0.74 (team page formula 3)
        val cabin = CabinFactor(params)
        var m = 1.0
        repeat(2400) { m = cabin.step(26.0, recirc = true, windowOpen = false, dt = 1.0) }
        assertEquals(0.74, m, 0.005)
        assertEquals(1.0, cabin.step(22.0, recirc = false, windowOpen = false, dt = 1.0), 1e-12)
    }

    @Test
    fun personalThresholdShrinksTowardPrior() {
        // team page example uses tau0 = 40, n0 = 5
        val p = params.copy(tau0 = 40.0, tauPriorN = 5.0)
        assertEquals(34.4, personalThreshold(p, listOf(22.0, 25.0, 28.0)), 0.05)
        assertEquals(29.3, personalThreshold(p, List(10) { 24.0 }), 0.05)
        assertEquals(22.0, familyThreshold(listOf(41.0, 22.0)))
    }
}
