/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — core/calc/GeoMath.kt                         ║
║ GPS run math (works offline, no map needed):                       ║
║  • distance between two GPS points (haversine formula)            ║
║  • total distance with noise filtering                             ║
║  • pace (min per mile) and split times per mile                    ║
║  • turning lat/lon into x/y so the route line can be drawn         ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.core.calc

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoMath {
    const val EARTH_RADIUS_MI = 3958.8

    private fun rad(deg: Double) = deg * PI / 180.0   // degrees → radians

    data class Point(val lat: Double, val lon: Double, val timeMs: Long, val accuracyM: Float = 0f)

    // HAVERSINE: great-circle distance between two points on a sphere.
    //   a = sin²(Δφ/2) + cos φ1 · cos φ2 · sin²(Δλ/2)
    //   d = 2 R · asin(√a)
    //   φ = latitude, λ = longitude (radians), R = Earth radius 3,958.8 mi.
    // Example: 1/100 of a degree of latitude ≈ 0.691 mi.
    fun haversineMi(a: Point, b: Point): Double {
        val p1 = rad(a.lat); val p2 = rad(b.lat)
        val dp = p2 - p1; val dl = rad(b.lon - a.lon)
        val h = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * EARTH_RADIUS_MI * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /** Keeps only trustworthy points: accuracy ≤ 30 m and speed below ~27 mph (no GPS jumps). */
    fun clean(points: List<Point>): List<Point> {
        val out = mutableListOf<Point>()
        for (p in points) {
            if (p.accuracyM > 30f) continue
            val last = out.lastOrNull()
            if (last != null) {
                val hours = (p.timeMs - last.timeMs) / 3_600_000.0
                if (hours <= 0) continue
                if (haversineMi(last, p) / hours > 27.0) continue   // faster than a sprinter = glitch
            }
            out += p
        }
        return out
    }

    fun totalMiles(points: List<Point>): Double =
        points.zipWithNext().sumOf { (a, b) -> haversineMi(a, b) }

    // PACE = time ÷ distance. Example: 30 min for 3.1 mi → 9.68 min/mi → "9:41 /mi"
    fun paceMinPerMile(seconds: Long, miles: Double): Double? =
        if (miles < 0.01) null else (seconds / 60.0) / miles

    /** Seconds taken for each full mile (mile 1, mile 2, …). */
    fun splitsSec(points: List<Point>): List<Long> {
        val out = mutableListOf<Long>()
        var dist = 0.0; var nextMark = 1.0
        var markTime = points.firstOrNull()?.timeMs ?: return out
        for ((a, b) in points.zipWithNext()) {
            val seg = haversineMi(a, b)
            if (seg <= 0) continue
            while (dist + seg >= nextMark) {      // a mile boundary is inside this segment
                val frac = (nextMark - dist) / seg
                val t = a.timeMs + ((b.timeMs - a.timeMs) * frac).toLong()
                out += (t - markTime) / 1000
                markTime = t; nextMark += 1.0
            }
            dist += seg
        }
        return out
    }

    /** Flat x/y (0..1) for drawing. Uses cos(latitude) so east-west isn't stretched. */
    fun project(points: List<Point>): List<Pair<Float, Float>> {
        if (points.size < 2) return emptyList()
        val midLat = rad(points.map { it.lat }.average())
        val xs = points.map { it.lon * cos(midLat) }; val ys = points.map { it.lat }
        val minX = xs.min(); val maxX = xs.max(); val minY = ys.min(); val maxY = ys.max()
        val span = maxOf(maxX - minX, maxY - minY, 1e-9)
        return points.indices.map { i ->
            (((xs[i] - minX) / span).toFloat()) to ((1 - (ys[i] - minY) / span).toFloat())
        }
    }

    /** Route saved as "lat,lon,time;lat,lon,time;…" (keeps ≤ 1,500 points). */
    fun encode(points: List<Point>): String {
        val step = maxOf(1, points.size / 1500)
        return points.filterIndexed { i, _ -> i % step == 0 }
            .joinToString(";") { "${r6(it.lat)},${r6(it.lon)},${it.timeMs}" }
    }

    // 6 decimals of a degree ≈ 4 inches — plenty for a running route.
    private fun r6(x: Double) = (kotlin.math.round(x * 1e6) / 1e6).toString()

    fun decode(s: String): List<Point> = s.split(';').mapNotNull { part ->
        val f = part.split(',')
        if (f.size < 3) null else runCatching { Point(f[0].toDouble(), f[1].toDouble(), f[2].toLong()) }.getOrNull()
    }
}
