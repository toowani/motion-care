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
        return raw.lines().filter { it.contains('=') }.map { line ->
            val (name, doses) = line.split('=', limit = 2)
            Profile(name, doses.split(',').mapNotNull { it.toDoubleOrNull() })
        }
    }

    fun save(profiles: List<Profile>) {
        prefs.edit().putString("profiles", profiles.joinToString("\n") { p ->
            p.name + "=" + p.onsetDoses.joinToString(",") { "%.2f".format(java.util.Locale.US, it) }
        }).apply()
    }

    /** Demo family: 민준 gets sick easily, 서연 rarely (the team page example). */
    private fun defaults() = listOf(
        Profile("민준", listOf(20.0, 22.0, 24.0, 21.0, 23.0, 22.0, 25.0, 20.0, 24.0, 23.0)),
        Profile("서연", listOf(45.0, 48.0, 44.0)),
    )
}
