/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/screens/LibraryScreen.kt                  ║
║ Exercise library (300+ built-in + yours):                          ║
║  • search box, favorites ⭐ on top, filters by type, muscle and     ║
║    equipment;                                                      ║
║  • tap = details (or "choose" when opened from a workout/routine); ║
║  • ExerciseDetailScreen: drawing, muscles, equipment, how-to,      ║
║    common mistakes, your records (1RM Epley + Brzycki), progress   ║
║    chart, your rest time, "copy & edit";                           ║
║  • CustomExerciseScreen: make your own or edit a copy.             ║
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.gymcat.app.core.model.Equipment
import com.gymcat.app.core.model.ExerciseType
import com.gymcat.app.core.model.LogMode
import com.gymcat.app.core.model.Muscle
import com.gymcat.app.data.db.CustomExerciseEntity
import com.gymcat.app.ui.Fmt
import com.gymcat.app.ui.MainViewModel
import com.gymcat.app.ui.components.CatArt
import com.gymcat.app.ui.components.GCard
import com.gymcat.app.ui.components.LineChart
import com.gymcat.app.ui.components.Muted
import com.gymcat.app.ui.components.SectionTitle
import com.gymcat.app.ui.components.Stepper
import com.gymcat.app.ui.theme.GymColors
import java.util.UUID

@Composable
fun muscleName(m: Muscle) = stringArrayResource(R.array.muscle_names)[m.ordinal]
@Composable
fun equipmentName(e: Equipment) = stringArrayResource(R.array.equipment_names)[e.ordinal]
@Composable
fun typeName(t: ExerciseType) = stringArrayResource(R.array.exercise_types)[t.ordinal]

