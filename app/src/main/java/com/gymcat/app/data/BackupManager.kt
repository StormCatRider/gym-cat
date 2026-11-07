/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — data/BackupManager.kt                        ║
║ Backup = one .zip with CSV files (same style as Gym & Ñam:         ║
║ UTF-8 + BOM, "\r\n" line ends, dates YYYY-MM-DD) + your progress   ║
║ photos in a photos/ folder.                                        ║
║ workouts.csv starts with Gym & Ñam's exercise.csv columns          ║
║   id,date,name,emoji,minutes,kcal,source,created                   ║
║ then adds Gym Cat's extra columns.                                 ║
║ Restore REPLACES everything in Gym Cat with the backup's content.  ║
║ Weekly auto-backup (if on) saves to Downloads/GymCat.              ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.data

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.room.withTransaction
import com.gymcat.app.core.csv.Csv
import com.gymcat.app.data.db.BadgeEntity
import com.gymcat.app.data.db.CustomExerciseEntity
import com.gymcat.app.data.db.ExercisePrefEntity
import com.gymcat.app.data.db.GymDatabase
import com.gymcat.app.data.db.LadderEntity
import com.gymcat.app.data.db.MeasureEntity
import com.gymcat.app.data.db.PhotoEntity
import com.gymcat.app.data.db.ProgramStateEntity
import com.gymcat.app.data.db.RoutineEntity
import com.gymcat.app.data.db.RoutineItemEntity
import com.gymcat.app.data.db.SetEntity
import com.gymcat.app.data.db.WorkoutEntity
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(private val context: Context, private val db: GymDatabase) {
    private val dao = db.dao()
    val photoDir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    private fun d(x: Double?) = x?.toString() ?: ""
    private fun b(x: Boolean) = if (x) "1" else "0"

    /** Writes the whole backup zip into [out]. */
    suspend fun export(out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            fun put(name: String, text: String) {
                zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray(Charsets.UTF_8)); zip.closeEntry()
            }
            put("workouts.csv", Csv.write(
                listOf("id", "date", "name", "emoji", "minutes", "kcal", "source", "created",
                    "start_ms", "end_ms", "kind", "routine_id", "program_id", "program_day", "rating", "notes",
                    "miles", "route", "met", "hc_record_id", "deload"),
                dao.allWorkoutsNow().filter { it.endMs > 0 }.map { w ->
                    val emoji = when (w.kind) { "CARDIO" -> "🏃"; "STRETCH" -> "🧘"; "CLASS" -> "🤸"; else -> "🏋️" }
                    listOf(w.id.toString(), Csv.dateOf(w.date), w.title, emoji,
                        "%.1f".format(java.util.Locale.US, (w.endMs - w.startMs) / 60_000.0),
                        "%.1f".format(java.util.Locale.US, w.kcal), "gymcat", w.created.toString(),
                        w.startMs.toString(), w.endMs.toString(), w.kind, w.routineId?.toString() ?: "",
                        w.programId ?: "", w.programDay.toString(), w.rating.toString(), w.notes,
                        w.miles.toString(), w.route, w.met.toString(), w.hcRecordId, b(w.deload))
                }))
            val finishedIds = dao.allWorkoutsNow().filter { it.endMs > 0 }.map { it.id }.toSet()
            put("sets.csv", Csv.write(
                listOf("id", "workout_id", "exercise_id", "exercise_order", "set_index", "type", "lb", "reps", "seconds",
                    "miles", "rpe", "note", "superset", "done", "target_min", "target_max", "target_rpe", "rest_sec", "created"),
                dao.allSetsNow().filter { it.workoutId in finishedIds }.map { s ->
                    listOf(s.id.toString(), s.workoutId.toString(), s.exerciseId, s.exerciseOrder.toString(), s.setIndex.toString(),
                        s.type, s.lb.toString(), s.reps.toString(), s.seconds.toString(), s.miles.toString(), d(s.rpe), s.note,
                        s.superset.toString(), b(s.done), s.targetMin.toString(), s.targetMax.toString(), d(s.targetRpe),
                        s.restSec.toString(), s.created.toString())
                }))
            put("exercises.csv", Csv.write(
                listOf("id", "name", "type", "equipment", "primary", "secondary", "mode", "how_to", "mistakes",
                    "bw_pct", "compound", "emoji", "met", "copied_from", "created"),
                dao.customExercisesNow().map { e ->
                    listOf(e.id, e.name, e.type, e.equipment, e.primary, e.secondary, e.mode, e.howTo, e.mistakes,
                        e.bwPct.toString(), b(e.compound), e.emoji, e.met.toString(), e.copiedFrom, e.created.toString())
                }))
            put("exercise_prefs.csv", Csv.write(listOf("exercise_id", "favorite", "rest_sec"),
                dao.prefsNow().map { listOf(it.exerciseId, b(it.favorite), it.restSec.toString()) }))
            put("routines.csv", Csv.write(listOf("id", "name", "notes", "sort_order", "created"),
                dao.routinesNow().map { listOf(it.id.toString(), it.name, it.notes, it.sortOrder.toString(), it.created.toString()) }))
            put("routine_items.csv", Csv.write(
                listOf("id", "routine_id", "sort_order", "exercise_id", "sets", "rep_min", "rep_max", "target_rpe",
                    "rest_sec", "warmups", "superset", "hold_sec", "minutes"),
                dao.allRoutineItemsNow().map { i ->
                    listOf(i.id.toString(), i.routineId.toString(), i.sortOrder.toString(), i.exerciseId, i.sets.toString(),
                        i.repMin.toString(), i.repMax.toString(), d(i.targetRpe), i.restSec.toString(), b(i.warmups),
                        i.superset.toString(), i.holdSec.toString(), i.minutes.toString())
                }))
            put("body_measurements.csv", Csv.write(
                listOf("id", "date", "neck_in", "chest_in", "arm_in", "waist_in", "hip_in", "thigh_in", "body_fat_pct", "created"),
                dao.measuresNow().map { m ->
                    listOf(m.id.toString(), Csv.dateOf(m.date), d(m.neck), d(m.chest), d(m.arm), d(m.waist), d(m.hip),
                        d(m.thigh), d(m.bodyFat), m.created.toString())
                }))
            val photos = dao.photosNow()
            put("photos.csv", Csv.write(listOf("id", "date", "file", "pose", "created"),
                photos.map { listOf(it.id.toString(), Csv.dateOf(it.date), it.fileName, it.pose, it.created.toString()) }))
            for (p in photos) {
                val f = File(photoDir, p.fileName)
                if (f.exists()) { zip.putNextEntry(ZipEntry("photos/${p.fileName}")); f.inputStream().use { it.copyTo(zip) }; zip.closeEntry() }
            }
            put("programs.csv", Csv.write(listOf("program_id", "next_day", "active", "started", "last_deload", "last_skip"),
                dao.programsNow().map { listOf(it.programId, it.nextDay.toString(), b(it.active), Csv.dateOf(it.started),
                    Csv.dateOf(it.lastDeload), if (it.lastSkip > 0) Csv.dateOf(it.lastSkip) else "") }))
            put("badges.csv", Csv.write(listOf("id", "date"), dao.badgesNow().map { listOf(it.id, Csv.dateOf(it.date)) }))
            put("ladders.csv", Csv.write(listOf("ladder_id", "step"), dao.laddersNow().map { listOf(it.ladderId, it.step.toString()) }))
        }
    }

    /** Reads a backup zip and REPLACES Gym Cat's data. Returns false if the file isn't a Gym Cat backup. */
    suspend fun restore(input: InputStream): Boolean {
        val files = mutableMapOf<String, String>()
        val photoBytes = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                val name = e.name
                if (!name.contains("..")) {                      // safety: no paths outside our folder
                    if (name.startsWith("photos/")) photoBytes[name.removePrefix("photos/")] = zip.readBytes()
                    else if (name.endsWith(".csv")) files[name] = zip.readBytes().toString(Charsets.UTF_8)
                }
                e = zip.nextEntry
            }
        }
        if ("workouts.csv" !in files || "sets.csv" !in files) return false

        fun rows(name: String): List<Map<String, String>> {
            val all = Csv.parse(files[name] ?: return emptyList())
            if (all.isEmpty()) return emptyList()
            val h = all.first()
            return all.drop(1).filter { r -> r.any { it.isNotBlank() } }.map { r -> h.indices.associate { h[it] to (r.getOrNull(it) ?: "") } }
        }
        fun Map<String, String>.s(k: String) = this[k] ?: ""
        fun Map<String, String>.l(k: String) = s(k).toLongOrNull() ?: 0L
        fun Map<String, String>.i(k: String) = s(k).toIntOrNull() ?: 0
        fun Map<String, String>.dbl(k: String) = s(k).toDoubleOrNull() ?: 0.0
        fun Map<String, String>.dn(k: String) = s(k).toDoubleOrNull()
        fun Map<String, String>.bool(k: String) = s(k) == "1" || s(k).equals("true", true)
        fun Map<String, String>.day(k: String) = runCatching { Csv.epochDayOf(s(k)) }.getOrDefault(0L)

        db.withTransaction {
            dao.clearSets(); dao.clearWorkouts(); dao.clearRoutineItems(); dao.clearRoutines(); dao.clearCustom()
            dao.clearPrefs(); dao.clearMeasures(); dao.clearPhotos(); dao.clearPrograms(); dao.clearBadges(); dao.clearLadders()
            dao.insertWorkouts(rows("workouts.csv").map { r ->
                WorkoutEntity(id = r.l("id"), date = r.day("date"), startMs = r.l("start_ms"), endMs = r.l("end_ms"),
                    title = r.s("name"), kind = r.s("kind").ifBlank { "STRENGTH" }, routineId = r.s("routine_id").toLongOrNull(),
                    programId = r.s("program_id").ifBlank { null }, programDay = r.s("program_day").toIntOrNull() ?: -1,
                    rating = r.i("rating"), notes = r.s("notes"), kcal = r.dbl("kcal"), miles = r.dbl("miles"),
                    route = r.s("route"), met = r.dbl("met"), hcRecordId = r.s("hc_record_id"), deload = r.bool("deload"),
                    created = r.l("created"))
            })
            dao.insertSets(rows("sets.csv").map { r ->
                SetEntity(id = r.l("id"), workoutId = r.l("workout_id"), exerciseId = r.s("exercise_id"),
                    exerciseOrder = r.i("exercise_order"), setIndex = r.i("set_index"), type = r.s("type").ifBlank { "WORKING" },
                    lb = r.dbl("lb"), reps = r.i("reps"), seconds = r.i("seconds"), miles = r.dbl("miles"), rpe = r.dn("rpe"),
                    note = r.s("note"), superset = r.i("superset"), done = r.bool("done"), targetMin = r.i("target_min"),
                    targetMax = r.i("target_max"), targetRpe = r.dn("target_rpe"), restSec = r.i("rest_sec"), created = r.l("created"))
            })
            rows("exercises.csv").forEach { r ->
                dao.upsertCustom(CustomExerciseEntity(r.s("id"), r.s("name"), r.s("type"), r.s("equipment"), r.s("primary"),
                    r.s("secondary"), r.s("mode"), r.s("how_to"), r.s("mistakes"), r.dbl("bw_pct"), r.bool("compound"),
                    r.s("emoji"), r.dbl("met"), r.s("copied_from"), r.l("created")))
            }
            rows("exercise_prefs.csv").forEach { r -> dao.upsertPref(ExercisePrefEntity(r.s("exercise_id"), r.bool("favorite"), r.i("rest_sec"))) }
            dao.insertRoutines(rows("routines.csv").map { r -> RoutineEntity(r.l("id"), r.s("name"), r.s("notes"), r.i("sort_order"), r.l("created")) })
            dao.insertRoutineItems(rows("routine_items.csv").map { r ->
                RoutineItemEntity(r.l("id"), r.l("routine_id"), r.i("sort_order"), r.s("exercise_id"), r.i("sets"), r.i("rep_min"),
                    r.i("rep_max"), r.dn("target_rpe"), r.i("rest_sec"), r.bool("warmups"), r.i("superset"), r.i("hold_sec"), r.i("minutes"))
            })
            dao.insertMeasures(rows("body_measurements.csv").map { r ->
                MeasureEntity(r.l("id"), r.day("date"), r.dn("neck_in"), r.dn("chest_in"), r.dn("arm_in"), r.dn("waist_in"),
                    r.dn("hip_in"), r.dn("thigh_in"), r.dn("body_fat_pct"), r.l("created"))
            })
            dao.insertPhotos(rows("photos.csv").map { r -> PhotoEntity(r.l("id"), r.day("date"), r.s("file"), r.s("pose"), r.l("created")) })
            rows("programs.csv").forEach { r ->
                dao.upsertProgram(ProgramStateEntity(r.s("program_id"), r.i("next_day"), r.bool("active"), r.day("started"), r.day("last_deload"),
                    if (r.s("last_skip").isBlank()) 0 else r.day("last_skip")))
            }
            dao.insertBadges(rows("badges.csv").map { r -> BadgeEntity(r.s("id"), r.day("date")) })
            rows("ladders.csv").forEach { r -> dao.upsertLadder(LadderEntity(r.s("ladder_id"), r.i("step"))) }
        }
        for ((name, bytes) in photoBytes) if (!name.contains('/')) File(photoDir, name).writeBytes(bytes)
        return true
    }

    fun fileName(): String = "GymCat-backup-${java.time.LocalDate.now()}.zip"

    /** Weekly auto-backup: Downloads/GymCat/GymCat-backup-YYYY-MM-DD.zip (no permission needed on Android 10+). */
    suspend fun autoBackupToDownloads(): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName())
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/GymCat")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        return runCatching { context.contentResolver.openOutputStream(uri)?.use { export(it) }; true }.getOrDefault(false)
    }
}
