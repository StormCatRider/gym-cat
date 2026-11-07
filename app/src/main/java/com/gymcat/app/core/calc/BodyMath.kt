/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — core/calc/BodyMath.kt                        ║
║ 1. U.S. Navy tape-method body-fat estimate (inches).              ║
║ 2. Unit conversions for the kg/km/cm display switch.              ║
║ Storage is ALWAYS lb / miles / inches (matches Gym & Ñam); metric ║
║ is only how numbers are shown on screen.                           ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.core.calc

import com.gymcat.app.core.model.Sex
import kotlin.math.log10

object BodyMath {
    // ── U.S. Navy body-fat formula (Hodgdon & Beckett, 1984; DoD Instruction 1308.3), inches:
    // MEN:   %BF = 86.010 × log10(waist − neck) − 70.041 × log10(height) + 36.76
    // WOMEN: %BF = 163.205 × log10(waist + hip − neck) − 97.684 × log10(height) − 78.387
    //   waist = at the navel (men) / narrowest point (women), neck = below the larynx,
    //   hip = widest point of the buttocks, height = standing height. All in inches.
    // Example (man): height 70, neck 15, waist 34 →
    //   86.010 × log10(19) − 70.041 × log10(70) + 36.76
    //   = 86.010 × 1.2788 − 70.041 × 1.8451 + 36.76 = 109.99 − 129.23 + 36.76 ≈ 17.5 %
    // Typical accuracy ± 3–4 percentage points. Returns null if numbers don't make sense.
    fun navyBodyFat(sex: Sex, heightIn: Double, neckIn: Double, waistIn: Double, hipIn: Double?): Double? {
        if (heightIn <= 0 || neckIn <= 0 || waistIn <= 0) return null
        val v = when (sex) {
            Sex.MALE -> {
                val d = waistIn - neckIn
                if (d <= 0) return null
                86.010 * log10(d) - 70.041 * log10(heightIn) + 36.76
            }
            Sex.FEMALE -> {
                val h = hipIn ?: return null
                val d = waistIn + h - neckIn
                if (d <= 0) return null
                163.205 * log10(d) - 97.684 * log10(heightIn) - 78.387
            }
        }
        return if (v.isFinite() && v in 2.0..70.0) v else null
    }

    // ── Conversions (exact definitions)
    const val KG_PER_LB = 0.45359237      // 1 lb = 0.45359237 kg (exact)
    const val KM_PER_MI = 1.609344        // 1 mi = 1.609344 km (exact)
    const val CM_PER_IN = 2.54            // 1 in = 2.54 cm (exact)

    fun lbToKg(lb: Double) = lb * KG_PER_LB
    fun kgToLb(kg: Double) = kg / KG_PER_LB
    fun miToKm(mi: Double) = mi * KM_PER_MI
    fun kmToMi(km: Double) = km / KM_PER_MI
    fun inToCm(i: Double) = i * CM_PER_IN
    fun cmToIn(cm: Double) = cm / CM_PER_IN
}
