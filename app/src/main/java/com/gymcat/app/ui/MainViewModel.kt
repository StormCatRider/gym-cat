/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/MainViewModel.kt                          ║
║ The ViewModel holds what the screens show and survives screen      ║
║ rotation. It:                                                       ║
║  • turns database tables into live lists (StateFlow) for screens;  ║
║  • works out derived numbers (streak, records, weekly muscle sets, ║
║    deload suggestion, "missed a program day");                     ║
║  • forwards button presses to the Repository in the background.    ║
║ One ViewModel + one factory, same style as Gym & Ñam.              ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gymcat.app.GymCatApp
import com.gymcat.app.core.calc.StreakMath
import com.gymcat.app.core.calc.StrengthMath
import com.gymcat.app.core.model.Exercise
import com.gymcat.app.core.model.LogMode
import com.gymcat.app.core.model.Muscle
import com.gymcat.app.core.model.SetType
import com.gymcat.app.core.programs.Programs
import com.gymcat.app.data.AppSettings
import com.gymcat.app.data.FinishResult
import com.gymcat.app.data.PlanItem
import com.gymcat.app.data.ShareCodec
import com.gymcat.app.data.db.CustomExerciseEntity
import com.gymcat.app.data.db.ExercisePrefEntity
import com.gymcat.app.data.db.MeasureEntity
import com.gymcat.app.data.db.PhotoEntity
import com.gymcat.app.data.db.RoutineEntity
import com.gymcat.app.data.db.RoutineItemEntity
import com.gymcat.app.data.db.SetEntity
import com.gymcat.app.data.db.WorkoutEntity
import com.gymcat.app.data.today
import com.gymcat.app.service.WorkoutClock
import com.gymcat.app.service.WorkoutService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

