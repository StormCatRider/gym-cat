/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — data/db/GymDao.kt                            ║
║ DAO = "Data Access Object": the list of questions (queries) and    ║
║ changes the app can ask the database. The SQL text inside @Query  ║
║ is the actual database question.                                  ║
║  Flow<…>     → a live answer that updates the screen automatically. ║
║  suspend fun → runs once in the background (doesn't freeze the UI). ║
║ Multi-step changes are wrapped in db.withTransaction in the        ║
║ Repository, so they happen all-or-nothing.                         ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    // ── Workouts
    @Insert suspend fun insertWorkout(w: WorkoutEntity): Long
    @Update suspend fun updateWorkout(w: WorkoutEntity)
    @Query("DELETE FROM workouts WHERE id = :id") suspend fun deleteWorkout(id: Long)
    @Query("SELECT * FROM workouts WHERE id = :id") suspend fun workout(id: Long): WorkoutEntity?
    @Query("SELECT * FROM workouts WHERE id = :id") fun workoutFlow(id: Long): Flow<WorkoutEntity?>
    @Query("SELECT * FROM workouts WHERE endMs > 0 ORDER BY startMs DESC") fun finishedWorkouts(): Flow<List<WorkoutEntity>>
    @Query("SELECT * FROM workouts WHERE endMs > 0 ORDER BY startMs DESC") suspend fun finishedWorkoutsNow(): List<WorkoutEntity>
    @Query("SELECT * FROM workouts WHERE endMs = 0 ORDER BY startMs DESC LIMIT 1") fun activeWorkout(): Flow<WorkoutEntity?>
    @Query("SELECT * FROM workouts WHERE endMs = 0 ORDER BY startMs DESC LIMIT 1") suspend fun activeWorkoutNow(): WorkoutEntity?

    // ── Sets
    @Insert suspend fun insertSet(s: SetEntity): Long
    @Insert suspend fun insertSets(s: List<SetEntity>)
    @Update suspend fun updateSet(s: SetEntity)
    @Update suspend fun updateSets(s: List<SetEntity>)
    @Query("DELETE FROM sets WHERE id = :id") suspend fun deleteSet(id: Long)
    @Query("DELETE FROM sets WHERE workoutId = :wid") suspend fun deleteSetsOf(wid: Long)
    @Query("SELECT * FROM sets WHERE workoutId = :wid ORDER BY exerciseOrder, setIndex") fun setsOf(wid: Long): Flow<List<SetEntity>>
    @Query("SELECT * FROM sets WHERE workoutId = :wid ORDER BY exerciseOrder, setIndex") suspend fun setsOfNow(wid: Long): List<SetEntity>
    // Every DONE set in finished workouts, oldest first (used for records, charts and badges).
    @Query("""SELECT s.* FROM sets s JOIN workouts w ON w.id = s.workoutId
              WHERE w.endMs > 0 AND s.done = 1 ORDER BY w.startMs, s.exerciseOrder, s.setIndex""")
    fun allDoneSets(): Flow<List<SetEntity>>
    @Query("""SELECT s.* FROM sets s JOIN workouts w ON w.id = s.workoutId
              WHERE w.endMs > 0 AND s.done = 1 ORDER BY w.startMs, s.exerciseOrder, s.setIndex""")
    suspend fun allDoneSetsNow(): List<SetEntity>
    // Last finished time you did an exercise (for "last time" and prefill).
    @Query("""SELECT s.* FROM sets s JOIN workouts w ON w.id = s.workoutId
              WHERE w.endMs > 0 AND s.done = 1 AND s.exerciseId = :exId
              AND w.id = (SELECT w2.id FROM workouts w2 JOIN sets s2 ON s2.workoutId = w2.id
                          WHERE w2.endMs > 0 AND s2.done = 1 AND s2.exerciseId = :exId
                          ORDER BY w2.startMs DESC LIMIT 1)
              ORDER BY s.setIndex""")
    suspend fun lastSessionSets(exId: String): List<SetEntity>

    // ── Routines
    @Query("SELECT * FROM routines ORDER BY sortOrder, id") fun routines(): Flow<List<RoutineEntity>>
    @Query("SELECT * FROM routines WHERE id = :id") suspend fun routine(id: Long): RoutineEntity?
    @Insert suspend fun insertRoutine(r: RoutineEntity): Long
    @Update suspend fun updateRoutine(r: RoutineEntity)
    @Query("DELETE FROM routines WHERE id = :id") suspend fun deleteRoutineRow(id: Long)
    @Query("SELECT * FROM routine_items WHERE routineId = :rid ORDER BY sortOrder") suspend fun routineItems(rid: Long): List<RoutineItemEntity>
    @Query("SELECT * FROM routine_items ORDER BY routineId, sortOrder") fun allRoutineItems(): Flow<List<RoutineItemEntity>>
    @Insert suspend fun insertRoutineItems(items: List<RoutineItemEntity>)
    @Query("DELETE FROM routine_items WHERE routineId = :rid") suspend fun deleteRoutineItems(rid: Long)

    // ── Custom exercises + preferences
    @Query("SELECT * FROM custom_exercises ORDER BY name") fun customExercises(): Flow<List<CustomExerciseEntity>>
    @Query("SELECT * FROM custom_exercises") suspend fun customExercisesNow(): List<CustomExerciseEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCustom(e: CustomExerciseEntity)
    @Query("DELETE FROM custom_exercises WHERE id = :id") suspend fun deleteCustom(id: String)
    @Query("SELECT * FROM exercise_prefs") fun prefs(): Flow<List<ExercisePrefEntity>>
    @Query("SELECT * FROM exercise_prefs") suspend fun prefsNow(): List<ExercisePrefEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPref(p: ExercisePrefEntity)

    // ── Body
    @Query("SELECT * FROM body_measurements ORDER BY date DESC, id DESC") fun measures(): Flow<List<MeasureEntity>>
    @Insert suspend fun insertMeasure(m: MeasureEntity): Long
    @Delete suspend fun deleteMeasure(m: MeasureEntity)
    @Query("SELECT * FROM photos ORDER BY date DESC, id DESC") fun photos(): Flow<List<PhotoEntity>>
    @Insert suspend fun insertPhoto(p: PhotoEntity): Long
    @Delete suspend fun deletePhoto(p: PhotoEntity)

    // ── Programs, badges, ladders
    @Query("SELECT * FROM program_state WHERE active = 1 LIMIT 1") fun activeProgram(): Flow<ProgramStateEntity?>
    @Query("SELECT * FROM program_state WHERE active = 1 LIMIT 1") suspend fun activeProgramNow(): ProgramStateEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertProgram(p: ProgramStateEntity)
    @Query("UPDATE program_state SET active = 0") suspend fun deactivatePrograms()
    @Query("SELECT * FROM badges ORDER BY date DESC") fun badges(): Flow<List<BadgeEntity>>
    @Query("SELECT * FROM badges") suspend fun badgesNow(): List<BadgeEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertBadges(b: List<BadgeEntity>)
    @Query("SELECT * FROM ladders") fun ladders(): Flow<List<LadderEntity>>
    @Query("SELECT * FROM ladders") suspend fun laddersNow(): List<LadderEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertLadder(l: LadderEntity)

    // ── Restore helpers (backup)
    @Query("DELETE FROM workouts") suspend fun clearWorkouts()
    @Query("DELETE FROM sets") suspend fun clearSets()
    @Query("DELETE FROM routines") suspend fun clearRoutines()
    @Query("DELETE FROM routine_items") suspend fun clearRoutineItems()
    @Query("DELETE FROM custom_exercises") suspend fun clearCustom()
    @Query("DELETE FROM exercise_prefs") suspend fun clearPrefs()
    @Query("DELETE FROM body_measurements") suspend fun clearMeasures()
    @Query("DELETE FROM photos") suspend fun clearPhotos()
    @Query("DELETE FROM program_state") suspend fun clearPrograms()
    @Query("DELETE FROM badges") suspend fun clearBadges()
    @Query("DELETE FROM ladders") suspend fun clearLadders()
    @Insert suspend fun insertWorkouts(w: List<WorkoutEntity>)
    @Insert suspend fun insertRoutines(r: List<RoutineEntity>)
    @Insert suspend fun insertMeasures(m: List<MeasureEntity>)
    @Insert suspend fun insertPhotos(p: List<PhotoEntity>)
    @Query("SELECT * FROM routines") suspend fun routinesNow(): List<RoutineEntity>
    @Query("SELECT * FROM routine_items") suspend fun allRoutineItemsNow(): List<RoutineItemEntity>
    @Query("SELECT * FROM body_measurements") suspend fun measuresNow(): List<MeasureEntity>
    @Query("SELECT * FROM photos") suspend fun photosNow(): List<PhotoEntity>
    @Query("SELECT * FROM program_state") suspend fun programsNow(): List<ProgramStateEntity>
    @Query("SELECT * FROM workouts") suspend fun allWorkoutsNow(): List<WorkoutEntity>
    @Query("SELECT * FROM sets") suspend fun allSetsNow(): List<SetEntity>
}
