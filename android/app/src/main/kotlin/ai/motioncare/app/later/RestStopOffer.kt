package ai.motioncare.app.later

import ai.motioncare.app.NaviGuide

/**
 * Parked (round 7): the rest-stop question, asked when the engine plans SUGGEST_REST_STOP and a
 * few seconds after 멀미 완화. "응" adds the nearest demo rest stop as a waypoint (NaviGuide).
 * One offer at a time, none after a stop was added.
 *
 * - [askYesNo]: the service's voice + button question.
 * - [asking]: true when a question ending with [QUESTION] is already shown or queued.
 * - [position]: the car's position (live) or the replay's last sample; null when unknown.
 */
class RestStopOffer(
    private val navi: NaviGuide,
    private val askYesNo: (String, () -> Unit) -> Unit,
    private val asking: ((String) -> Boolean) -> Boolean,
    private val position: () -> Pair<Double?, Double?>,
    private val action: (String) -> Unit,
) {
    private var added = false

    /** Call at the start of every trip. */
    fun reset() { added = false }

    /** Engine and relief word it differently; both end with [QUESTION]. */
    fun offer(question: String = QUESTION) {
        fun isRest(q: String) = q.endsWith(QUESTION)
        if (added || asking(::isRest)) return
        askYesNo(question) {
            val (lat, lon) = position()
            if (navi.addRestStop(lat, lon)) { added = true; action("휴게소를 경유지로 넣었어요") } else action("가까운 휴게소에 들러 주세요")
        }
    }

    companion object {
        const val QUESTION = "가까운 휴게소에 들를까요?"
        /** After 멀미 완화 the question follows the spoken relief line instead of talking over it. */
        const val DELAY_AFTER_RELIEF_MS = 6_000L
    }
}
