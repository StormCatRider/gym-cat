/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/screens/ProgressScreen.kt                 ║
║ Progress tab (your 4 charts + records, badges and ladders):        ║
║  1. Workout calendar + workouts per week                           ║
║  2. Per exercise: estimated 1RM (Epley AND Brzycki) + top weight   ║
║  3. Volume per muscle: sets this week + weekly total volume        ║
║  4. Body weight (read from Gym & Ñam through Health Connect)       ║
║ Plus records, badges, skill ladders and the full history.          ║
║ HistoryScreen: calendar + list; WorkoutDetailScreen: one workout.  ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gymcat.app.R
import com.gymcat.app.core.calc.Badges
import com.gymcat.app.core.calc.GeoMath
import com.gymcat.app.core.calc.StreakMath
import com.gymcat.app.core.calc.StrengthMath
import com.gymcat.app.core.model.LogMode
import com.gymcat.app.core.model.SetType
import com.gymcat.app.core.programs.Programs
import com.gymcat.app.data.db.SetEntity
import com.gymcat.app.data.db.WorkoutEntity
import com.gymcat.app.data.today
import com.gymcat.app.ui.Fmt
import com.gymcat.app.ui.MainViewModel
import com.gymcat.app.ui.components.BadgeIcon
import com.gymcat.app.ui.components.BarList
import com.gymcat.app.ui.components.Cat
import com.gymcat.app.ui.components.CatArt
import com.gymcat.app.ui.components.ColumnChart
import com.gymcat.app.ui.components.EmptyState
import com.gymcat.app.ui.components.GCard
import com.gymcat.app.ui.components.LineChart
import com.gymcat.app.ui.components.Muted
import com.gymcat.app.ui.components.RouteView
import com.gymcat.app.ui.components.SectionTitle
import com.gymcat.app.ui.theme.GymColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ProgressScreen(vm: MainViewModel, nav: NavHostController) {
    val c = GymColors.current
    val s by vm.settings.collectAsState()
    val workouts by vm.workouts.collectAsState()
    val sets by vm.doneSets.collectAsState()
    val records by vm.records.collectAsState()
    val muscleSets by vm.muscleSetsThisWeek.collectAsState()
    val badges by vm.badges.collectAsState()
    val ladders by vm.ladders.collectAsState()
    val weights by vm.weightHistory.collectAsState()
    val streak by vm.streak.collectAsState()
    val es = vm.es
    LaunchedEffect(s.healthConnect) { vm.refreshHealth() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.tab_progress), Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = c.text)
            TextButton(onClick = { nav.navigate("history") }) { Text(stringResource(R.string.history)) }
        }
        if (workouts.isEmpty()) {
            EmptyState(Cat.EMPTY, "🎒", stringResource(R.string.no_workouts_title), stringResource(R.string.no_workouts_body))
            return@Column
        }

        // ── 1. Calendar + workouts per week
        GCard {
            Text(stringResource(R.string.calendar), fontWeight = FontWeight.SemiBold, color = c.text)
            MonthCalendar(workouts) { day -> workouts.firstOrNull { it.date == day }?.let { nav.navigate("workoutDetail/${it.id}") } }
            Text("🔥 " + stringResource(R.string.streak_weeks, streak.streakWeeks) + "   🏅 " + stringResource(R.string.best_streak, streak.bestStreak), color = c.text)
            Spacer(Modifier.height(8.dp))
            Muted(stringResource(R.string.workouts_per_week))
            val thisWeek = StreakMath.weekStart(today())
            val weeks = (7 downTo 0).map { thisWeek - it * 7 }
            ColumnChart(weeks.map { w -> workouts.count { it.date in w until w + 7 }.toDouble() },
                weeks.map { LocalDate.ofEpochDay(it).let { d -> "${d.monthValue}/${d.dayOfMonth}" } })
        }

        // ── 2. Per exercise
        val liftIds = records.filter { r -> vm.ex(r.exerciseId)?.mode == LogMode.WEIGHT_REPS }.sortedByDescending { it.bestEpley }.map { it.exerciseId }
        if (liftIds.isNotEmpty()) GCard {
            var pick by remember { mutableStateOf(liftIds.first()) }
            var menu by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.per_exercise), Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = c.text)
                TextButton(onClick = { menu = true }) { Text(vm.exName(pick) + " ▾") }
                DropdownMenu(menu, { menu = false }) {
                    liftIds.forEach { id -> DropdownMenuItem(text = { Text(vm.exName(id)) }, onClick = { pick = id; menu = false }) }
                }
            }
            val prog = vm.exerciseProgress(pick)
            Muted(stringResource(R.string.chart_e1rm_legend), size = 12)
            LineChart(listOf(prog.map { it.first } to c.accent, prog.map { it.second } to c.navy, prog.map { it.third } to c.warn))
            records.firstOrNull { it.exerciseId == pick }?.let {
                Text(stringResource(R.string.e1rm_both, Fmt.weight(it.bestEpley, s.metric), Fmt.weight(it.bestBrzycki, s.metric)), color = c.text)
            }
        }

        // ── 3. Volume per muscle
        GCard {
            Text(stringResource(R.string.sets_per_muscle_week), fontWeight = FontWeight.SemiBold, color = c.text)
            if (muscleSets.isEmpty()) Muted(stringResource(R.string.nothing_this_week))
            else BarList(muscleSets.entries.sortedByDescending { it.value }.map { muscleName(it.key) to it.value }) { Fmt.num(it) }
            Spacer(Modifier.height(8.dp))
            Muted(stringResource(R.string.weekly_volume))
            val thisWeek = StreakMath.weekStart(today())
            val weeks = (7 downTo 0).map { thisWeek - it * 7 }
            val byId = workouts.associateBy { it.id }
            ColumnChart(weeks.map { w ->
                sets.filter { st -> byId[st.workoutId]?.date?.let { it in w until w + 7 } == true && st.type != SetType.WARMUP.name }
                    .sumOf { st -> vm.ex(st.exerciseId)?.let { StrengthMath.volume(it.mode, st.lb, st.reps, s.effectiveBodyLb, it.bwPct) } ?: 0.0 }
            }, weeks.map { LocalDate.ofEpochDay(it).let { d -> "${d.monthValue}/${d.dayOfMonth}" } })
        }

        // ── 4. Body weight (Health Connect)
        GCard {
            Text(stringResource(R.string.body_weight), fontWeight = FontWeight.SemiBold, color = c.text)
            if (!s.healthConnect) Muted(stringResource(R.string.body_weight_hc_off))
            else if (weights.size < 2) Muted(stringResource(R.string.body_weight_none))
            else {
                LineChart(listOf(weights.map { if (s.metric) com.gymcat.app.core.calc.BodyMath.lbToKg(it.second) else it.second } to c.accent))
                Muted(stringResource(R.string.latest) + ": " + Fmt.weight(weights.last().second, s.metric))
            }
        }

        // ── Records
        SectionTitle(stringResource(R.string.records))
        GCard {
            records.sortedByDescending { it.bestEpley + it.mostReps + it.longestSec + it.longestMi * 100 }.take(12).forEach { r ->
                val e = vm.ex(r.exerciseId)
                val txt = when (e?.mode) {
                    LogMode.WEIGHT_REPS -> Fmt.weight(r.heaviestLb, s.metric) + " · 1RM " + Fmt.weight(r.bestEpley, s.metric)
                    LogMode.BODYWEIGHT_REPS -> "${r.mostReps} reps"
                    LogMode.CARDIO -> Fmt.dist(r.longestMi, s.metric)
                    else -> Fmt.duration(r.longestSec.toLong())
                }
                Row(Modifier.fillMaxWidth().clickable { nav.navigate("exercise/${r.exerciseId}") }.padding(vertical = 4.dp)) {
                    Text(vm.exName(r.exerciseId), Modifier.weight(1f), color = c.text)
                    Text(txt, color = c.text2)
                }
            }
        }

        // ── Badges
        SectionTitle(stringResource(R.string.badges))
        val have = badges.map { it.id }.toSet()
        GCard {
            Badges.all.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { b ->
                        Column(Modifier.weight(1f).padding(4.dp).alpha(if (b.id in have) 1f else 0.3f), horizontalAlignment = Alignment.CenterHorizontally) {
                            BadgeIcon(b.emoji, 52.dp)
                            Text(b.title(es), fontSize = 10.sp, textAlign = TextAlign.Center, color = c.text, maxLines = 2)
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // ── Skill ladders
        SectionTitle(stringResource(R.string.skill_ladders))
        Programs.ladders.forEach { l ->
            val step = (ladders[l.id] ?: 0).coerceIn(0, l.steps.lastIndex)
            val cur = l.steps[step]
            GCard {
                Text(l.emoji + " " + l.name(es), fontWeight = FontWeight.SemiBold, color = c.text)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    l.steps.indices.forEach { i ->
                        Box(Modifier.size(14.dp).clip(CircleShape).background(if (i <= step) c.accent else c.track))
                    }
                }
                Text(stringResource(R.string.ladder_current, vm.exName(cur.exerciseId)), color = c.text)
                Muted(if (cur.holdSec > 0) stringResource(R.string.ladder_target_hold, cur.sets, cur.holdSec)
                      else stringResource(R.string.ladder_target_reps, cur.sets, cur.reps))
                Row {
                    TextButton(onClick = { vm.setLadderStep(l.id, (step - 1).coerceAtLeast(0)) }, enabled = step > 0) { Text("‹ " + stringResource(R.string.step_down)) }
                    TextButton(onClick = { vm.setLadderStep(l.id, (step + 1).coerceAtMost(l.steps.lastIndex)) }, enabled = step < l.steps.lastIndex) { Text(stringResource(R.string.step_up) + " ›") }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Month grid (Monday first). Dots = days you trained. Tap a dot to open that workout. */
@Composable
fun MonthCalendar(workouts: List<WorkoutEntity>, onDay: (Long) -> Unit) {
    val c = GymColors.current
    var offset by remember { mutableIntStateOf(0) }
    val first = LocalDate.now().withDayOfMonth(1).plusMonths(offset.toLong())
    val days = workouts.map { it.date }.toSet()
    val lead = first.dayOfWeek.value - 1                            // Monday = 0
    val count = first.lengthOfMonth()
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { offset-- }) { Text("‹") }
        Text(first.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.titlecase() } + " " + first.year,
            Modifier.weight(1f), textAlign = TextAlign.Center, color = c.text)
        TextButton(onClick = { offset++ }, enabled = offset < 0) { Text("›") }
    }
    Row { stringArrayResource(R.array.week_days_short).forEach { Muted(it, Modifier.weight(1f), size = 11) } }
    (0 until ((lead + count + 6) / 7)).forEach { week ->
        Row {
            (0 until 7).forEach { col ->
                val n = week * 7 + col - lead + 1
                Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                    if (n in 1..count) {
                        val d = first.withDayOfMonth(n).toEpochDay()
                        val trained = d in days
                        Box(Modifier.fillMaxSize().clip(CircleShape).background(if (trained) c.accent else Color.Transparent)
                            .clickable(enabled = trained) { onDay(d) }, contentAlignment = Alignment.Center) {
                            Text(n.toString(), fontSize = 12.sp, color = if (trained) Color.White else c.text)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(vm: MainViewModel, nav: NavHostController) {
    val c = GymColors.current
    val s by vm.settings.collectAsState()
    val workouts by vm.workouts.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) { Text("‹ " + stringResource(R.string.back)) }
            Text(stringResource(R.string.history), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = c.text)
        }
        GCard { MonthCalendar(workouts) { d -> workouts.firstOrNull { it.date == d }?.let { nav.navigate("workoutDetail/${it.id}") } } }
        Muted(stringResource(R.string.history_hint))
        workouts.forEach { w ->
            GCard(onClick = { nav.navigate("workoutDetail/${w.id}") }) {
                Text(w.title, fontWeight = FontWeight.SemiBold, color = c.text)
                Muted(LocalDate.ofEpochDay(w.date).toString() + " · " + Fmt.duration((w.endMs - w.startMs) / 1000) + " · " + Fmt.kcal(w.kcal) +
                    if (w.miles > 0) " · " + Fmt.dist(w.miles, s.metric) else "")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun WorkoutDetailScreen(vm: MainViewModel, nav: NavHostController, id: Long) {
    val c = GymColors.current
    val s by vm.settings.collectAsState()
    val w by produceState<WorkoutEntity?>(null, id) { value = vm.repo.dao.workout(id) }
    val sets by produceState(emptyList<SetEntity>(), id) { value = vm.repo.dao.setsOfNow(id) }
    var confirm by remember { mutableStateOf(false) }
    val workout = w ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) { Text("‹ " + stringResource(R.string.back)) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { confirm = true }) { Text(stringResource(R.string.delete), color = c.bad) }
        }
        Text(workout.title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.text)
        Muted(LocalDate.ofEpochDay(workout.date).toString() + " · " + Fmt.duration((workout.endMs - workout.startMs) / 1000) + " · " + Fmt.kcal(workout.kcal))
        if (workout.rating > 0) Text(listOf("😫", "😕", "🙂", "😀", "🤩")[workout.rating - 1] + " " + workout.notes, color = c.text)
        if (workout.route.isNotBlank()) {
            val pts = GeoMath.decode(workout.route)
            RouteView(GeoMath.project(pts))
            val splits = GeoMath.splitsSec(pts)
            if (splits.isNotEmpty()) Muted(stringResource(R.string.splits) + ": " + splits.joinToString(" · ") { Fmt.duration(it) })
        }
        sets.groupBy { it.exerciseOrder }.toSortedMap().values.forEach { g ->
            val e = vm.ex(g.first().exerciseId)
            GCard(onClick = { nav.navigate("exercise/${g.first().exerciseId}") }) {
                Text((e?.emoji ?: "") + " " + vm.exName(g.first().exerciseId), fontWeight = FontWeight.SemiBold, color = c.text)
                g.forEach { st ->
                    val txt = when (e?.mode) {
                        LogMode.WEIGHT_REPS -> "${Fmt.weight(st.lb, s.metric)} × ${st.reps}" + (st.rpe?.let { " @${Fmt.num(it)}" } ?: "")
                        LogMode.BODYWEIGHT_REPS -> "${st.reps} reps" + if (st.lb != 0.0) " (${if (st.lb > 0) "+" else ""}${Fmt.weight(st.lb, s.metric)})" else ""
                        LogMode.CARDIO -> Fmt.duration(st.seconds.toLong()) + " · " + Fmt.dist(st.miles, s.metric)
                        else -> Fmt.duration(st.seconds.toLong())
                    }
                    val warm = if (st.type == SetType.WARMUP.name) " (W)" else ""
                    val note = if (st.note.isNotBlank()) " — ${st.note}" else ""
                    Text("${st.setIndex}. $txt$warm$note", color = c.text)
                }
            }
        }
        OutlinedButton(onClick = { vm.saveBackToRoutine(workout.id, null); vm.toast.value = null; nav.popBackStack() }) {
            Text(stringResource(R.string.save_as_routine))
        }
        Spacer(Modifier.height(24.dp))
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false },
        title = { Text(stringResource(R.string.delete_workout)) },
        text = { Text(stringResource(R.string.delete_workout_body)) },
        confirmButton = { TextButton(onClick = { vm.deleteWorkout(workout); confirm = false; nav.popBackStack() }) { Text(stringResource(R.string.delete), color = c.bad) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } })
}
