package ai.motioncare.core

/** Parses the sim/scenarios CSV files rows into [Sample]s (used by the app's replay mode and by tests). */
object ScenarioCsv {
    fun parse(text: String): List<Sample> {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return emptyList()
        val col = lines.first().split(',').withIndex().associate { (i, name) -> name.trim() to i }
        return lines.drop(1).map { line ->
            val f = line.split(',')
            fun s(name: String) = f[col.getValue(name)]
            fun d(name: String) = s(name).toDouble()
            fun opt(name: String) = col[name]?.let { f.getOrNull(it)?.toDoubleOrNull() } ?: Double.NaN
            Sample(
                t = d("t"),
                speed = d("speed"),
                bearing = d("bearing"),
                turnSignal = TurnSignal.valueOf(s("turn_signal")),
                nextTurnM = d("next_turn_m"),
                nextTurnLeft = s("next_turn_dir") == "L",
                cabinTemp = d("cabin_temp"),
                recirc = s("recirc") == "1",
                windowOpen = s("window_open") == "1",
                lat = opt("lat"),
                lon = opt("lon"),
            )
        }
    }
}