@Composable
fun LibraryScreen(vm: MainViewModel, nav: NavHostController) {
    val c = GymColors.current
    val all by vm.exercises.collectAsState()
    val prefs by vm.prefs.collectAsState()
    val pickMode by vm.pickMode.collectAsState()
    var query by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<ExerciseType?>(if (pickMode == "stretch") ExerciseType.STRETCH else if (pickMode == "cardio") ExerciseType.CARDIO else null) }
    var muscle by remember { mutableStateOf<Muscle?>(null) }
    var equip by remember { mutableStateOf<Equipment?>(null) }
    var muscleMenu by remember { mutableStateOf(false) }
    var equipMenu by remember { mutableStateOf(false) }
    val es = vm.es
    val picking = pickMode.isNotBlank()
    androidx.activity.compose.BackHandler(enabled = picking) { vm.pickMode.value = ""; nav.popBackStack() }

    val list = all.filter { e ->
        (type == null || e.type == type) && (muscle == null || muscle in e.primary || muscle in e.secondary) &&
            (equip == null || e.equipment == equip) &&
            (query.isBlank() || e.nameEn.contains(query, true) || e.nameEs.contains(query, true))
    }.sortedWith(compareByDescending<com.gymcat.app.core.model.Exercise> { prefs[it.id]?.favorite == true }.thenBy { it.name(es) })

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (picking) stringResource(R.string.choose_exercise) else stringResource(R.string.library),
                Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.text)
            TextButton(onClick = { nav.navigate("custom/new") }) { Text("＋ " + stringResource(R.string.my_exercise)) }
            if (picking) TextButton(onClick = { vm.pickMode.value = ""; nav.popBackStack() }) { Text(stringResource(R.string.cancel)) }
        }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.search)) }, singleLine = true)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
            items(ExerciseType.entries) { t -> FilterChip(type == t, { type = if (type == t) null else t }, { Text(typeName(t)) }) }
            item {
                FilterChip(muscle != null, { muscleMenu = true }, { Text(muscle?.let { muscleName(it) } ?: stringResource(R.string.muscle)) })
                DropdownMenu(muscleMenu, { muscleMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.all)) }, onClick = { muscle = null; muscleMenu = false })
                    Muscle.entries.forEach { m -> DropdownMenuItem(text = { Text(muscleName(m)) }, onClick = { muscle = m; muscleMenu = false }) }
                }
            }
            item {
                FilterChip(equip != null, { equipMenu = true }, { Text(equip?.let { equipmentName(it) } ?: stringResource(R.string.equipment)) })
                DropdownMenu(equipMenu, { equipMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.all)) }, onClick = { equip = null; equipMenu = false })
                    Equipment.entries.forEach { e -> DropdownMenuItem(text = { Text(equipmentName(e)) }, onClick = { equip = e; equipMenu = false }) }
                }
            }
        }
        Muted(stringResource(R.string.n_exercises, list.size), size = 12)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list, key = { it.id }) { e ->
                val fav = prefs[e.id]?.favorite == true
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    CatArt(e.catArt, e.emoji, 40.dp)
                    TextButton(onClick = {
                        if (picking) { vm.onPicked(e.id); nav.popBackStack() } else nav.navigate("exercise/${e.id}")
                    }, modifier = Modifier.weight(1f)) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(e.name(es), color = c.text, fontWeight = FontWeight.Medium)
                            Muted(e.primary.joinToString { muscleName(it) } + " · " + equipmentName(e.equipment), size = 12)
                        }
                    }
                    IconButton(onClick = { vm.toggleFavorite(e.id) }) {
                        Icon(if (fav) Icons.Filled.Star else Icons.Filled.StarBorder, stringResource(R.string.favorite), tint = if (fav) c.warn else c.text2)
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun ExerciseDetailScreen(vm: MainViewModel, nav: NavHostController, id: String) {
    val c = GymColors.current
    val s by vm.settings.collectAsState()
    val prefs by vm.prefs.collectAsState()
    val records by vm.records.collectAsState()
    vm.exercises.collectAsState().value                      // refresh when customs change
    val e = vm.ex(id) ?: run { Muted("?"); return }
    val es = vm.es
    val rec = records.firstOrNull { it.exerciseId == id }
    val pref = prefs[id]

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) { Text("‹ " + stringResource(R.string.back)) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { vm.toggleFavorite(id) }) {
                Icon(if (pref?.favorite == true) Icons.Filled.Star else Icons.Filled.StarBorder, stringResource(R.string.favorite))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CatArt(e.catArt, e.emoji, 96.dp)
            Column(Modifier.padding(start = 12.dp)) {
                Text(e.name(es), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.text)
                Muted(typeName(e.type) + " · " + equipmentName(e.equipment))
            }
        }
        GCard {
            Text(stringResource(R.string.muscles_worked), fontWeight = FontWeight.SemiBold, color = c.text)
            Text(stringResource(R.string.primary) + ": " + e.primary.joinToString { muscleName(it) }, color = c.text)
            if (e.secondary.isNotEmpty()) Muted(stringResource(R.string.secondary) + ": " + e.secondary.joinToString { muscleName(it) })
        }
        GCard {
            Text(stringResource(R.string.how_to), fontWeight = FontWeight.SemiBold, color = c.text)
            Text(e.howTo(es), color = c.text)
            Spacer(Modifier.height(8.dp))
            Text("⚠️ " + stringResource(R.string.mistakes), fontWeight = FontWeight.SemiBold, color = c.text)
            Text(e.mistakes(es), color = c.text)
        }
        if (rec != null) GCard {
            Text(stringResource(R.string.your_records), fontWeight = FontWeight.SemiBold, color = c.text)
            when (e.mode) {
                LogMode.WEIGHT_REPS -> {
                    Text(stringResource(R.string.heaviest) + ": " + Fmt.weight(rec.heaviestLb, s.metric), color = c.text)
                    Text(stringResource(R.string.e1rm_both, Fmt.weight(rec.bestEpley, s.metric), Fmt.weight(rec.bestBrzycki, s.metric)), color = c.text)
                    val prog = vm.exerciseProgress(id)
                    if (prog.size >= 2) {
                        Muted(stringResource(R.string.chart_e1rm_legend), size = 12)
                        LineChart(listOf(prog.map { it.first } to c.accent, prog.map { it.second } to c.navy, prog.map { it.third } to c.warn))
                    }
                }
                LogMode.BODYWEIGHT_REPS -> Text(stringResource(R.string.most_reps) + ": ${rec.mostReps}", color = c.text)
                LogMode.HOLD, LogMode.STRETCH -> Text(stringResource(R.string.longest_hold) + ": " + Fmt.duration(rec.longestSec.toLong()), color = c.text)
                LogMode.CARDIO -> Text(stringResource(R.string.longest) + ": " + Fmt.dist(rec.longestMi, s.metric), color = c.text)
            }
        }
        if (e.mode != LogMode.CARDIO && e.mode != LogMode.STRETCH) GCard {
            val rest = pref?.restSec ?: 0
            Stepper(stringResource(R.string.your_rest), if (rest == 0) stringResource(R.string.default_short) else Fmt.duration(rest.toLong()),
                { vm.setRest(id, (rest - 15).coerceAtLeast(0)) },
                { vm.setRest(id, (if (rest == 0) defaultRest(e, s.restCompound, s.restIsolation, s.restCalisthenics) else rest) + 15) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { nav.navigate("custom/" + (if (e.custom) id else "copy_$id")) }) {
                Text(if (e.custom) stringResource(R.string.edit) else stringResource(R.string.copy_and_edit))
            }
            if (e.custom) TextButton(onClick = { vm.deleteCustom(id); nav.popBackStack() }) { Text(stringResource(R.string.delete), color = c.bad) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** id = "new", "copy_<builtinId>" or an existing custom id. */
@Composable
fun CustomExerciseScreen(vm: MainViewModel, nav: NavHostController, id: String) {
    val c = GymColors.current
    val es = vm.es
    val base = when {
        id.startsWith("copy_") -> vm.ex(id.removePrefix("copy_"))
        id != "new" -> vm.ex(id)
        else -> null
    }
    var name by remember { mutableStateOf(base?.let { if (id.startsWith("copy_")) it.name(es) + " *" else it.name(es) } ?: "") }
    var type by remember { mutableStateOf(base?.type ?: ExerciseType.STRENGTH) }
    var equip by remember { mutableStateOf(base?.equipment ?: Equipment.DUMBBELL) }
    var mode by remember { mutableStateOf(base?.mode ?: LogMode.WEIGHT_REPS) }
    var primary by remember { mutableStateOf(base?.primary?.toSet() ?: emptySet()) }
    var howTo by remember { mutableStateOf(base?.howTo(es) ?: "") }
    var mistakes by remember { mutableStateOf(base?.mistakes(es) ?: "") }
    var compound by remember { mutableStateOf(base?.compound ?: false) }
    var emoji by remember { mutableStateOf(base?.emoji ?: "🏋️") }
    val modeNames = stringArrayResource(R.array.log_modes)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) { Text(stringResource(R.string.cancel)) }
            Spacer(Modifier.weight(1f))
            Button(enabled = name.isNotBlank(), onClick = {
                val newId = if (id == "new" || id.startsWith("copy_")) "c_" + UUID.randomUUID().toString().take(8) else id
                vm.saveCustom(CustomExerciseEntity(
                    id = newId, name = name.trim(), type = type.name, equipment = equip.name,
                    primary = primary.joinToString(",") { it.name }, secondary = base?.secondary?.joinToString(",") { it.name } ?: "",
                    mode = mode.name, howTo = howTo, mistakes = mistakes, bwPct = base?.bwPct ?: if (mode == LogMode.BODYWEIGHT_REPS) 1.0 else 0.0,
                    compound = compound, emoji = emoji.ifBlank { "🏋️" }, met = base?.met ?: 5.0,
                    copiedFrom = if (id.startsWith("copy_")) id.removePrefix("copy_") else ""))
                nav.popBackStack()
            }) { Text(stringResource(R.string.save)) }
        }
        OutlinedTextField(name, { name = it.take(60) }, label = { Text(stringResource(R.string.name)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(emoji, { emoji = it.take(4) }, label = { Text(stringResource(R.string.emoji)) })
        SectionTitle(stringResource(R.string.type))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(ExerciseType.entries) { t -> FilterChip(type == t, { type = t }, { Text(typeName(t)) }) } }
        SectionTitle(stringResource(R.string.how_logged))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(LogMode.entries) { m -> FilterChip(mode == m, { mode = m }, { Text(modeNames[m.ordinal]) }) } }
        SectionTitle(stringResource(R.string.equipment))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(Equipment.entries) { e -> FilterChip(equip == e, { equip = e }, { Text(equipmentName(e)) }) } }
        SectionTitle(stringResource(R.string.muscles_worked))
        Muscle.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { m -> FilterChip(m in primary, { primary = if (m in primary) primary - m else primary + m }, { Text(muscleName(m), fontSize = 12.sp) }) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.compound_lift), Modifier.weight(1f), color = c.text)
            Switch(compound, { compound = it })
        }
        OutlinedTextField(howTo, { howTo = it.take(600) }, label = { Text(stringResource(R.string.how_to)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(mistakes, { mistakes = it.take(400) }, label = { Text(stringResource(R.string.mistakes)) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }
}
