/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/Format.kt                                 ║
║ How numbers appear on screen. Storage is always lb / mi / in.      ║
║ When "metric" is on in Settings, we only CONVERT FOR DISPLAY:      ║
║   kg = lb × 0.45359237, km = mi × 1.609344, cm = in × 2.54         ║
║ And when you type a number in kg, it's turned back into lb before  ║
║ saving (toLb). Example: you type 100 kg → saved as 220.46 lb.      ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui

import com.gymcat.app.core.calc.BodyMath
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object Fmt {
    private fun trim(x: Double, decimals: Int = 1): String {
        val r = String.format(Locale.US, "%.${decimals}f", x)
        return if (r.contains('.')) r.trimEnd('0').trimEnd('.') else r
    }

    fun weightNum(lb: Double, metric: Boolean): String = trim(if (metric) BodyMath.lbToKg(lb) else lb)
    fun weight(lb: Double, metric: Boolean): String = weightNum(lb, metric) + if (metric) " kg" else " lb"
    fun weightUnit(metric: Boolean) = if (metric) "kg" else "lb"
    /** Typed number (in the unit on screen) → lb for storage. */
    fun toLb(value: Double, metric: Boolean): Double = if (metric) BodyMath.kgToLb(value) else value

    fun distNum(mi: Double, metric: Boolean): String = trim(if (metric) BodyMath.miToKm(mi) else mi, 2)
    fun dist(mi: Double, metric: Boolean): String = distNum(mi, metric) + if (metric) " km" else " mi"
    fun distUnit(metric: Boolean) = if (metric) "km" else "mi"
    fun toMi(value: Double, metric: Boolean): Double = if (metric) BodyMath.kmToMi(value) else value

    fun lengthNum(inches: Double, metric: Boolean): String = trim(if (metric) BodyMath.inToCm(inches) else inches)
    fun lengthUnit(metric: Boolean) = if (metric) "cm" else "in"
    fun toIn(value: Double, metric: Boolean): Double = if (metric) BodyMath.cmToIn(value) else value

    /** 83 → "1:23", 3723 → "1:02:03" */
    fun duration(sec: Long): String {
        val s = abs(sec); val h = s / 3600; val m = (s % 3600) / 60; val r = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, r) else "%d:%02d".format(m, r)
    }

    /** Pace from seconds per mile → "9:41 /mi" (or per km). */
    fun pace(secPerMile: Double, metric: Boolean): String {
        val perUnit = if (metric) secPerMile / BodyMath.KM_PER_MI else secPerMile
        return duration(perUnit.roundToInt().toLong()) + if (metric) " /km" else " /mi"
    }

    fun kcal(k: Double) = "${k.roundToInt()} kcal"

    /**
     * Cleans a typed number. Spanish keyboards type "62,5" — the comma becomes a dot.
     * Keeps digits, one dot, and (if allowed) a leading minus. Example: "6,25" → "6.25".
     */
    fun cleanDecimal(raw: String, allowNegative: Boolean = false, maxLen: Int = 8): String {
        val s = raw.replace(',', '.')
        val out = StringBuilder()
        for ((i, ch) in s.withIndex()) {
            when {
                ch.isDigit() -> out.append(ch)
                ch == '.' && '.' !in out -> out.append(ch)
                ch == '-' && allowNegative && i == 0 -> out.append(ch)
            }
        }
        return out.toString().take(maxLen)
    }
    fun num(x: Double) = trim(x)
}
