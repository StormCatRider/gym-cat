/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — data/db/GymDatabase.kt                       ║
║ Creates the database file "gymcat.db" on the phone and lists all   ║
║ its tables. version = 1 (first release). When a column is added    ║
║ later, the version goes up and a "migration" keeps your data.      ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkoutEntity::class, SetEntity::class, RoutineEntity::class, RoutineItemEntity::class,
        CustomExerciseEntity::class, ExercisePrefEntity::class, MeasureEntity::class, PhotoEntity::class,
        ProgramStateEntity::class, BadgeEntity::class, LadderEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun dao(): GymDao

    companion object {
        fun create(context: Context): GymDatabase =
            Room.databaseBuilder(context, GymDatabase::class.java, "gymcat.db").build()
    }
}
