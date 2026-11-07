/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — core/model/Models.kt                         ║
║ The "vocabulary" of the app: lists of choices (enums) and the      ║
║ shape of an exercise. Pure Kotlin: NO Android imports, so this     ║
║ file can move to Kotlin Multiplatform (iOS) later unchanged.       ║
║                                                                    ║
║ enum class  = a fixed list of options (like a drop-down).          ║
║ data class  = a bundle of named values (like one row of a table).  ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.core.model

// Muscles drawn on the body map (front + back figure).
enum class Muscle {
    CHEST, LATS, UPPER_BACK, TRAPS, FRONT_DELTS, SIDE_DELTS, REAR_DELTS,
    BICEPS, TRICEPS, FOREARMS, ABS, OBLIQUES, LOWER_BACK,
    GLUTES, QUADS, HAMSTRINGS, CALVES, ADDUCTORS
}

// What you need to do the exercise.
enum class Equipment {
    BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT, BAND, KETTLEBELL,
    SMITH, EZ_BAR, TRAP_BAR, PLATE, OTHER, NONE
}

// Big families used for filtering the library.
enum class ExerciseType { STRENGTH, CALISTHENICS, CARDIO, STRETCH }

// How a set is typed in:
//  WEIGHT_REPS     → pounds × reps (bench press)
//  BODYWEIGHT_REPS → reps, plus optional added (+lb) or assistance (−lb) (pull-up)
//  HOLD            → seconds, plus optional added weight (plank)
//  CARDIO          → minutes + miles (treadmill)
//  STRETCH         → seconds held (hamstring stretch)
enum class LogMode { WEIGHT_REPS, BODYWEIGHT_REPS, HOLD, CARDIO, STRETCH }

// Kind of set (shown as a small chip on the set card).
enum class SetType { WARMUP, WORKING, DROP, FAILURE }

// Kind of whole workout.
enum class WorkoutKind { STRENGTH, CARDIO, STRETCH, CLASS }

// Onboarding answers.
enum class Goal { STRENGTH, MUSCLE, FAT_LOSS, GENERAL }
enum class Level { BEGINNER, INTERMEDIATE, ADVANCED }
enum class EquipmentAccess { FULL_GYM, HOME_DUMBBELLS, BODYWEIGHT }
enum class Sex { MALE, FEMALE }

/**
 * One exercise from the library (built-in or your own).
 *  bwPct  = share of body weight you lift in bodyweight moves
 *           (pull-up ≈ 1.00, push-up ≈ 0.64 — Ebben et al. 2011). 0 = don't count body weight.
 *  met    = calorie intensity number (Compendium of Physical Activities).
 *  catArt = name of the Gym Cat drawing (e.g. "cat_17_squat"), or null.
 */
data class Exercise(
    val id: String,
    val nameEn: String,
    val nameEs: String,
    val type: ExerciseType,
    val equipment: Equipment,
    val primary: List<Muscle>,
    val secondary: List<Muscle>,
    val mode: LogMode,
    val howToEn: String,
    val howToEs: String,
    val mistakesEn: String,
    val mistakesEs: String,
    val bwPct: Double,
    val compound: Boolean,
    val emoji: String,
    val catArt: String?,
    val met: Double,
    val custom: Boolean = false,
) {
    // Picks the Spanish text when the phone is in Spanish (falls back to English).
    fun name(es: Boolean) = if (es && nameEs.isNotBlank()) nameEs else nameEn
    fun howTo(es: Boolean) = if (es && howToEs.isNotBlank()) howToEs else howToEn
    fun mistakes(es: Boolean) = if (es && mistakesEs.isNotBlank()) mistakesEs else mistakesEn
}
