/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — data/ExerciseLibrary.kt                      ║
║ Loads the 300+ built-in exercises from assets/exercises.json once, ║
║ then mixes in your own (custom) exercises from the database.       ║
║ "lookup(id)" finds any exercise by its id in an instant.           ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.data

import android.content.Context
import com.gymcat.app.core.model.Equipment
import com.gymcat.app.core.model.Exercise
import com.gymcat.app.core.model.ExerciseType
import com.gymcat.app.core.model.LogMode
import com.gymcat.app.core.model.Muscle
import com.gymcat.app.data.db.CustomExerciseEntity
import org.json.JSONArray

class ExerciseLibrary(context: Context) {

    // Read the JSON file once (lazy = only the first time it's needed).
    val builtIn: List<Exercise> by lazy {
        val text = context.assets.open("exercises.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(text)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            fun muscles(key: String) = o.getJSONArray(key).let { a -> (0 until a.length()).map { Muscle.valueOf(a.getString(it)) } }
            Exercise(
                id = o.getString("id"),
                nameEn = o.getString("nameEn"), nameEs = o.getString("nameEs"),
                type = ExerciseType.valueOf(o.getString("type")),
                equipment = Equipment.valueOf(o.getString("equipment")),
                primary = muscles("primary"), secondary = muscles("secondary"),
                mode = LogMode.valueOf(o.getString("mode")),
                howToEn = o.getString("howToEn"), howToEs = o.getString("howToEs"),
                mistakesEn = o.getString("mistakesEn"), mistakesEs = o.getString("mistakesEs"),
                bwPct = o.getDouble("bwPct"), compound = o.getBoolean("compound"),
                emoji = o.getString("emoji"),
                catArt = if (o.isNull("catArt")) null else o.getString("catArt"),
                met = o.getDouble("met"),
            )
        }
    }

    private val builtInById by lazy { builtIn.associateBy { it.id } }
    @Volatile private var customById: Map<String, Exercise> = emptyMap()

    /** Called whenever the custom-exercise table changes. */
    fun setCustom(list: List<CustomExerciseEntity>) {
        customById = list.associate { it.id to it.toExercise() }
    }

    fun all(): List<Exercise> = builtIn + customById.values
    fun lookup(id: String): Exercise? = customById[id] ?: builtInById[id]

    companion object {
        fun CustomExerciseEntity.toExercise() = Exercise(
            id = id, nameEn = name, nameEs = name,
            type = runCatching { ExerciseType.valueOf(type) }.getOrDefault(ExerciseType.STRENGTH),
            equipment = runCatching { Equipment.valueOf(equipment) }.getOrDefault(Equipment.OTHER),
            primary = primary.split(',').filter { it.isNotBlank() }.mapNotNull { runCatching { Muscle.valueOf(it) }.getOrNull() },
            secondary = secondary.split(',').filter { it.isNotBlank() }.mapNotNull { runCatching { Muscle.valueOf(it) }.getOrNull() },
            mode = runCatching { LogMode.valueOf(mode) }.getOrDefault(LogMode.WEIGHT_REPS),
            howToEn = howTo, howToEs = howTo, mistakesEn = mistakes, mistakesEs = mistakes,
            bwPct = bwPct, compound = compound, emoji = emoji, catArt = null, met = met, custom = true,
        )
    }
}
