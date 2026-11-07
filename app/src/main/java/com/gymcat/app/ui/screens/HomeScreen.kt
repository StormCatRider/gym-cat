/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/screens/HomeScreen.kt                     ║
║ The Home tab (your choice of 3 sections):                          ║
║  1. Next workout — program day or first routine + Start button.    ║
║     Also: "resume workout", "missed a day?" and "deload?" cards.   ║
║  2. This week — Mon–Sun dots, days vs goal, streak and freezes.    ║
║  3. Latest win — newest badge.                                     ║
║ AddSheet = the menu that opens from the big round + button.        ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gymcat.app.R
import com.gymcat.app.core.calc.Badges
import com.gymcat.app.core.programs.Programs
import com.gymcat.app.ui.MainViewModel
import com.gymcat.app.ui.components.Cat
import com.gymcat.app.ui.components.CatArt
import com.gymcat.app.ui.components.GCard
import com.gymcat.app.ui.components.Muted
import com.gymcat.app.ui.components.WeekDots
import com.gymcat.app.ui.theme.GymColors

@Composable
fun HomeScreen(vm: MainViewModel, nav: NavHostController) {
    val c = GymColors.current
    val active by vm.active.collectAsState()
    val program by vm.program.collectAsState()
    val routines by vm.routines.collectAsState()
    val streak by vm.streak.collectAsState()
    val week by vm.weekTrained.collectAsState()
    val badges by vm.badges.collectAsState()
    val missed by vm.missedProgramDay.collectAsState()
    val deload by vm.deloadSuggested.collectAsState()
    val es = vm.es

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CatArt(Cat.HOME, "🐱", 56.dp)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = c.text)
                Muted(stringResource(R.string.home_subtitle))
            }
            IconButton(onClick = { nav.navigate("settings") }) { Icon(Icons.Filled.Settings, stringResource(R.string.settings)) }
        }

        // ── Resume an unfinished workout
        active?.let { w ->
            GCard(onClick = { nav.navigate("workout") }) {
                Text(stringResource(R.string.resume_workout), fontWeight = FontWeight.SemiBold, color = c.text)
                Muted(w.title)
            }
        }

        // ── Missed a planned program day?
        if (missed && active == null) GCard {
            Text(stringResource(R.string.missed_day_title), fontWeight = FontWeight.SemiBold, color = c.text)
            Muted(stringResource(R.string.missed_day_body))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = { vm.startProgramDay { nav.navigate("workout") } }) { Text(stringResource(R.string.do_it_now)) }
                OutlinedButton(onClick = { vm.skipProgramDay() }) { Text(stringResource(R.string.skip_it)) }
            }
        }

        // ── Deload suggestion
        if (deload && active == null) GCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CatArt(Cat.REST_DAY, "😴", 56.dp)
                Column(Modifier.padding(start = 8.dp)) {
                    Text(stringResource(R.string.deload_title), fontWeight = FontWeight.SemiBold, color = c.text)
                    Muted(stringResource(R.string.deload_body))
                }
            }
            Row {
                if (program != null) TextButton(onClick = { vm.startProgramDay(deload = true) { nav.navigate("workout") } }) {
                    Text(stringResource(R.string.start_deload))
                }
                // Hide the suggestion for a week.
                TextButton(onClick = { vm.updateSettings { it.copy(deloadSnoozeDay = com.gymcat.app.data.today() + 7) } }) {
                    Text(stringResource(R.string.not_now))
                }
            }
        }

        // ── 1. Next workout
        GCard {
            Muted(stringResource(R.string.next_workout))
            val prog = program?.let { Programs.byId(it.programId) }
            when {
                prog != null -> {
                    val day = prog.days[program!!.nextDay % prog.days.size]
                    Text(prog.name(es), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = c.text)
                    Muted(if (es) day.nameEs else day.nameEn)
                    Spacer(Modifier.height(4.dp))
                    day.items.take(5).forEach { item ->
                        val name = if (item.exerciseId.startsWith("ladder:"))
                            Programs.ladder(item.exerciseId.removePrefix("ladder:"))?.name(es) ?: item.exerciseId
                        else vm.exName(item.exerciseId)
                        Muted("• $name")
                    }
                    Button(onClick = { vm.startProgramDay { nav.navigate("workout") } }, enabled = active == null,
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) { Text(stringResource(R.string.start)) }
                }
                routines.isNotEmpty() -> {
                    val r = routines.first()
                    Text(r.name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = c.text)
                    Button(onClick = { vm.startRoutine(r.id) { nav.navigate("workout") } }, enabled = active == null,
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) { Text(stringResource(R.string.start)) }
                }
                else -> {
                    Text(stringResource(R.string.no_plan_yet), color = c.text)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        val title = stringResource(R.string.free_workout)
                        Button(onClick = { vm.startEmpty(title) { nav.navigate("workout") } }, enabled = active == null) {
                            Text(stringResource(R.string.start_empty))
                        }
                        OutlinedButton(onClick = { nav.navigate("routines") }) { Text(stringResource(R.string.pick_program)) }
                    }
                }
            }
        }

        // ── 2. This week
        GCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Muted(stringResource(R.string.this_week))
                    Text(stringResource(R.string.days_of_goal, streak.thisWeekDays, streak.goal), fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp, color = c.text)
                }
                // Cat mood: a freeze saved last week → ice cube cat; streak alive → flame; else headband
                val frozeLastWeek = (streak.weekStart - 7) in streak.frozenWeeks
                when {
                    frozeLastWeek -> CatArt(Cat.FREEZE, "🧊", 48.dp)
                    streak.streakWeeks > 0 -> CatArt(Cat.STREAK, "🔥", 48.dp)
                    else -> CatArt(Cat.STREAK_LOST, "💪", 48.dp)
                }
            }
            Spacer(Modifier.height(8.dp))
            WeekDots(week, stringArrayResource(R.array.week_days_short).toList())
            Spacer(Modifier.height(8.dp))
            Row {
                Text("🔥 " + stringResource(R.string.streak_weeks, streak.streakWeeks), color = c.text)
                Spacer(Modifier.width(16.dp))
                Text("❄️ " + stringResource(R.string.freezes_n, streak.freezes), color = c.text)
            }
        }

        // ── 3. Latest win
        GCard {
            Muted(stringResource(R.string.latest_win))
            val b = badges.firstOrNull()?.let { Badges.byId(it.id) }
            if (b != null) Row(verticalAlignment = Alignment.CenterVertically) {
                CatArt(Cat.BADGE, b.emoji, 52.dp)
                Text(b.title(es), Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold, color = c.text)
            } else Muted(stringResource(R.string.latest_win_empty))
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Menu from the big + button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSheet(vm: MainViewModel, nav: NavHostController, onClose: () -> Unit) {
    val c = GymColors.current
    val routines by vm.routines.collectAsState()
    val program by vm.program.collectAsState()
    val active by vm.active.collectAsState()
    var showClass by remember { mutableStateOf(false) }
    val freeTitle = stringResource(R.string.free_workout)
    fun go(r: String) { onClose(); nav.navigate(r) }

    ModalBottomSheet(onDismissRequest = onClose, containerColor = c.card) {
        LazyColumn(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            item { Text(stringResource(R.string.start_workout), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = c.text) }
            if (active != null) item {
                SheetRow("▶️", stringResource(R.string.resume_workout)) { go("workout") }
            } else {
                item { SheetRow("🏋️", stringResource(R.string.start_empty)) { vm.startEmpty(freeTitle) { go("workout") } } }
                if (program != null) item {
                    SheetRow("📅", stringResource(R.string.start_program_day)) { vm.startProgramDay { go("workout") } }
                }
                items(routines.size) { i ->
                    val r = routines[i]
                    SheetRow("📋", r.name) { vm.startRoutine(r.id) { go("workout") } }
                }
            }
            item { HorizontalDivider(Modifier.padding(vertical = 6.dp)) }
            item { SheetRow("🏃", stringResource(R.string.cardio_gps)) { go("cardio/gps") } }
            item { SheetRow("⏱️", stringResource(R.string.cardio_timer)) { go("cardio/timer") } }
            item { SheetRow("🔁", stringResource(R.string.cardio_intervals)) { go("cardio/intervals") } }
            item { SheetRow("✍️", stringResource(R.string.cardio_manual)) { go("cardio/manual") } }
            item { SheetRow("🧘", stringResource(R.string.guided_stretch)) { go("stretch") } }
            item { SheetRow("🤸", stringResource(R.string.log_class)) { showClass = true } }
        }
    }
    if (showClass) ClassDialog(vm, onDismiss = { showClass = false; onClose() })
}

@Composable
private fun SheetRow(emoji: String, text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(emoji, fontSize = 20.sp); Spacer(Modifier.width(12.dp))
        Text(text, Modifier.weight(1f), color = GymColors.current.text)
    }
}

