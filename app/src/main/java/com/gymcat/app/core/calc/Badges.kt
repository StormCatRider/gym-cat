/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — core/calc/Badges.kt                          ║
║ The list of badges and the rule that unlocks each one.            ║
║ Families: workout counts, weekly streaks, strength vs body weight, ║
║ calisthenics and cardio milestones.                                ║
║ Input = a small summary of your history (BadgeStats).              ║
║ Output = the set of badge ids you have earned.                     ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.core.calc

data class BadgeDef(val id: String, val emoji: String, val titleEn: String, val titleEs: String) {
    fun title(es: Boolean) = if (es) titleEs else titleEn
}

/** Summary numbers the badge rules look at. Weights in lb, distances in miles. */
data class BadgeStats(
    val workouts: Int,
    val bestStreakWeeks: Int,
    val bodyLb: Double,
    val bestBenchLb: Double,      // heaviest single set weight, any reps ≥ 1
    val bestSquatLb: Double,
    val bestDeadliftLb: Double,
    val maxPullupsOneSet: Int,
    val maxPushupsOneSet: Int,
    val maxPlankSec: Int,
    val totalMiles: Double,
    val longestRunMi: Double,
)

object Badges {
    private val countSteps = listOf(1, 10, 25, 50, 100, 250, 500)
    private val streakSteps = listOf(4, 8, 12, 26, 52)
    private val mileSteps = listOf(10, 50, 100, 500)

    val all: List<BadgeDef> = buildList {
        countSteps.forEach { add(BadgeDef("count_$it", "🏋️", "$it workouts", "$it entrenamientos")) }
        streakSteps.forEach { add(BadgeDef("streak_$it", "🔥", "$it-week streak", "Racha de $it semanas")) }
        for ((lift, en, es) in listOf(
            Triple("bench", "Bench", "Press de banca"),
            Triple("squat", "Squat", "Sentadilla"),
            Triple("deadlift", "Deadlift", "Peso muerto"),
        )) for (x in listOf("1", "1.5", "2")) {
            add(BadgeDef("bw_${lift}_$x", "💪", "$en ${x}× body weight", "$es ${x}× tu peso"))
        }
        add(BadgeDef("first_pullup", "🧗", "First pull-up", "Primera dominada"))
        add(BadgeDef("pullups_10", "🧗", "10 pull-ups in a set", "10 dominadas seguidas"))
        add(BadgeDef("pushups_20", "🙌", "20 push-ups in a set", "20 lagartijas seguidas"))
        add(BadgeDef("plank_60", "🧱", "1-minute plank", "Plancha de 1 minuto"))
        add(BadgeDef("first_5k", "🏃", "First 5K (3.1 mi)", "Primer 5K (3.1 mi)"))
        mileSteps.forEach { add(BadgeDef("miles_$it", "🛣️", "$it total miles", "$it millas en total")) }
    }

    fun byId(id: String) = all.firstOrNull { it.id == id }

    // Strength example: body 180 lb, best bench 185 lb → 185 ≥ 1 × 180 → "Bench 1×" earned.
    fun earned(s: BadgeStats): Set<String> {
        val out = mutableSetOf<String>()
        countSteps.filter { s.workouts >= it }.forEach { out += "count_$it" }
        streakSteps.filter { s.bestStreakWeeks >= it }.forEach { out += "streak_$it" }
        if (s.bodyLb > 0) {
            val lifts = mapOf("bench" to s.bestBenchLb, "squat" to s.bestSquatLb, "deadlift" to s.bestDeadliftLb)
            for ((k, v) in lifts) for ((x, m) in listOf("1" to 1.0, "1.5" to 1.5, "2" to 2.0)) {
                if (v >= s.bodyLb * m) out += "bw_${k}_$x"
            }
        }
        if (s.maxPullupsOneSet >= 1) out += "first_pullup"
        if (s.maxPullupsOneSet >= 10) out += "pullups_10"
        if (s.maxPushupsOneSet >= 20) out += "pushups_20"
        if (s.maxPlankSec >= 60) out += "plank_60"
        if (s.longestRunMi >= 3.1) out += "first_5k"
        mileSteps.filter { s.totalMiles >= it }.forEach { out += "miles_$it" }
        return out
    }
}
