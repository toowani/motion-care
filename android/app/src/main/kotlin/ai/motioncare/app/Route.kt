package ai.motioncare.app

import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot

/** Metres along the scenario route for each CSV row (one row per second). */
object RouteTrack {
    /** Uses the optional "s" column; older CSVs without it fall back to the distance driven between fixes. */
    fun fromCsv(text: String): DoubleArray? {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.size < 2) return null
        val col = lines.first().split(',').withIndex().associate { (i, n) -> n.trim() to i }
        val rows = lines.drop(1).map { it.split(',') }
        col["s"]?.let { i ->
            val s = rows.map { it.getOrNull(i)?.toDoubleOrNull() ?: Double.NaN }
            if (s.none { it.isNaN() }) return s.toDoubleArray()
        }
        val la = col["lat"] ?: return null
        val lo = col["lon"] ?: return null
        val out = DoubleArray(rows.size)
        var prev: Pair<Double, Double>? = null
        rows.forEachIndexed { k, f ->
            val lat = f.getOrNull(la)?.toDoubleOrNull()
            val lon = f.getOrNull(lo)?.toDoubleOrNull()
            val p = prev
            out[k] = (if (k > 0) out[k - 1] else 0.0) +
                (if (p != null && lat != null && lon != null) NavRoute.metres(p.first, p.second, lat, lon) else 0.0)
            if (lat != null && lon != null) prev = lat to lon
        }
        return out
    }
}

/** A scenario's road polyline and its navigation maneuvers (sim/scenarios/NAME.route.json). */
class NavRoute(
    val name: String,
    val title: String,
    val dest: String,
    val lat: DoubleArray,
    val lon: DoubleArray,
    val steps: List<Step>,
) {
    data class Step(val s: Double, val type: String, val text: String, val road: String)

    /** Cumulative metres at each geometry point. */
    val cum = DoubleArray(lat.size).also { c ->
        for (i in 1 until lat.size) c[i] = c[i - 1] + metres(lat[i - 1], lon[i - 1], lat[i], lon[i])
    }
    val totalM get() = cum.lastOrNull() ?: 0.0

    /** First maneuver still ahead of [s]. */
    fun nextStep(s: Double) = steps.firstOrNull { it.s > s + 2 }

    /**
     * Map matching for live fixes: the nearest point on the polyline, searched near the previous
     * match first so a road that doubles back does not make the car jump.
     */
    fun nearestS(la: Double, lo: Double, hint: Double?): Double {
        val n = lat.size
        if (n < 2) return 0.0
        val range = if (hint == null) 0 until n - 1 else {
            val i = cum.indexOfFirst { it >= hint }.let { if (it < 0) n - 1 else it }
            maxOf(0, i - 40) until minOf(n - 1, i + 80)
        }
        val kx = 111_195.0 * cos(Math.toRadians(la))
        var best = Double.MAX_VALUE
        var bestS = 0.0
        for (i in range) {
            val ax = (lon[i] - lo) * kx; val ay = (lat[i] - la) * 111_195.0
            val bx = (lon[i + 1] - lo) * kx; val by = (lat[i + 1] - la) * 111_195.0
            val dx = bx - ax; val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 > 0) (-(ax * dx + ay * dy) / len2).coerceIn(0.0, 1.0) else 0.0
            val d = hypot(ax + dx * t, ay + dy * t)
            if (d < best) { best = d; bestS = cum[i] + (cum[i + 1] - cum[i]) * t }
        }
        return if (hint != null && best > 60) nearestS(la, lo, null) else bestS
    }

    /** Direction of travel at [s], degrees clockwise from north. */
    fun bearingAt(s: Double): Double {
        val i = cum.indexOfFirst { it > s }.let { if (it <= 0) 1 else it }.coerceAtMost(lat.size - 1)
        val dy = lat[i] - lat[i - 1]
        val dx = (lon[i] - lon[i - 1]) * cos(Math.toRadians(lat[i]))
        return (Math.toDegrees(atan2(dx, dy)) + 360) % 360
    }

    /** "[[lat,lon],...]" for the map page. */
    fun geometryJson() = buildString {
        append('[')
        for (i in lat.indices) { if (i > 0) append(','); append('[').append(lat[i]).append(',').append(lon[i]).append(']') }
        append(']')
    }

    companion object {
        fun metres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dy = (lat2 - lat1) * 111_195.0
            val dx = (lon2 - lon1) * 111_195.0 * cos(Math.toRadians((lat1 + lat2) / 2))
            return hypot(dx, dy)
        }

        fun parse(text: String): NavRoute {
            val o = JSONObject(text)
            val g = o.getJSONArray("geometry")
            val lat = DoubleArray(g.length()) { g.getJSONArray(it).getDouble(0) }
            val lon = DoubleArray(g.length()) { g.getJSONArray(it).getDouble(1) }
            require(lat.size >= 2) { "route needs at least two points" }
            val st = o.optJSONArray("steps")
            val steps = (0 until (st?.length() ?: 0)).map { i ->
                val s = st!!.getJSONObject(i)
                Step(s.getDouble("s"), s.optString("type"), s.optString("text"), s.optString("road"))
            }.sortedBy { it.s }
            return NavRoute(o.optString("name"), o.optString("title"), o.optString("dest"), lat, lon, steps)
        }
    }
}
