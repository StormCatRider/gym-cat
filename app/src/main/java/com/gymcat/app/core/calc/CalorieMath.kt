/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — core/calc/CalorieMath.kt                     ║
║ Calorie estimates with MET values (same method as Gym & Ñam).      ║
║   kcal = MET × kg × hours,  kg = lb / 2.2046                       ║
║ MET = "Metabolic Equivalent of Task": how many times harder than   ║
║ sitting still. Values from the Compendium of Physical Activities.  ║
║ For weightlifting we use the SET + REST split you chose:           ║
║   MET 5.0 while lifting, MET 1.5 while resting between sets.       ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.core.calc

import kotlin.math.max

object CalorieMath {
    const val LB_PER_KG = 2.2046
    const val MET_LIFTING = 5.0      // resistance training (Compendium; same as Gym & Ñam "weights")
    const val MET_REST = 1.5         // standing/sitting quietly between sets (≈1.3–1.5)
    const val SECONDS_PER_REP = 3    // typical rep tempo: ~1 s up, ~2 s down (estimate)

    // kcal = MET × kg × hours
    // Example: run (9.8) for 30 min at 180 lb → 9.8 × 81.65 × 0.5 = 400 kcal
    fun met(met: Double, bodyLb: Double, minutes: Double): Double =
        met * (bodyLb / LB_PER_KG) * (minutes / 60.0)

    // Time under load for one set: reps × 3 s, or the hold time for planks.
    fun activeSeconds(reps: Int, holdSeconds: Int): Int =
        if (holdSeconds > 0) holdSeconds else reps * SECONDS_PER_REP

    // SET + REST split:
    //   kcal = (5.0 × active h + 1.5 × rest h) × kg,  rest = total − active
    // Example: 180 lb (81.65 kg), 60-min workout, 20 sets × 10 reps
    //   active = 20 × 10 × 3 s = 600 s = 0.1667 h ; rest = 0.8333 h
    //   kcal = (5.0 × 0.1667 + 1.5 × 0.8333) × 81.65 = (0.833 + 1.250) × 81.65 ≈ 170 kcal
    fun strength(bodyLb: Double, totalMinutes: Double, activeSec: Int): Double {
        val kg = bodyLb / LB_PER_KG
        val totalSec = max(totalMinutes * 60.0, activeSec.toDouble())
        val restSec = totalSec - activeSec
        return (MET_LIFTING * activeSec / 3600.0 + MET_REST * restSec / 3600.0) * kg
    }
}
