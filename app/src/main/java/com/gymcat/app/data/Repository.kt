/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — data/Repository.kt                           ║
║ The "manager" between the screens and the database. Screens never  ║
║ talk to the database directly; they call functions here, like:     ║
║   startRoutine(…)   → creates a workout with pre-filled sets       ║
║   finishWorkout(…)  → calories, records, badges, ladders,          ║
║                       Health Connect, program's next day           ║
║   saveCardio(…)     → stores a run/ride/row                        ║
║ Formulas themselves live in core/calc (pure Kotlin).               ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.data

import androidx.room.withTransaction
import com.gymcat.app.core.calc.Badges
import com.gymcat.app.core.calc.BadgeStats
import com.gymcat.app.core.calc.BodyMath
import com.gymcat.app.core.calc.CalorieMath
import com.gymcat.app.core.calc.StreakMath
import com.gymcat.app.core.calc.StrengthMath
import com.gymcat.app.core.model.Exercise
import com.gymcat.app.core.model.ExerciseType
import com.gymcat.app.core.model.LogMode
import com.gymcat.app.core.model.SetType
import com.gymcat.app.core.model.WorkoutKind
import com.gymcat.app.core.programs.Programs
import com.gymcat.app.core.programs.TemplateItem
import com.gymcat.app.data.db.BadgeEntity
import com.gymcat.app.data.db.GymDatabase
import com.gymcat.app.data.db.LadderEntity
import com.gymcat.app.data.db.ProgramStateEntity
import com.gymcat.app.data.db.RoutineItemEntity
import com.gymcat.app.data.db.SetEntity
import com.gymcat.app.data.db.WorkoutEntity
import java.time.LocalDate
import kotlin.math.max

/** One planned exercise when starting a workout (from a routine, a program day, or "add exercise"). */
data class PlanItem(
    val exerciseId: String,
    val sets: Int = 3,
    val repMin: Int = 8,
    val repMax: Int = 12,
    val rpe: Double? = null,
    val restSec: Int = 0,
    val warmups: Boolean = false,
    val holdSec: Int = 0,
    val minutes: Int = 0,
    val superset: Int = 0,
)

/** A personal record hit in the workout that just finished. kind: weight | e1rm | reps | distance | pace */
data class PrHit(val exerciseId: String, val kind: String, val value: Double)

data class FinishResult(
    val workoutId: Long,
    val minutes: Double,
    val kcal: Double,
    val volumeLb: Double,
    val setsDone: Int,
    val prs: List<PrHit>,
    val newBadges: List<String>,
    val ladderUps: List<Pair<String, Int>>,   // ladderId → next step index
    val proteinG: Double?,
)

fun today(): Long = LocalDate.now().toEpochDay()

