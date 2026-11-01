package ai.motioncare.core

import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EngineTest {
    private val shared = File(System.getProperty("sharedDir") ?: "../../shared")
    private val params = Params.fromJson(File(shared, "params.json").readText())

    /** STEP 5 gate: the Kotlin engine reproduces the Python prototype on shared/test_vectors.csv. */
    @Test
    fun matchesPythonTestVectors() {
        val lines = File(shared, "test_vectors.csv").readLines().filter { it.isNotBlank() }
        val col = lines.first().split(',').withIndex().associate { (i, n) -> n to i }
        val engine = Engine(params)
        var maxErr = 0.0
        for (line in lines.drop(1)) {
            val f = line.split(',')
            fun s(n: String) = f[col.getValue(n)]
            fun d(n: String) = s(n).toDouble()
            val sample = Sample(
                t = d("t"), speed = d("speed"), bearing = d("bearing"),
                turnSignal = TurnSignal.valueOf(s("turn_signal")),
                nextTurnM = d("next_turn_m"), nextTurnLeft = s("next_turn_dir") == "L",
                cabinTemp = d("cabin_temp"), recirc = s("recirc") == "1", windowOpen = s("window_open") == "1",
            )
            val out = engine.step(sample)
            for ((name, value) in listOf("ax" to out.ax, "ay" to out.ay, "index" to out.index, "dose" to out.dose, "m" to out.m)) {
                val err = abs(value - d(name))
                maxErr = maxOf(maxErr, err)
                assertTrue(err < 1e-5, "t=${s("t")} $name: kotlin $value vs python ${d(name)}")
            }
            assertEquals(s("state"), out.state.name, "state at t=${s("t")}")
            assertEquals(s("actions"), out.actions.joinToString("|") { it.name }, "actions at t=${s("t")}")
        }
        println("matched ${lines.size - 1} samples, max abs error $maxErr")
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
