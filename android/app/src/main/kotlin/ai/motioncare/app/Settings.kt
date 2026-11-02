package ai.motioncare.app

import android.content.Context

/** User choices that survive restarts. Stored on the device only. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var themeId: String
        get() = prefs.getString("theme", "clean") ?: "clean"
        set(v) = prefs.edit().putString("theme", v).apply()
    var voiceOn: Boolean
        get() = prefs.getBoolean("voice", true)
        set(v) = prefs.edit().putBoolean("voice", v).apply()
    var lightCueOn: Boolean
        get() = prefs.getBoolean("light", true)
        set(v) = prefs.edit().putBoolean("light", v).apply()
    var autoClimateOn: Boolean
        get() = prefs.getBoolean("climate", true)
        set(v) = prefs.edit().putBoolean("climate", v).apply()
    var demoPanelOn: Boolean
        get() = prefs.getBoolean("demo", true)
        set(v) = prefs.edit().putBoolean("demo", v).apply()
    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(v) = prefs.edit().putBoolean("onboarded", v).apply()
}
