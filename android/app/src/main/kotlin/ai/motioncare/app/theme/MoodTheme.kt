package ai.motioncare.app.theme

import ai.motioncare.core.AlertState
import android.graphics.Canvas

/** How the passenger is doing, as the icon shows it. Follows the engine's alert state. */
enum class Mood { CALM, UNEASY, SICK;
    companion object {
        fun of(state: AlertState) = when (state) {
            AlertState.NORMAL -> CALM
            AlertState.CAUTION -> UNEASY
            AlertState.HIGH -> SICK
        }

        /**
         * The face as shown: the shake now, but never red while the chance of sickness is still low
         * (a red face next to a green "낮음" gauge read as a contradiction).
         */
        fun shown(state: AlertState, p: Double) = of(state).let { if (it == SICK && p < 0.25) UNEASY else it }
    }
}

/**
 * A look for the status icon that changes with the sickness index. Themes only draw; the engine,
 * alerts and voice logic stay the same. Draw with [android.graphics.Canvas] primitives only
 * (no bitmaps), so every theme scales to any panel size and stays light.
 *
 * - [level]: 0 = calm … 1 = at the high threshold … up to 1.5; use it to scale intensity smoothly.
 * - [t]: seconds since the view started; animated themes use it for idle motion (bobbing, blinking).
 */
interface MoodTheme {
    val id: String
    val title: String
    val subtitle: String
    /** True if [draw] changes with [t]; the view then redraws every frame. */
    val animated: Boolean

    /** One or two words under the icon, in the theme's voice ("편안해요", "어질어질"). */
    fun label(mood: Mood): String

    /** Spoken turn cue in the theme's voice; null keeps the default "곧 왼쪽으로 돌아요." */
    fun cueLine(left: Boolean): String? = null

    /** Draws the icon centred at ([cx], [cy]) inside a square of side [size]. */
    fun draw(c: Canvas, cx: Float, cy: Float, size: Float, mood: Mood, level: Float, t: Float)
}

object MoodThemes {
    val all: List<MoodTheme> by lazy { listOf(CleanTheme, ClayTheme, ToonTheme, IdolTheme) }
    fun byId(id: String?) = all.firstOrNull { it.id == id } ?: CleanTheme
}
