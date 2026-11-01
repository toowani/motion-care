package ai.motioncare.core

/** One passenger's learned sickness history (team page formula 5). */
data class Profile(val name: String, val onsetDoses: List<Double> = emptyList()) {
    fun threshold(p: Params): Double = personalThreshold(p, onsetDoses)
}

/** How the passenger felt after the trip (the single post-trip question). */
enum class Rating { FINE, SLIGHT, BAD }

object ProfileLearning {
    /**
     * Updates a profile after a trip.
     * A reported sickness adds the dose at which it was reported; a calm trip that went past the
     * current threshold adds an observation above it, so the threshold slowly rises.
     */
    fun update(p: Params, profile: Profile, rating: Rating, reportedDose: Double?, tripMaxDose: Double): Profile {
        val tau = profile.threshold(p)
        val add = when (rating) {
            Rating.SLIGHT, Rating.BAD -> reportedDose ?: tripMaxDose
            Rating.FINE -> if (tripMaxDose > tau) tripMaxDose * 1.2 else null
        } ?: return profile
        return profile.copy(onsetDoses = (profile.onsetDoses + add).takeLast(30))
    }
}

/**
 * Family mode: follows the most sensitive passenger on board and fires once when their
 * sickness probability crosses 0.5, before the index alone would react.
 */
class FamilyMonitor(private val p: Params, profiles: List<Profile>) {
    private val taus = profiles.associate { it.name to it.threshold(p) }
    private var fired = false

    val weakest: String? = taus.minByOrNull { it.value }?.key
    val threshold: Double? = taus.values.minOrNull()

    data class Result(val probability: Double, val alert: Boolean)

    fun step(dose: Double, m: Double): Result {
        val tau = threshold ?: return Result(0.0, false)
        val prob = sicknessProbability(p, dose, tau, m)
        val alert = !fired && prob >= 0.5
        if (alert) fired = true
        return Result(prob, alert)
    }
}
