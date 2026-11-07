/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — data/db/Entities.kt                          ║
║ The TABLES of the on-phone database (Room). Each data class = one  ║
║ table, each property = one column.                                 ║
║   @Entity     → "this is a table"                                  ║
║   @PrimaryKey → the unique id of each row                          ║
║ Conventions shared with Gym & Ñam:                                 ║
║   date = epochDay (days since 1970-01-01), times = epoch ms,       ║
║   weights in lb, distances in miles, body sizes in inches.         ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One workout (lifting session, run, stretch session or class). endMs = 0 → still in progress. */
@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val startMs: Long,
    val endMs: Long = 0,
    val title: String,
    val kind: String,                 // WorkoutKind name
    val routineId: Long? = null,
    val programId: String? = null,
    val programDay: Int = -1,
    val rating: Int = 0,              // 1–5 "how did it feel"
    val notes: String = "",
    val kcal: Double = 0.0,
    val miles: Double = 0.0,
    val route: String = "",           // GPS route (GeoMath.encode)
    val met: Double = 0.0,            // for classes/sports
    val hcRecordId: String = "",      // our own Health Connect record (so we can delete only ours)
    val deload: Boolean = false,
    val created: Long = System.currentTimeMillis(),
)

/** One set inside a workout. lb = weight (or ±added/assist for bodyweight moves). */
@Entity(tableName = "sets", indices = [Index("workoutId"), Index("exerciseId")])
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: String,
    val exerciseOrder: Int,           // position of the exercise in the workout
    val setIndex: Int,                // 1st, 2nd… set of that exercise
    val type: String = "WORKING",     // SetType name
    val lb: Double = 0.0,
    val reps: Int = 0,
    val seconds: Int = 0,
    val miles: Double = 0.0,
    val rpe: Double? = null,
    val note: String = "",
    val superset: Int = 0,            // same number = alternated together (0 = none)
    val done: Boolean = false,
    val targetMin: Int = 0,
    val targetMax: Int = 0,
    val targetRpe: Double? = null,
    val restSec: Int = 0,
    val created: Long = System.currentTimeMillis(),
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String = "",
    val sortOrder: Int = 0,
    val created: Long = System.currentTimeMillis(),
)

@Entity(tableName = "routine_items", indices = [Index("routineId")])
data class RoutineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val sortOrder: Int,
    val exerciseId: String,
    val sets: Int = 3,
    val repMin: Int = 8,
    val repMax: Int = 12,
    val targetRpe: Double? = null,
    val restSec: Int = 0,             // 0 = default for the exercise type
    val warmups: Boolean = false,
    val superset: Int = 0,
    val holdSec: Int = 0,
    val minutes: Int = 0,
)

/** Your own exercises (new ones, or edited copies of built-ins). */
@Entity(tableName = "custom_exercises")
data class CustomExerciseEntity(
    @PrimaryKey val id: String,       // "c_…"
    val name: String,
    val type: String,
    val equipment: String,
    val primary: String,              // muscles, comma-separated
    val secondary: String,
    val mode: String,
    val howTo: String,
    val mistakes: String,
    val bwPct: Double,
    val compound: Boolean,
    val emoji: String,
    val met: Double,
    val copiedFrom: String = "",
    val created: Long = System.currentTimeMillis(),
)

/** Favorite star + your own rest time per exercise. */
@Entity(tableName = "exercise_prefs")
data class ExercisePrefEntity(
    @PrimaryKey val exerciseId: String,
    val favorite: Boolean = false,
    val restSec: Int = 0,             // 0 = default
)

@Entity(tableName = "body_measurements")
data class MeasureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val neck: Double? = null,
    val chest: Double? = null,
    val arm: Double? = null,
    val waist: Double? = null,
    val hip: Double? = null,
    val thigh: Double? = null,
    val bodyFat: Double? = null,      // Navy method %, saved when it could be computed
    val created: Long = System.currentTimeMillis(),
)

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val fileName: String,             // inside the app's private photos folder
    val pose: String = "front",
    val created: Long = System.currentTimeMillis(),
)

/** Which built-in program you follow and which day is next. */
@Entity(tableName = "program_state")
data class ProgramStateEntity(
    @PrimaryKey val programId: String,
    val nextDay: Int = 0,
    val active: Boolean = true,
    val started: Long,                // epochDay
    val lastDeload: Long,             // epochDay of the last deload (or start)
    val lastSkip: Long = 0,           // epochDay you last skipped a missed day (hides the "missed" card)
)

@Entity(tableName = "badges")
data class BadgeEntity(@PrimaryKey val id: String, val date: Long)

@Entity(tableName = "ladders")
data class LadderEntity(@PrimaryKey val ladderId: String, val step: Int = 0)