/** Best numbers for one exercise (Progress → Records). */
data class ExerciseRecord(val exerciseId: String, val heaviestLb: Double, val bestEpley: Double, val bestBrzycki: Double,
                          val mostReps: Int, val longestSec: Int, val longestMi: Double)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val app: GymCatApp) : ViewModel() {
    val repo = app.repo
    private val dao = repo.dao
    private fun <T> kotlinx.coroutines.flow.Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    val es: Boolean get() = Locale.getDefault().language == "es"

    val settings: StateFlow<AppSettings> = repo.settings.flow.state(AppSettings())
    /** null until the settings file has been read once — so the lock screen never "flashes" open. */
    val settingsReady: StateFlow<AppSettings?> = repo.settings.flow.map<AppSettings, AppSettings?> { it }.state(null)

    val exercises: StateFlow<List<Exercise>> = dao.customExercises().map { repo.library.setCustom(it); repo.library.all() }
        .state(repo.library.builtIn)
    val prefs: StateFlow<Map<String, ExercisePrefEntity>> = dao.prefs().map { l -> l.associateBy { it.exerciseId } }.state(emptyMap())
    val workouts: StateFlow<List<WorkoutEntity>> = dao.finishedWorkouts().state(emptyList())
    val doneSets: StateFlow<List<SetEntity>> = dao.allDoneSets().state(emptyList())
    val active: StateFlow<WorkoutEntity?> = dao.activeWorkout().state(null)
    val activeSets: StateFlow<List<SetEntity>> = active.flatMapLatest { w -> if (w == null) flowOf(emptyList()) else dao.setsOf(w.id) }.state(emptyList())
    val routines: StateFlow<List<RoutineEntity>> = dao.routines().state(emptyList())
    val routineItems: StateFlow<Map<Long, List<RoutineItemEntity>>> = dao.allRoutineItems().map { l -> l.groupBy { it.routineId } }.state(emptyMap())
    val program = dao.activeProgram().state(null)
    val badges = dao.badges().state(emptyList())
    val ladders: StateFlow<Map<String, Int>> = dao.ladders().map { l -> l.associate { it.ladderId to it.step } }.state(emptyMap())
    val measures: StateFlow<List<MeasureEntity>> = dao.measures().state(emptyList())
    val photos: StateFlow<List<PhotoEntity>> = dao.photos().state(emptyList())

    /** App lock: true at app start; set again after 60 s in the background (see MainActivity). */
    val locked = MutableStateFlow(true)
    var backgroundAt = 0L

    /** Result of the workout that just finished (Finish screen). */
    val lastFinish = MutableStateFlow<FinishResult?>(null)
    /** Message to show once (snackbar). */
    val toast = MutableStateFlow<String?>(null)
    val weightHistory = MutableStateFlow<List<Pair<Long, Double>>>(emptyList())

    fun ex(id: String): Exercise? = repo.library.lookup(id)
    fun exName(id: String): String = ex(id)?.name(es) ?: id

    // ─────────── derived: streak ───────────
    val streak: StateFlow<StreakMath.State> = combine(workouts, settings) { w, s ->
        StreakMath.compute(w.map { it.date }, s.weeklyGoal, today())
    }.state(StreakMath.compute(emptyList(), 3, today()))

    /** Which days (Mon..Sun) of this week have a workout. */
    val weekTrained: StateFlow<List<Boolean>> = workouts.map { w ->
        val start = StreakMath.weekStart(today()); val days = w.map { it.date }.toSet()
        (0 until 7).map { (start + it) in days }
    }.state(List(7) { false })

    // ─────────── derived: weekly sets per muscle (body map) ───────────
    // primary +1 per working set, secondary +0.5
    val muscleSetsThisWeek: StateFlow<Map<Muscle, Double>> = combine(doneSets, workouts) { sets, w ->
        val start = StreakMath.weekStart(today())
        val ids = w.filter { it.date >= start }.map { it.id }.toSet()
        val out = mutableMapOf<Muscle, Double>()
        sets.filter { it.workoutId in ids && it.type != SetType.WARMUP.name }.forEach { s ->
            val e = ex(s.exerciseId) ?: return@forEach
            if (e.mode == LogMode.CARDIO || e.mode == LogMode.STRETCH) return@forEach
            e.primary.forEach { out[it] = (out[it] ?: 0.0) + 1.0 }
            e.secondary.forEach { out[it] = (out[it] ?: 0.0) + 0.5 }
        }
        out
    }.state(emptyMap())

    // ─────────── derived: records ───────────
    val records: StateFlow<List<ExerciseRecord>> = doneSets.map { sets ->
        sets.filter { it.type != SetType.WARMUP.name }.groupBy { it.exerciseId }.map { (id, l) ->
            ExerciseRecord(id, l.maxOf { it.lb }, l.maxOf { StrengthMath.epley(it.lb, it.reps) },
                l.maxOf { StrengthMath.brzycki(it.lb, it.reps) }, l.maxOf { it.reps }, l.maxOf { it.seconds }, l.maxOf { it.miles })
        }
    }.state(emptyList())

    /** Per-session best e1RM (Epley and Brzycki) and top weight for one exercise, oldest first. */
    fun exerciseProgress(exId: String): List<Triple<Double, Double, Double>> {
        val w = workouts.value.associateBy { it.id }
        return doneSets.value.filter { it.exerciseId == exId && it.type != SetType.WARMUP.name && it.workoutId in w }
            .groupBy { it.workoutId }.toList().sortedBy { w[it.first]?.startMs ?: 0 }
            .map { (_, l) -> Triple(l.maxOf { StrengthMath.epley(it.lb, it.reps) }, l.maxOf { StrengthMath.brzycki(it.lb, it.reps) }, l.maxOf { it.lb }) }
    }

    // ─────────── derived: deload + missed day ───────────
    /** True when ≥ 6 weeks since last deload/program start, or ≥ 2 main lifts stalled. */
    val deloadSuggested: StateFlow<Boolean> = combine(doneSets, workouts, program, settings) { sets, w, prog, st ->
        if (today() < st.deloadSnoozeDay) return@combine false            // you tapped "Not now"
        val byId = w.associateBy { it.id }
        val recentIds = w.filter { it.date >= today() - 42 }.map { it.id }.toSet()
        val stalled = sets.filter { it.workoutId in recentIds && it.type != SetType.WARMUP.name && ex(it.exerciseId)?.mode == LogMode.WEIGHT_REPS }
            .groupBy { it.exerciseId }.count { (_, l) ->
                val sessions = l.groupBy { it.workoutId }.toList().sortedBy { byId[it.first]?.startMs ?: 0 }
                    .map { (_, s) -> s.maxOf { StrengthMath.epley(it.lb, it.reps) } }
                StrengthMath.isStalled(sessions)
            }
        val weeks = if (prog != null) ((today() - prog.lastDeload) / 7).toInt() else 0
        (prog != null || w.size >= 12) && StrengthMath.shouldDeload(weeks, stalled)
    }.state(false)

    /**
     * Missed a planned program day? Allowed gap = 7 ÷ days-per-week, rounded up, + 1 day.
     * Example: 3 days/week → gap 3 + 1 = 4 days without a program workout → ask.
     */
    val missedProgramDay: StateFlow<Boolean> = combine(workouts, program) { w, prog ->
        if (prog == null) return@combine false
        val p = Programs.byId(prog.programId) ?: return@combine false
        // Last time you touched the program: a program workout, the start day, or a skip.
        val last = maxOf(w.filter { it.programId == prog.programId }.maxOfOrNull { it.date } ?: prog.started, prog.started, prog.lastSkip)
        val gap = (7 + p.daysPerWeek - 1) / p.daysPerWeek + 1
        today() - last > gap
    }.state(false)

    // ─────────── actions ───────────
    fun updateSettings(change: (AppSettings) -> AppSettings) = viewModelScope.launch { repo.settings.update(change) }

    private fun afterStart(id: Long) {
        if (id <= 0) return
        viewModelScope.launch {
            val w = dao.workout(id) ?: return@launch
            WorkoutService.start(app, w.title, w.startMs)
        }
    }

    fun startEmpty(title: String, onStarted: () -> Unit) = viewModelScope.launch {
        afterStart(repo.startWorkout(title, emptyList())); onStarted()
    }
    fun startRoutine(rid: Long, deload: Boolean = false, onStarted: () -> Unit) = viewModelScope.launch {
        afterStart(repo.startRoutine(rid, deload)); onStarted()
    }
    fun startProgramDay(deload: Boolean = false, onStarted: () -> Unit) = viewModelScope.launch {
        afterStart(repo.startProgramDay(es, deload)); onStarted()
    }
    fun startPlan(title: String, plan: List<PlanItem>, onStarted: () -> Unit) = viewModelScope.launch {
        afterStart(repo.startWorkout(title, plan)); onStarted()
    }
    fun skipProgramDay() = viewModelScope.launch { repo.skipProgramDay() }
    fun activateProgram(id: String) = viewModelScope.launch { repo.activateProgram(id) }
    fun stopProgram() = viewModelScope.launch { repo.stopProgram() }

    fun addExercise(exId: String) = viewModelScope.launch { active.value?.let { repo.addExercise(it.id, exId) } }
    fun addSet(order: Int) = viewModelScope.launch { active.value?.let { repo.addSet(it.id, order) } }
    fun updateSet(s: SetEntity) = viewModelScope.launch { repo.updateSet(s) }
    fun deleteSet(s: SetEntity) = viewModelScope.launch { repo.deleteSet(s) }
    fun swapExercise(order: Int, newId: String) = viewModelScope.launch { active.value?.let { repo.swapExercise(it.id, order, newId) } }
    fun skipExercise(order: Int) = viewModelScope.launch { active.value?.let { repo.skipExercise(it.id, order) } }
    fun moveExercise(order: Int, delta: Int) = viewModelScope.launch { active.value?.let { repo.moveExercise(it.id, order, delta) } }
    fun toggleSuperset(order: Int) = viewModelScope.launch { active.value?.let { repo.toggleSuperset(it.id, order) } }

    /** Tick a set as done → start the rest timer, buzz, maybe meow. */
    fun completeSet(s: SetEntity, nextIsSameSuperset: Boolean) = viewModelScope.launch {
        repo.updateSet(s.copy(done = !s.done))
        if (!s.done) {
            app.feedback.buzz(); app.feedback.meow(chance = 0.15)
            if (!nextIsSameSuperset) WorkoutClock.startRest(s.restSec)
        }
    }

    fun discardWorkout() = viewModelScope.launch {
        active.value?.let { repo.discardWorkout(it.id) }
        WorkoutService.stop(app)
    }

    /**
     * Finish, THEN (if asked) save the changes back to the routine — one after the other,
     * so the routine is built only from the sets you really did.
     * emptyMsg = text shown when no set was ticked (the workout is discarded, not saved).
     */
    fun finishWorkout(rating: Int, notes: String, saveBack: Boolean, emptyMsg: String, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val w = active.value ?: return@launch
        val r = repo.finishWorkout(w.id, rating, notes)
        WorkoutService.stop(app)
        WorkoutClock.skipRest()
        if (r == null) { toast.value = emptyMsg; onDone(false); return@launch }
        if (saveBack) repo.saveWorkoutAsRoutine(w.id, w.routineId)
        lastFinish.value = r
        if (r != null && (r.prs.isNotEmpty() || r.newBadges.isNotEmpty())) { app.feedback.buzz(strong = true); app.feedback.meow() }
        com.gymcat.app.widget.Widgets.refresh(app)
        onDone(true)
    }

    fun saveBackToRoutine(wid: Long, rid: Long?) = viewModelScope.launch { repo.saveWorkoutAsRoutine(wid, rid) }
    fun levelUp(ladderId: String, step: Int) = viewModelScope.launch { repo.setLadderStep(ladderId, step) }
    fun setLadderStep(ladderId: String, step: Int) = levelUp(ladderId, step)

    fun saveCardio(exId: String, startMs: Long, endMs: Long, seconds: Int, miles: Double, route: String, onDone: (Long) -> Unit) =
        viewModelScope.launch {
            val id = repo.saveCardio(exId, startMs, endMs, seconds, miles, route, exName(exId))
            com.gymcat.app.widget.Widgets.refresh(app); onDone(id)
        }
    fun saveStretch(holds: List<Pair<String, Int>>, startMs: Long, title: String, onDone: () -> Unit) = viewModelScope.launch {
        repo.saveStretch(holds, startMs, title); com.gymcat.app.widget.Widgets.refresh(app); onDone()
    }
    fun saveClass(name: String, met: Double, minutes: Int, onDone: () -> Unit) = viewModelScope.launch {
        repo.saveClass(name, met, minutes); com.gymcat.app.widget.Widgets.refresh(app); onDone()
    }
    fun deleteWorkout(w: WorkoutEntity) = viewModelScope.launch { repo.deleteWorkout(w) }

    // Routines
    fun saveRoutine(r: RoutineEntity, items: List<RoutineItemEntity>, onDone: (Long) -> Unit = {}) =
        viewModelScope.launch { onDone(repo.saveRoutine(r, items)) }
    fun deleteRoutine(id: Long) = viewModelScope.launch { repo.deleteRoutine(id) }
    suspend fun routineItemsNow(id: Long) = dao.routineItems(id)

    /** Share code for a routine (file or QR). */
    suspend fun shareCode(rid: Long): String? {
        val r = dao.routine(rid) ?: return null
        val items = dao.routineItems(rid)
        val custom = dao.customExercisesNow().filter { c -> items.any { it.exerciseId == c.id } }
        return ShareCodec.encode(r, items, custom)
    }

    /** Import a friend's routine. Returns its name, or null if the code is wrong. */
    fun importCode(code: String, onResult: (String?) -> Unit) = viewModelScope.launch {
        val s = ShareCodec.decode(code)
        if (s == null) { onResult(null); return@launch }
        s.custom.forEach { if (repo.library.lookup(it.id) == null) dao.upsertCustom(it) }
        val known = s.items.filter { repo.library.lookup(it.exerciseId) != null || s.custom.any { c -> c.id == it.exerciseId } }
        repo.saveRoutine(RoutineEntity(name = s.name, notes = s.notes), known)
        onResult(s.name)
    }

    // ─────────── exercise picker (Library opened "to choose") ───────────
    // pickMode: "workout" (add to workout), "swap:<order>", "routine", "stretch", "cardio"
    val pickMode = MutableStateFlow("")
    val editingRoutine = MutableStateFlow(RoutineEntity(name = ""))
    val editingItems = MutableStateFlow<List<RoutineItemEntity>>(emptyList())
    val stretchList = MutableStateFlow<List<Pair<String, Int>>>(emptyList())   // (exerciseId, seconds)
    val cardioExercise = MutableStateFlow("treadmill_run")

    fun onPicked(id: String) {
        val mode = pickMode.value
        when {
            mode == "workout" -> addExercise(id)
            mode.startsWith("swap:") -> swapExercise(mode.removePrefix("swap:").toInt(), id)
            mode == "routine" -> {
                val e = ex(id)
                val cardio = e?.mode == LogMode.CARDIO; val hold = e?.mode == LogMode.HOLD || e?.mode == LogMode.STRETCH
                editingItems.value = editingItems.value + RoutineItemEntity(routineId = 0, sortOrder = editingItems.value.size,
                    exerciseId = id, sets = if (cardio || e?.mode == LogMode.STRETCH) 1 else 3,
                    repMin = if (cardio || hold) 0 else 8, repMax = if (cardio || hold) 0 else 12,
                    holdSec = if (hold) 30 else 0, minutes = if (cardio) 20 else 0)
            }
            mode == "stretch" -> stretchList.value = stretchList.value + (id to 30)
            mode == "cardio" -> cardioExercise.value = id
        }
        pickMode.value = ""
    }

    fun editRoutine(id: Long) {
        // Clear right away so the editor never shows the previous routine for a moment.
        editingRoutine.value = RoutineEntity(name = ""); editingItems.value = emptyList()
        if (id != 0L) viewModelScope.launch {
            editingRoutine.value = dao.routine(id) ?: RoutineEntity(name = ""); editingItems.value = dao.routineItems(id)
        }
    }

    // Library
    fun toggleFavorite(id: String) = viewModelScope.launch {
        val p = prefs.value[id] ?: ExercisePrefEntity(id)
        dao.upsertPref(p.copy(favorite = !p.favorite))
    }
    fun setRest(id: String, sec: Int) = viewModelScope.launch {
        val p = prefs.value[id] ?: ExercisePrefEntity(id)
        dao.upsertPref(p.copy(restSec = sec))
    }
    fun saveCustom(e: CustomExerciseEntity) = viewModelScope.launch { dao.upsertCustom(e) }
    fun deleteCustom(id: String) = viewModelScope.launch { dao.deleteCustom(id) }

    // Body
    fun addMeasure(m: MeasureEntity) = viewModelScope.launch { dao.insertMeasure(m) }
    fun deleteMeasure(m: MeasureEntity) = viewModelScope.launch { dao.deleteMeasure(m) }
    fun addPhoto(p: PhotoEntity) = viewModelScope.launch { dao.insertPhoto(p) }
    fun deletePhoto(p: PhotoEntity) = viewModelScope.launch {
        java.io.File(app.backup.photoDir, p.fileName).delete(); dao.deletePhoto(p)
    }

    // Health Connect
    fun refreshHealth() = viewModelScope.launch {
        repo.syncWeight()
        if (settings.value.healthConnect) weightHistory.value = repo.health.weightHistory()
    }
    suspend fun proteinToday(): Double? = if (settings.value.healthConnect) repo.health.proteinToday() else null

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(application as GymCatApp) as T
    }
}
