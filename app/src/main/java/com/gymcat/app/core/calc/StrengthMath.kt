/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — core/calc/StrengthMath.kt                    ║
║ All the lifting math in one place (pure Kotlin, no Android):       ║
║  1. Estimated 1RM (the most you could lift once): Epley + Brzycki  ║
║  2. Effective load for bodyweight moves (pull-ups, push-ups…)      ║
║  3. Volume (weight × reps)                                         ║
║  4. Next-weight suggestion from RPE (how hard the set felt)        ║
║  5. Automatic warm-up sets                                         ║
║  6. Deload (lighter week) suggestion                               ║
║ Every formula has a worked example right above it.                ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.core.calc

import com.gymcat.app.core.model.LogMode
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.floor

object StrengthMath {

    // ───────────────────── 1. Estimated one-rep max ─────────────────────
    // EPLEY (Epley, 1985):   1RM = w × (1 + r / 30)
    //   w = weight lifted (lb), r = reps done.
    //   Example: 185 lb × 5 reps → 185 × (1 + 5/30) = 185 × 1.1667 = 215.8 lb
    // BRZYCKI (Brzycki, 1993): 1RM = w × 36 / (37 − r)
    //   Example: 185 lb × 5 reps → 185 × 36 / 32 = 208.1 lb
    // Both get less accurate above ~10 reps. For r = 1 we return w itself.
    fun epley(w: Double, reps: Int): Double = when {
        reps <= 0 || w <= 0 -> 0.0
        reps == 1 -> w
        else -> w * (1 + reps / 30.0)
    }

    fun brzycki(w: Double, reps: Int): Double = when {
        reps <= 0 || w <= 0 || reps >= 37 -> 0.0   // formula breaks at 37 reps
        reps == 1 -> w
        else -> w * 36.0 / (37 - reps)
    }

    // ───────────────────── 2. Effective load ─────────────────────
    // For bodyweight moves you lift part of your own body:
    //   load = BW × pct + added − assist
    //   BW = body weight (lb), pct = share lifted (pull-up 1.00, push-up 0.64),
    //   signedLb = +added weight (vest/belt) or −assistance (band/machine).
    //   Example: 180 lb person, pull-up (1.00) with 25 lb belt → 180 + 25 = 205 lb
    //   Example: same person, push-up (0.64) → 180 × 0.64 = 115.2 lb
    fun effectiveLoad(mode: LogMode, enteredLb: Double, bodyLb: Double, bwPct: Double): Double =
        when (mode) {
            LogMode.WEIGHT_REPS -> enteredLb
            LogMode.BODYWEIGHT_REPS, LogMode.HOLD -> max(0.0, bodyLb * bwPct + enteredLb)
            else -> 0.0
        }

    // ───────────────────── 3. Volume ─────────────────────
    // volume = load × reps   (holds, cardio and stretches add 0)
    //   Example: 3 sets of 135 lb × 10 → 3 × 1,350 = 4,050 lb
    // Dumbbells are entered as ONE dumbbell, so volume counts one dumbbell.
    fun volume(mode: LogMode, enteredLb: Double, reps: Int, bodyLb: Double, bwPct: Double): Double =
        if (mode == LogMode.WEIGHT_REPS || mode == LogMode.BODYWEIGHT_REPS)
            effectiveLoad(mode, enteredLb, bodyLb, bwPct) * reps
        else 0.0

    // ───────────────────── 4. RPE-based next weight ─────────────────────
    // RPE = Rate of Perceived Exertion, 1–10 (10 = nothing left).
    // RIR = Reps In Reserve = 10 − RPE  (RPE 8 ≈ 2 reps left).
    // Idea: the reps you COULD have done = reps + RIR. Put that into Epley:
    //   e1RM = w × (1 + (r + RIR) / 30)
    // Then solve Epley backwards for the target reps and target RPE:
    //   next = e1RM / (1 + (rTarget + RIRtarget) / 30)
    // Example: last 185 × 5 at RPE 7 (RIR 3) → e1RM = 185 × (1 + 8/30) = 234.3
    //          target 5 reps at RPE 8 (RIR 2) → next = 234.3 / (1 + 7/30) = 190.0 lb
    // The change is capped at ±10% per session and rounded to the increment
    // (5 lb barbell/machine, 5 lb dumbbells, 2.5 kg in metric).
    fun e1rmWithRpe(w: Double, reps: Int, rpe: Double): Double =
        if (w <= 0 || reps <= 0) 0.0 else w * (1 + (reps + (10 - rpe)) / 30.0)

    fun suggestNext(
        lastLb: Double, lastReps: Int, lastRpe: Double?,
        targetReps: Int, targetRpe: Double?, incrementLb: Double,
    ): Double? {
        if (lastLb <= 0 || lastRpe == null || targetRpe == null || targetReps <= 0) return null
        val e = e1rmWithRpe(lastLb, lastReps, lastRpe)
        val raw = e / (1 + (targetReps + (10 - targetRpe)) / 30.0)
        val capped = raw.coerceIn(lastLb * 0.9, lastLb * 1.1)
        return roundTo(capped, incrementLb)
    }

    // Rounds to the nearest plate step (halves round UP).
    // Example: roundTo(191.7, 5.0) = 190.0 ; roundTo(112.5, 5.0) = 115.0
    fun roundTo(x: Double, inc: Double): Double = if (inc <= 0) x else floor(x / inc + 0.5) * inc

    // ───────────────────── 5. Warm-up sets ─────────────────────
    // Three ramp-up sets before the first working set:
    //   50% × 8, 70% × 5, 85% × 3   (never lighter than the empty bar)
    // Example: working 225 lb, bar 45 →
    //   112.5→115 × 8, 157.5→160 × 5 (rounded to 5), 191.25→190 × 3
    data class WarmupSet(val lb: Double, val reps: Int)

    fun warmups(workingLb: Double, barLb: Double, incrementLb: Double): List<WarmupSet> {
        if (workingLb <= barLb) return emptyList()
        val plan = listOf(0.50 to 8, 0.70 to 5, 0.85 to 3)
        val out = mutableListOf<WarmupSet>()
        for ((pct, reps) in plan) {
            val w = max(barLb, roundTo(workingLb * pct, incrementLb))
            if (w < workingLb && out.none { it.lb == w }) out += WarmupSet(w, reps)
        }
        return out
    }

    // ───────────────────── 6. Deload suggestion ─────────────────────
    // A lift is "stalled" when its best e1RM in the last 3 sessions is not
    // higher than its best before those 3 sessions.
    // Suggest a deload when:  ≥ 6 weeks since the last deload/program start,
    //                    or:  2 or more lifts are stalled.
    // Deload plan: weight × 0.90, half the sets (rounded up).
    //   Example: 4 × 5 at 200 lb → 2 × 5 at 180 lb.
    fun isStalled(sessionBestE1rm: List<Double>): Boolean {
        if (sessionBestE1rm.size < 4) return false
        val recent = sessionBestE1rm.takeLast(3).max()
        val before = sessionBestE1rm.dropLast(3).max()
        return recent <= before
    }

    fun shouldDeload(weeksSinceDeload: Int, stalledLifts: Int): Boolean =
        weeksSinceDeload >= 6 || stalledLifts >= 2

    fun deloadSets(sets: Int): Int = max(1, ceil(sets / 2.0).toInt())
    fun deloadWeight(lb: Double, inc: Double): Double = roundTo(lb * 0.9, inc)
}