class Repository(
    private val db: GymDatabase,
    val library: ExerciseLibrary,
    val settings: SettingsStore,
    val health: HealthConnectManager,
) {
    val dao = db.dao()

    // ───────────────────────── helpers ─────────────────────────
    fun ex(id: String): Exercise? = library.lookup(id)

    /** Default rest by exercise type: compound 2:30, isolation 1:30, calisthenics 1:30 (editable). */
    suspend fun restFor(e: Exercise, planRest: Int): Int {
        if (planRest > 0) return planRest
        val pref = dao.prefsNow().firstOrNull { it.exerciseId == e.id }?.restSec ?: 0
        if (pref > 0) return pref
        val s = settings.current()
        return when {
            e.mode == LogMode.CARDIO || e.mode == LogMode.STRETCH -> 0
            e.type == ExerciseType.CALISTHENICS -> s.restCalisthenics
            e.compound -> s.restCompound
            else -> s.restIsolation
        }
    }

    /** Plate step in lb: 5 lb, or 2.5 kg (= 5.51 lb) when the app shows kg. */
    suspend fun incrementLb(): Double = if (settings.current().metric) BodyMath.kgToLb(2.5) else 5.0

    /** "ladder:pushup" → the exercise of the step you're on. */
    suspend fun resolveLadder(item: TemplateItem): TemplateItem {
        if (!item.exerciseId.startsWith("ladder:")) return item
        val ladder = Programs.ladder(item.exerciseId.removePrefix("ladder:")) ?: return item
        val step = dao.laddersNow().firstOrNull { it.ladderId == ladder.id }?.step ?: 0
        val s = ladder.steps[step.coerceIn(0, ladder.steps.lastIndex)]
        return if (s.holdSec > 0) item.copy(exerciseId = s.exerciseId, holdSec = s.holdSec, repMin = 0, repMax = 0)
        else item.copy(exerciseId = s.exerciseId, repMin = minOf(item.repMin.takeIf { it > 0 } ?: s.reps, s.reps), repMax = s.reps)
    }

    fun TemplateItem.toPlan() = PlanItem(exerciseId, sets, repMin, repMax, rpe, restSec, warmups, holdSec, minutes)
    fun RoutineItemEntity.toPlan() = PlanItem(exerciseId, sets, repMin, repMax, targetRpe, restSec, warmups, holdSec, minutes, superset)

    // ───────────────────────── starting a workout ─────────────────────────
    suspend fun startWorkout(
        title: String, plan: List<PlanItem>, routineId: Long? = null,
        programId: String? = null, programDay: Int = -1, deload: Boolean = false,
    ): Long {
        dao.activeWorkoutNow()?.let { return it.id }          // only one workout at a time
        val kind = when {
            plan.isNotEmpty() && plan.all { ex(it.exerciseId)?.mode == LogMode.STRETCH } -> WorkoutKind.STRETCH
            plan.isNotEmpty() && plan.all { ex(it.exerciseId)?.mode == LogMode.CARDIO } -> WorkoutKind.CARDIO
            else -> WorkoutKind.STRENGTH
        }
        val now = System.currentTimeMillis()
        val wid = dao.insertWorkout(
            WorkoutEntity(date = today(), startMs = now, title = title, kind = kind.name,
                routineId = routineId, programId = programId, programDay = programDay, deload = deload)
        )
        plan.forEachIndexed { order, p -> dao.insertSets(buildSets(wid, order, p, deload)) }
        return wid
    }

    /**
     * Makes the set rows for one exercise, pre-filled from last time.
     * Warm-ups (if asked): 50% × 8, 70% × 5, 85% × 3 of the first working weight.
     * Deload: weight × 0.9 and half the sets.
     */
    private suspend fun buildSets(wid: Long, order: Int, p: PlanItem, deload: Boolean): List<SetEntity> {
        val e = ex(p.exerciseId) ?: return emptyList()
        val last = dao.lastSessionSets(e.id).filter { it.type != SetType.WARMUP.name }
        val rest = restFor(e, p.restSec)
        val inc = incrementLb()
        var count = when (e.mode) { LogMode.CARDIO, LogMode.STRETCH -> 1; else -> max(1, p.sets) }
        if (deload) count = StrengthMath.deloadSets(count)
        val out = mutableListOf<SetEntity>()
        val firstLb = last.firstOrNull()?.lb ?: 0.0
        if (p.warmups && e.mode == LogMode.WEIGHT_REPS && firstLb > 0) {
            val bar = if (settings.current().plateKg) BodyMath.kgToLb(20.0) else 45.0
            StrengthMath.warmups(if (deload) StrengthMath.deloadWeight(firstLb, inc) else firstLb, bar, inc).forEach { w ->
                out += SetEntity(workoutId = wid, exerciseId = e.id, exerciseOrder = order, setIndex = 0,
                    type = SetType.WARMUP.name, lb = w.lb, reps = w.reps, superset = p.superset, restSec = 60)
            }
        }
        for (i in 0 until count) {
            val prev = last.getOrNull(i) ?: last.lastOrNull()
            var lb = prev?.lb ?: 0.0
            if (deload && e.mode == LogMode.WEIGHT_REPS) lb = StrengthMath.deloadWeight(lb, inc)
            out += SetEntity(
                workoutId = wid, exerciseId = e.id, exerciseOrder = order, setIndex = 0,
                lb = lb,
                reps = prev?.reps ?: p.repMax.takeIf { it > 0 } ?: 0,
                seconds = when (e.mode) {
                    LogMode.HOLD, LogMode.STRETCH -> prev?.seconds?.takeIf { it > 0 } ?: p.holdSec.takeIf { it > 0 } ?: 30
                    LogMode.CARDIO -> (prev?.seconds?.takeIf { it > 0 } ?: (p.minutes.takeIf { it > 0 } ?: 20) * 60)
                    else -> 0
                },
                miles = if (e.mode == LogMode.CARDIO) prev?.miles ?: 0.0 else 0.0,
                superset = p.superset, targetMin = p.repMin, targetMax = p.repMax, targetRpe = p.rpe, restSec = rest,
            )
        }
        return out.mapIndexed { i, s -> s.copy(setIndex = i + 1) }
    }

    suspend fun planFromRoutine(rid: Long): List<PlanItem> = dao.routineItems(rid).map { it.toPlan() }

    suspend fun startRoutine(rid: Long, deload: Boolean = false): Long {
        val r = dao.routine(rid) ?: return -1
        return startWorkout(r.name, planFromRoutine(rid), routineId = rid, deload = deload)
    }

    suspend fun startProgramDay(es: Boolean, deload: Boolean = false): Long {
        val st = dao.activeProgramNow() ?: return -1
        val prog = Programs.byId(st.programId) ?: return -1
        val dayIdx = st.nextDay % prog.days.size
        val day = prog.days[dayIdx]
        val plan = day.items.map { resolveLadder(it).toPlan() }
        val title = prog.name(es) + " · " + (if (es) day.nameEs else day.nameEn)
        return startWorkout(title, plan, programId = prog.id, programDay = dayIdx, deload = deload)
    }

    suspend fun skipProgramDay() {
        val st = dao.activeProgramNow() ?: return
        val prog = Programs.byId(st.programId) ?: return
        dao.upsertProgram(st.copy(nextDay = (st.nextDay + 1) % prog.days.size, lastSkip = today()))
    }

    suspend fun activateProgram(programId: String) {
        dao.deactivatePrograms()
        dao.upsertProgram(ProgramStateEntity(programId, 0, true, today(), today()))
    }

    suspend fun stopProgram() = dao.deactivatePrograms()

    // ───────────────────────── editing during a workout ─────────────────────────
    suspend fun addExercise(wid: Long, exId: String) {
        val order = (dao.setsOfNow(wid).maxOfOrNull { it.exerciseOrder } ?: -1) + 1
        dao.insertSets(buildSets(wid, order, PlanItem(exId), false))
    }

    suspend fun addSet(wid: Long, order: Int) {
        val sets = dao.setsOfNow(wid).filter { it.exerciseOrder == order }
        val base = sets.lastOrNull() ?: return
        dao.insertSet(base.copy(id = 0, setIndex = sets.size + 1, done = false, rpe = null, note = "",
            type = SetType.WORKING.name, created = System.currentTimeMillis()))
    }

    suspend fun updateSet(s: SetEntity) = dao.updateSet(s)
    suspend fun deleteSet(s: SetEntity) {
        dao.deleteSet(s.id)
        renumber(s.workoutId, s.exerciseOrder)
    }

    private suspend fun renumber(wid: Long, order: Int) {
        val rest = dao.setsOfNow(wid).filter { it.exerciseOrder == order }
        dao.updateSets(rest.mapIndexed { i, x -> x.copy(setIndex = i + 1) })
    }

    /** Machine busy? Swap to another exercise; done sets stay, the rest get new pre-fills. */
    suspend fun swapExercise(wid: Long, order: Int, newId: String) = db.withTransaction {
        val sets = dao.setsOfNow(wid).filter { it.exerciseOrder == order }
        val pending = sets.filter { !it.done }
        pending.forEach { dao.deleteSet(it.id) }
        val first = sets.firstOrNull()
        val plan = PlanItem(newId, sets = max(1, pending.count { it.type != SetType.WARMUP.name }),
            repMin = first?.targetMin ?: 8, repMax = first?.targetMax ?: 12, rpe = first?.targetRpe, superset = first?.superset ?: 0)
        val fresh = buildSets(wid, order, plan, false)
        val doneCount = sets.count { it.done }
        // Done sets of the old exercise move to a new position at the end so history stays correct.
        if (doneCount > 0) {
            val endOrder = (dao.setsOfNow(wid).maxOfOrNull { it.exerciseOrder } ?: order) + 1
            dao.updateSets(sets.filter { it.done }.map { it.copy(exerciseOrder = endOrder) })
        }
        dao.insertSets(fresh)
    }

    /** Skip = remove the sets you haven't done for that exercise. */
    suspend fun skipExercise(wid: Long, order: Int) {
        dao.setsOfNow(wid).filter { it.exerciseOrder == order && !it.done }.forEach { dao.deleteSet(it.id) }
        compactOrders(wid)
    }

    private suspend fun compactOrders(wid: Long) {
        val orders = dao.setsOfNow(wid).map { it.exerciseOrder }.distinct().sorted()
        val map = orders.withIndex().associate { (i, o) -> o to i }
        dao.updateSets(dao.setsOfNow(wid).map { it.copy(exerciseOrder = map[it.exerciseOrder] ?: it.exerciseOrder) })
    }

    /** Move an exercise up (-1) or down (+1). */
    suspend fun moveExercise(wid: Long, order: Int, delta: Int) = db.withTransaction {
        val all = dao.setsOfNow(wid)
        val target = order + delta
        if (all.none { it.exerciseOrder == target }) return@withTransaction
        dao.updateSets(all.map {
            when (it.exerciseOrder) { order -> it.copy(exerciseOrder = target); target -> it.copy(exerciseOrder = order); else -> it }
        })
    }

    /** Link this exercise with the next one as a superset (or unlink). */
    suspend fun toggleSuperset(wid: Long, order: Int) {
        val all = dao.setsOfNow(wid)
        val mine = all.filter { it.exerciseOrder == order }
        val next = all.filter { it.exerciseOrder == order + 1 }
        if (next.isEmpty()) return
        val linked = mine.firstOrNull()?.superset ?: 0
        val group = if (linked != 0 && next.first().superset == linked) 0 else order + 1
        dao.updateSets((mine + next).map { it.copy(superset = group) })
    }

    suspend fun discardWorkout(wid: Long) = db.withTransaction { dao.deleteSetsOf(wid); dao.deleteWorkout(wid) }

    // ───────────────────────── finishing ─────────────────────────
    suspend fun finishWorkout(wid: Long, rating: Int, notes: String): FinishResult? {
        val w = dao.workout(wid) ?: return null
        val s = settings.current()
        val body = s.effectiveBodyLb
        // Previous bests BEFORE this workout (for records).
        val history = dao.allDoneSetsNow().filter { it.workoutId != wid }
        val sets = dao.setsOfNow(wid)
        sets.filter { !it.done }.forEach { dao.deleteSet(it.id) }        // drop sets you didn't do
        val done = sets.filter { it.done }
        // Nothing ticked as done → don't save an empty workout (it would count toward the streak).
        if (done.isEmpty()) { discardWorkout(wid); return null }
        val end = System.currentTimeMillis()
        val minutes = (end - w.startMs) / 60_000.0

        // Calories:
        //  lifting/calisthenics → SET + REST split (CalorieMath.strength)
        //  cardio/stretch/class → MET × kg × hours per exercise
        var activeSec = 0; var metKcal = 0.0; var metMinutes = 0.0
        for (x in done) {
            val e = ex(x.exerciseId) ?: continue
            when (e.mode) {
                LogMode.CARDIO, LogMode.STRETCH -> {
                    val m = x.seconds / 60.0
                    metKcal += CalorieMath.met(e.met, body, m); metMinutes += m
                }
                else -> activeSec += CalorieMath.activeSeconds(x.reps, if (e.mode == LogMode.HOLD) x.seconds else 0)
            }
        }
        val liftMinutes = max(0.0, minutes - metMinutes)
        val kcal = metKcal + if (activeSec > 0) CalorieMath.strength(body, liftMinutes, activeSec) else 0.0
        val volume = done.sumOf { x -> ex(x.exerciseId)?.let { StrengthMath.volume(it.mode, x.lb, x.reps, body, it.bwPct) } ?: 0.0 }
        val miles = done.sumOf { it.miles }

        var finished = w.copy(endMs = end, rating = rating, notes = notes, kcal = kcal, miles = miles)
        dao.updateWorkout(finished)
        finished = writeHealth(finished)

        // Program: advance to the next day; a deload resets the deload clock.
        dao.activeProgramNow()?.let { st ->
            if (w.programId == st.programId) {
                val prog = Programs.byId(st.programId)
                if (prog != null) dao.upsertProgram(st.copy(
                    nextDay = (w.programDay + 1).mod(prog.days.size),
                    lastDeload = if (w.deload) today() else st.lastDeload))
            }
        }

        val prs = findPrs(done, history)
        val newBadges = refreshBadges()
        val ups = ladderCandidates(done)
        val protein = if (s.healthConnect) health.proteinToday() else null
        return FinishResult(wid, minutes, kcal, volume, done.count { it.type != SetType.WARMUP.name }, prs, newBadges, ups, protein)
    }

    private suspend fun writeHealth(w: WorkoutEntity): WorkoutEntity {
        if (!settings.current().healthConnect) return w
        if (w.hcRecordId.isNotBlank()) health.deleteOwn(w.hcRecordId)
        val id = health.writeCalories(w.id, w.startMs, w.endMs, w.kcal, liveGps = w.route.isNotBlank())
        val updated = w.copy(hcRecordId = id)
        dao.updateWorkout(updated)
        return updated
    }

    /** Delete a finished workout (and only OUR Health Connect record for it). */
    suspend fun deleteWorkout(w: WorkoutEntity) {
        if (w.hcRecordId.isNotBlank()) health.deleteOwn(w.hcRecordId)
        discardWorkout(w.id)
    }

    /** Records: heaviest weight, best e1RM (Epley), most reps in a set, longest distance, fastest pace (≥ 1 mi). */
    private fun findPrs(done: List<SetEntity>, history: List<SetEntity>): List<PrHit> {
        val out = mutableListOf<PrHit>()
        for ((exId, mine) in done.filter { it.type != SetType.WARMUP.name }.groupBy { it.exerciseId }) {
            val e = ex(exId) ?: continue
            val old = history.filter { it.exerciseId == exId && it.type != SetType.WARMUP.name }
            if (old.isEmpty()) continue                                    // first time isn't a "record"
            when (e.mode) {
                LogMode.WEIGHT_REPS -> {
                    val w = mine.maxOf { it.lb }; if (w > old.maxOf { it.lb }) out += PrHit(exId, "weight", w)
                    val e1 = mine.maxOf { StrengthMath.epley(it.lb, it.reps) }
                    if (e1 > old.maxOf { StrengthMath.epley(it.lb, it.reps) } + 0.5) out += PrHit(exId, "e1rm", e1)
                }
                LogMode.BODYWEIGHT_REPS -> {
                    val r = mine.maxOf { it.reps }; if (r > old.maxOf { it.reps }) out += PrHit(exId, "reps", r.toDouble())
                }
                LogMode.HOLD -> {
                    val t = mine.maxOf { it.seconds }; if (t > old.maxOf { it.seconds }) out += PrHit(exId, "hold", t.toDouble())
                }
                LogMode.CARDIO -> {
                    val d = mine.maxOf { it.miles }; if (d > 0 && d > old.maxOf { it.miles }) out += PrHit(exId, "distance", d)
                    fun pace(x: SetEntity) = if (x.miles >= 1.0) x.seconds / x.miles else Double.MAX_VALUE
                    val p = mine.minOf { pace(it) }; val op = old.minOf { pace(it) }
                    if (p < Double.MAX_VALUE && p < op) out += PrHit(exId, "pace", p)
                }
                LogMode.STRETCH -> {}
            }
        }
        return out
    }

    /** Recomputes badges from all history and saves any new ones. Returns the new ids. */
    suspend fun refreshBadges(): List<String> {
        val s = settings.current()
        val sets = dao.allDoneSetsNow().filter { it.type != SetType.WARMUP.name }
        val workouts = dao.finishedWorkoutsNow()
        fun best(ids: Set<String>) = sets.filter { it.exerciseId in ids && it.reps >= 1 }.maxOfOrNull { it.lb } ?: 0.0
        fun maxReps(ids: Set<String>) = sets.filter { it.exerciseId in ids }.maxOfOrNull { it.reps } ?: 0
        val runs = setOf("treadmill_run", "outdoor_run", "sprints")
        val stats = BadgeStats(
            workouts = workouts.size,
            bestStreakWeeks = StreakMath.compute(workouts.map { it.date }, s.weeklyGoal, today()).bestStreak,
            bodyLb = s.effectiveBodyLb,
            bestBenchLb = best(setOf("bb_bench_press")),
            bestSquatLb = best(setOf("bb_back_squat", "bb_front_squat")),
            bestDeadliftLb = best(setOf("bb_deadlift", "trap_bar_deadlift", "sumo_deadlift")),
            maxPullupsOneSet = maxReps(setOf("pl_pullup", "pl_chinup", "pl_weighted", "pl_lsit_pullup")),
            maxPushupsOneSet = maxReps(setOf("pu_standard", "pu_wide", "pu_diamond", "pu_decline")),
            maxPlankSec = sets.filter { it.exerciseId == "plank" }.maxOfOrNull { it.seconds } ?: 0,
            totalMiles = sets.sumOf { it.miles },
            longestRunMi = sets.filter { it.exerciseId in runs }.maxOfOrNull { it.miles } ?: 0.0,
        )
        val have = dao.badgesNow().map { it.id }.toSet()
        val new = Badges.earned(stats) - have
        if (new.isNotEmpty()) dao.insertBadges(new.map { BadgeEntity(it, today()) })
        return new.toList()
    }

    private suspend fun ladderCandidates(done: List<SetEntity>): List<Pair<String, Int>> {
        val state = dao.laddersNow().associate { it.ladderId to it.step }
        return Programs.ladders.mapNotNull { l ->
            val idx = state[l.id] ?: 0
            val step = l.steps.getOrNull(idx) ?: return@mapNotNull null
            if (idx >= l.steps.lastIndex) return@mapNotNull null
            val mine = done.filter { it.exerciseId == step.exerciseId && it.type != SetType.WARMUP.name }
            if (mine.isEmpty()) return@mapNotNull null
            if (Programs.readyToLevelUp(step, mine.map { it.reps }, mine.map { it.seconds })) l.id to idx + 1 else null
        }
    }

    suspend fun setLadderStep(ladderId: String, step: Int) = dao.upsertLadder(LadderEntity(ladderId, step))

    /** "Save changes back" — rewrite the routine from what you actually did. */
    suspend fun saveWorkoutAsRoutine(wid: Long, rid: Long?) {
        val sets = dao.setsOfNow(wid).filter { it.type != SetType.WARMUP.name }
        val items = sets.groupBy { it.exerciseOrder }.toSortedMap().values.map { g ->
            val f = g.first()
            val mode = ex(f.exerciseId)?.mode
            RoutineItemEntity(routineId = rid ?: 0, sortOrder = 0, exerciseId = f.exerciseId, sets = g.size,
                repMin = if (f.targetMin > 0) f.targetMin else g.minOf { it.reps },
                repMax = if (f.targetMax > 0) f.targetMax else g.maxOf { it.reps },
                targetRpe = f.targetRpe, restSec = 0, superset = f.superset,
                holdSec = if (mode == LogMode.HOLD || mode == LogMode.STRETCH) f.seconds else 0,
                minutes = if (mode == LogMode.CARDIO) (f.seconds / 60).coerceAtLeast(1) else 0,
                warmups = dao.setsOfNow(wid).any { it.exerciseOrder == f.exerciseOrder && it.type == SetType.WARMUP.name })
        }
        db.withTransaction {
            // If the routine was deleted meanwhile, make a new one instead of saving "orphan" items.
            val existing = rid?.let { dao.routine(it) }?.id
            val id = existing ?: dao.insertRoutine(com.gymcat.app.data.db.RoutineEntity(name = dao.workout(wid)?.title ?: "Routine"))
            dao.deleteRoutineItems(id)
            dao.insertRoutineItems(items.mapIndexed { i, it -> it.copy(routineId = id, sortOrder = i) })
        }
    }

    suspend fun saveRoutine(r: com.gymcat.app.data.db.RoutineEntity, items: List<RoutineItemEntity>): Long = db.withTransaction {
        val id = if (r.id == 0L) dao.insertRoutine(r) else { dao.updateRoutine(r); r.id }
        dao.deleteRoutineItems(id)
        dao.insertRoutineItems(items.mapIndexed { i, it -> it.copy(id = 0, routineId = id, sortOrder = i) })
        id
    }

    suspend fun deleteRoutine(rid: Long) = db.withTransaction { dao.deleteRoutineItems(rid); dao.deleteRoutineRow(rid) }

    // ───────────────────────── cardio, stretch, classes ─────────────────────────
    /** Saves a finished cardio session as a workout with one cardio set. */
    suspend fun saveCardio(exId: String, startMs: Long, endMs: Long, seconds: Int, miles: Double, route: String, title: String): Long {
        val e = ex(exId)
        val body = settings.current().effectiveBodyLb
        val kcal = CalorieMath.met(e?.met ?: 7.0, body, seconds / 60.0)
        val wid = dao.insertWorkout(WorkoutEntity(date = today(), startMs = startMs, endMs = endMs, title = title,
            kind = WorkoutKind.CARDIO.name, kcal = kcal, miles = miles, route = route))
        dao.insertSet(SetEntity(workoutId = wid, exerciseId = exId, exerciseOrder = 0, setIndex = 1,
            seconds = seconds, miles = miles, done = true))
        dao.workout(wid)?.let { writeHealth(it) }
        refreshBadges()
        return wid
    }

    /** Guided stretch session: one set per stretch with the seconds held. */
    suspend fun saveStretch(holds: List<Pair<String, Int>>, startMs: Long, title: String): Long {
        val body = settings.current().effectiveBodyLb
        val end = System.currentTimeMillis()
        val kcal = CalorieMath.met(2.3, body, (end - startMs) / 60_000.0)
        val wid = dao.insertWorkout(WorkoutEntity(date = today(), startMs = startMs, endMs = end, title = title,
            kind = WorkoutKind.STRETCH.name, kcal = kcal))
        dao.insertSets(holds.mapIndexed { i, (id, sec) ->
            SetEntity(workoutId = wid, exerciseId = id, exerciseOrder = i, setIndex = 1, seconds = sec, done = true)
        })
        dao.workout(wid)?.let { writeHealth(it) }
        refreshBadges()
        return wid
    }

    /** Class or sport: name + minutes + MET. */
    suspend fun saveClass(name: String, met: Double, minutes: Int): Long {
        val body = settings.current().effectiveBodyLb
        val end = System.currentTimeMillis()
        val start = end - minutes * 60_000L
        val wid = dao.insertWorkout(WorkoutEntity(date = today(), startMs = start, endMs = end, title = name,
            kind = WorkoutKind.CLASS.name, met = met, kcal = CalorieMath.met(met, body, minutes.toDouble())))
        dao.workout(wid)?.let { writeHealth(it) }
        refreshBadges()
        return wid
    }

    /** Refresh body weight from Health Connect (if on). */
    suspend fun syncWeight() {
        if (!settings.current().healthConnect) return
        health.latestWeight()?.let { (lb, day) -> settings.update { it.copy(hcWeightLb = lb, hcWeightDay = day) } }
    }
}
