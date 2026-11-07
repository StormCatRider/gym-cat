/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — core/calc/PlateMath.kt                       ║
║ Plate calculator: which plates go on EACH side of the bar.         ║
║ Method ("greedy"): start with the heaviest plate that still fits,  ║
║ keep adding, then move to the next smaller plate.                  ║
║ Pounds set: 45 lb bar; plates 45/35/25/10/5/2.5 lb.                ║
║ Kilos set (Settings): 20 kg bar; plates 25/20/15/10/5/2.5/1.25 kg. ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.core.calc

import kotlin.math.abs

object PlateMath {
    val LB_PLATES = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
    const val LB_BAR = 45.0
    val KG_PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    const val KG_BAR = 20.0

    /** perSide = plates for ONE side; achieved = total you will really lift. */
    data class Result(val perSide: List<Double>, val achieved: Double, val leftover: Double)

    // Formula: per side = (target − bar) / 2
    // Example: 225 lb → (225 − 45) / 2 = 90 per side → 45 + 45 → [45, 45]
    // Example: 190 lb → 72.5 per side → 45 + 25 + 2.5 → [45, 25, 2.5]
    // Example: 100 kg → 40 per side → 25 + 15 → [25, 15]
    fun perSide(target: Double, bar: Double, plates: List<Double>): Result {
        if (target <= bar) return Result(emptyList(), bar, target - bar)
        var remaining = (target - bar) / 2.0
        val used = mutableListOf<Double>()
        for (p in plates.sortedDescending()) {
            while (remaining + 1e-9 >= p) {     // tiny tolerance for decimals
                used += p
                remaining -= p
            }
        }
        val achieved = bar + 2 * used.sum()
        return Result(used, achieved, if (abs(remaining) < 1e-9) 0.0 else remaining * 2)
    }
}
