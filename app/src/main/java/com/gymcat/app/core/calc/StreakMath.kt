/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — core/calc/StreakMath.kt                      ║
║ Weekly streak with earned freezes (your choice):                   ║
║  • Weeks run Monday → Sunday.                                      ║
║  • A week "counts" when you train on at least your weekly goal     ║
║    number of DAYS (two workouts on one day = one day).             ║
║  • Each week you hit the goal you EARN a freeze (max 2 saved).     ║
║  • A missed week uses a freeze automatically, so the streak lives. ║
║    No freeze left → streak goes back to 0.                         ║
║  • The current week adds +1 as soon as its goal is reached.        ║
║ Dates are "epochDay" = days since Jan 1 1970 (shared convention).  ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.core.calc

object StreakMath {
    const val MAX_FREEZES = 2

    data class State(
        val streakWeeks: Int,        // weeks in a row (frozen weeks don't add, but don't break it)
        val bestStreak: Int,
        val freezes: Int,            // freezes saved for later
        val thisWeekDays: Int,       // training days so far this week
        val goal: Int,
        val weekStart: Long,         // epochDay of this Monday
        val frozenWeeks: List<Long>, // Mondays saved by a freeze
    )

    // Jan 1 1970 was a THURSDAY. Day-of-week with Monday = 0:
    //   dow = (epochDay + 3) mod 7
    // Example: epochDay 0 (Thu) → (0+3) mod 7 = 3 → Thursday ✔
    fun dayOfWeekMon0(epochDay: Long): Int = (epochDay + 3).mod(7L).toInt()
    fun weekStart(epochDay: Long): Long = epochDay - dayOfWeekMon0(epochDay)

    fun compute(workoutDays: Collection<Long>, goal: Int, today: Long): State {
        val g = goal.coerceIn(1, 7)
        val current = weekStart(today)
        if (workoutDays.isEmpty()) return State(0, 0, 0, 0, g, current, emptyList())
        // Count DISTINCT training days per week.
        val perWeek = workoutDays.distinct().groupingBy { weekStart(it) }.eachCount()
        var streak = 0; var best = 0; var freezes = 0
        val frozen = mutableListOf<Long>()
        var w = weekStart(workoutDays.min())
        while (w < current) {                 // finished weeks only
            val c = perWeek[w] ?: 0
            when {
                c >= g -> { streak++; freezes = minOf(freezes + 1, MAX_FREEZES) }
                streak > 0 && freezes > 0 -> { freezes--; frozen += w }
                else -> streak = 0
            }
            best = maxOf(best, streak)
            w += 7
        }
        val thisWeek = perWeek[current] ?: 0
        if (thisWeek >= g) streak++
        best = maxOf(best, streak)
        return State(streak, best, freezes, thisWeek, g, current, frozen)
    }
}