/**
 * Class / sport quick log. MET values (Compendium of Physical Activities):
 * yoga 2.5, pilates 3.0, indoor cycling class 8.5, HIIT class 8.0, dance class 7.3, soccer 7.0,
 * basketball 6.5, tennis 7.3, swimming 8.0 (Gym & Ñam), hiking 6.0, martial arts/boxing 7.8
 * (boxing sparring; bag work is 5.5, fast martial arts 10.3), other 5.0. All 2011 Compendium.
 */
@Composable
fun ClassDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    val names = stringArrayResource(R.array.class_types)
    val mets = listOf(2.5, 3.0, 8.5, 8.0, 7.3, 7.0, 6.5, 7.3, 8.0, 6.0, 7.8, 5.0)
    var pick by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf("45") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.log_class)) },
        text = {
            Column {
                Column(Modifier.height(260.dp).verticalScroll(rememberScrollState())) {
                    names.forEachIndexed { i, n ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.RadioButton(selected = pick == i, onClick = { pick = i })
                            Text(n)
                        }
                    }
                }
                OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(3) }, label = { Text(stringResource(R.string.minutes)) })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val m = minutes.toIntOrNull() ?: 0
                if (m > 0) vm.saveClass(names[pick], mets.getOrElse(pick) { 5.0 }, m) { onDismiss() } else onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
