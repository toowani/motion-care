package ai.motioncare.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FamilyTest {
    private val p = Params()

    @Test
    fun followsTheMostSensitivePassengerAndFiresOnce() {
        val minjun = Profile("민준", List(10) { 22.0 })
        val seoyeon = Profile("서연", listOf(45.0, 48.0, 44.0))
        val monitor = FamilyMonitor(p, listOf(minjun, seoyeon))
        assertEquals("민준", monitor.weakest)
        assertTrue(monitor.threshold!! < seoyeon.threshold(p))

        var alerts = 0
        var dose = 0.0
        repeat(60) { dose += 1.0; if (monitor.step(dose, m = 1.0).alert) alerts++ }
        assertEquals(1, alerts)
    }

    @Test
    fun learningMovesThresholdTowardReports() {
        var kid = Profile("민준")
        val start = kid.threshold(p)
        repeat(5) { kid = ProfileLearning.update(p, kid, Rating.SLIGHT, reportedDose = 18.0, tripMaxDose = 25.0) }
        assertTrue(kid.threshold(p) < start)

        val calm = ProfileLearning.update(p, kid, Rating.FINE, reportedDose = null, tripMaxDose = 5.0)
        assertEquals(kid, calm, "a calm, short trip below the threshold teaches nothing")
        val calmLong = ProfileLearning.update(p, kid, Rating.FINE, reportedDose = null, tripMaxDose = 60.0)
        assertTrue(calmLong.threshold(p) > kid.threshold(p))
    }
}
