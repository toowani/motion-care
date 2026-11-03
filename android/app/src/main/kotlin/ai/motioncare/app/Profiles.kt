package ai.motioncare.app

import ai.motioncare.core.Profile
import android.content.Context

/**
 * Family profiles stay on the device (children's data never leaves the car).
 * Stored as "name=dose,dose,..." lines in SharedPreferences; nicknames only.
 */
class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("family_profiles", Context.MODE_PRIVATE)

    fun load(): List<Profile> {
        val raw = prefs.getString("profiles", null) ?: return defaults()
        return raw.lines().filter { it.contains('=') }.map(::parse)
    }

    fun save(profiles: List<Profile>) {
        prefs.edit().putString("profiles", profiles.joinToString("\n", transform = ::format)).apply()
    }

    /**
     * The one passenger profile used while family mode is parked: each 멀미 완화 press adds the
     * dose at that moment, and the shown chance of sickness follows its learnt threshold.
     */
    fun loadSelf(): Profile = prefs.getString("self", null)?.takeIf { it.contains('=') }?.let(::parse) ?: Profile(SELF)

    fun saveSelf(p: Profile) { prefs.edit().putString("self", format(p)).apply() }

    private fun parse(line: String): Profile {
        val (name, doses) = line.split('=', limit = 2)
        return Profile(name, doses.split(',').mapNotNull { it.toDoubleOrNull() })
    }

    private fun format(p: Profile) = p.name + "=" + p.onsetDoses.joinToString(",") { "%.2f".format(java.util.Locale.US, it) }

    /** Demo family: 민준 gets sick easily, 서연 rarely (the team page example). */
    private fun defaults() = listOf(
        Profile("민준", listOf(20.0, 22.0, 24.0, 21.0, 23.0, 22.0, 25.0, 20.0, 24.0, 23.0)),
        Profile("서연", listOf(45.0, 48.0, 44.0)),
    )

    private companion object {
        const val SELF = "나"
    }
}
