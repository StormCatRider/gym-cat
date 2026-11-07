/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/screens/RoutinesScreen.kt                 ║
║ Routines tab:                                                      ║
║  • Built-in programs (beginner full body, dumbbell home,            ║
║    calisthenics ladders, cardio + stretch) — start / stop.         ║
║  • Your routines — start, edit, share as a file, show a QR, delete.║
║  • Import a friend's routine from a file or by scanning a QR.      ║
║ RoutineEditorScreen: per exercise you set sets × rep range,        ║
║ target RPE, rest time, auto warm-ups and supersets.                ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.gymcat.app.R
import com.gymcat.app.core.model.LogMode
import com.gymcat.app.core.programs.Programs
import com.gymcat.app.data.db.RoutineItemEntity
import com.gymcat.app.ui.Fmt
import com.gymcat.app.ui.MainViewModel
import com.gymcat.app.ui.components.GCard
import com.gymcat.app.ui.components.Muted
import com.gymcat.app.ui.components.Pill
import com.gymcat.app.ui.components.SectionTitle
import com.gymcat.app.ui.components.Stepper
import com.gymcat.app.ui.theme.GymColors
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun RoutinesScreen(vm: MainViewModel, nav: NavHostController) {
    val c = GymColors.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val routines by vm.routines.collectAsState()
    val items by vm.routineItems.collectAsState()
    val program by vm.program.collectAsState()
    val active by vm.active.collectAsState()
    var confirmDelete by remember { mutableStateOf<Long?>(null) }
    val es = vm.es
    val importedFmt = stringResource(R.string.imported_routine)
    val badCode = stringResource(R.string.import_failed)

    // Import from a file the user picks.
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()
            if (text != null) vm.importCode(text) { name -> vm.toast.value = if (name != null) importedFmt.format(name) else badCode }
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tab_routines), Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = c.text)
                TextButton(onClick = { vm.pickMode.value = ""; nav.navigate("library") }) { Text("📚 " + stringResource(R.string.library)) }
            }
        }
        item { SectionTitle(stringResource(R.string.programs)) }
        items(Programs.all, key = { it.id }) { p ->
            val isActive = program?.programId == p.id
            GCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name(es), Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = c.text)
                    if (isActive) Pill(stringResource(R.string.active))
                }
                Muted(p.desc(es))
                Muted(stringResource(R.string.days_per_week_n, p.daysPerWeek))
                Row {
                    if (isActive) {
                        TextButton(onClick = { vm.startProgramDay { nav.navigate("workout") } }, enabled = active == null) { Text(stringResource(R.string.start_next_day)) }
                        TextButton(onClick = { vm.stopProgram() }) { Text(stringResource(R.string.stop_program)) }
                    } else TextButton(onClick = { vm.activateProgram(p.id) }) { Text(stringResource(R.string.follow_program)) }
                }
            }
        }
        item {
            SectionTitle(stringResource(R.string.my_routines))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.editRoutine(0); nav.navigate("routine") }) { Text("＋ " + stringResource(R.string.new_routine)) }
                OutlinedButton(onClick = { openFile.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.import_file)) }
                OutlinedButton(onClick = { nav.navigate("scan") }) { Text(stringResource(R.string.scan_qr)) }
            }
        }
        if (routines.isEmpty()) item { Muted(stringResource(R.string.no_routines)) }
        items(routines, key = { it.id }) { r ->
            var menu by remember { mutableStateOf(false) }
            GCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = c.text)
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, null) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.edit)) }, onClick = { menu = false; vm.editRoutine(r.id); nav.navigate("routine") })
                        DropdownMenuItem(text = { Text(stringResource(R.string.share_file)) }, onClick = {
                            menu = false
                            scope.launch { vm.shareCode(r.id)?.let { shareRoutineFile(ctx, r.name, it) } }
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.show_qr)) }, onClick = { menu = false; nav.navigate("qr/${r.id}") })
                        DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { menu = false; confirmDelete = r.id })
                    }
                }
                Muted((items[r.id] ?: emptyList()).joinToString(" · ") { vm.exName(it.exerciseId) }, size = 12)
                TextButton(onClick = { vm.startRoutine(r.id) { nav.navigate("workout") } }, enabled = active == null) { Text(stringResource(R.string.start)) }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    confirmDelete?.let { id ->
        AlertDialog(onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.delete_routine)) },
            confirmButton = { TextButton(onClick = { vm.deleteRoutine(id); confirmDelete = null }) { Text(stringResource(R.string.delete), color = c.bad) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

/** Saves the code as "<name>.gymcat" and opens the share sheet (WhatsApp, email…). */
fun shareRoutineFile(ctx: Context, name: String, code: String) {
    val safe = name.replace(Regex("[^A-Za-z0-9 _-]"), "").ifBlank { "routine" }
    val f = File(ctx.cacheDir, "$safe.gymcat")
    f.writeText(code)
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/octet-stream")
        .putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_TEXT, code)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null))
}

@Composable
fun RoutineEditorScreen(vm: MainViewModel, nav: NavHostController) {
    val c = GymColors.current
    val routine by vm.editingRoutine.collectAsState()
    val items by vm.editingItems.collectAsState()
    val s by vm.settings.collectAsState()
    fun setItems(l: List<RoutineItemEntity>) { vm.editingItems.value = l }
    fun update(i: Int, f: (RoutineItemEntity) -> RoutineItemEntity) = setItems(items.toMutableList().also { it[i] = f(it[i]) })
    val nameHint = stringResource(R.string.routine_name)

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { nav.popBackStack() }) { Text(stringResource(R.string.cancel)) }
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    val name = routine.name.ifBlank { nameHint }
                    vm.saveRoutine(routine.copy(name = name), items) { nav.popBackStack() }
                }, enabled = items.isNotEmpty()) { Text(stringResource(R.string.save)) }
            }
            OutlinedTextField(routine.name, { vm.editingRoutine.value = routine.copy(name = it.take(60)) },
                label = { Text(nameHint) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        itemsIndexed(items) { i, item ->
            val e = vm.ex(item.exerciseId)
            GCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text((e?.emoji ?: "🏋️") + " " + vm.exName(item.exerciseId), Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = c.text)
                    IconButton(onClick = { if (i > 0) setItems(items.toMutableList().also { l -> l.add(i - 1, l.removeAt(i)) }) }) { Icon(Icons.Filled.ArrowUpward, stringResource(R.string.move_up)) }
                    IconButton(onClick = { if (i < items.lastIndex) setItems(items.toMutableList().also { l -> l.add(i + 1, l.removeAt(i)) }) }) { Icon(Icons.Filled.ArrowDownward, stringResource(R.string.move_down)) }
                    IconButton(onClick = { setItems(items.toMutableList().also { l -> l.removeAt(i) }) }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                }
                when (e?.mode) {
                    LogMode.CARDIO -> Stepper(stringResource(R.string.minutes), item.minutes.toString(),
                        { update(i) { x -> x.copy(minutes = (x.minutes - 5).coerceAtLeast(1)) } }, { update(i) { x -> x.copy(minutes = x.minutes + 5) } })
                    LogMode.STRETCH, LogMode.HOLD -> Row {
                        if (e?.mode == LogMode.HOLD) Stepper(stringResource(R.string.sets), item.sets.toString(),
                            { update(i) { x -> x.copy(sets = (x.sets - 1).coerceAtLeast(1)) } }, { update(i) { x -> x.copy(sets = x.sets + 1) } })
                        Stepper(stringResource(R.string.seconds), item.holdSec.toString(),
                            { update(i) { x -> x.copy(holdSec = (x.holdSec - 5).coerceAtLeast(5)) } }, { update(i) { x -> x.copy(holdSec = x.holdSec + 5) } })
                    }
                    else -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Stepper(stringResource(R.string.sets), item.sets.toString(),
                                { update(i) { x -> x.copy(sets = (x.sets - 1).coerceAtLeast(1)) } }, { update(i) { x -> x.copy(sets = x.sets + 1) } })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Stepper(stringResource(R.string.reps_min), item.repMin.toString(),
                                { update(i) { x -> x.copy(repMin = (x.repMin - 1).coerceAtLeast(1)) } },
                                { update(i) { x -> x.copy(repMin = x.repMin + 1, repMax = maxOf(x.repMax, x.repMin + 1)) } })
                            Stepper(stringResource(R.string.reps_max), item.repMax.toString(),
                                { update(i) { x -> x.copy(repMax = (x.repMax - 1).coerceAtLeast(x.repMin)) } }, { update(i) { x -> x.copy(repMax = x.repMax + 1) } })
                        }
                        Muted(stringResource(R.string.target_rpe))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(listOf<Double?>(null, 6.0, 7.0, 7.5, 8.0, 8.5, 9.0)) { r ->
                                FilterChip(selected = item.targetRpe == r, onClick = { update(i) { x -> x.copy(targetRpe = r) } },
                                    label = { Text(r?.let { v -> "RPE ${Fmt.num(v)}" } ?: stringResource(R.string.none)) })
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.auto_warmups), Modifier.weight(1f), color = c.text)
                            Switch(item.warmups, { v -> update(i) { x -> x.copy(warmups = v) } })
                        }
                    }
                }
                if (e?.mode != LogMode.CARDIO && e?.mode != LogMode.STRETCH) {
                    Stepper(stringResource(R.string.rest), if (item.restSec == 0) stringResource(R.string.default_short) else Fmt.duration(item.restSec.toLong()),
                        { update(i) { x -> x.copy(restSec = (x.restSec - 15).coerceAtLeast(0)) } },
                        { update(i) { x -> x.copy(restSec = (if (x.restSec == 0) defaultRest(e, s.restCompound, s.restIsolation, s.restCalisthenics) else x.restSec) + 15) } })
                    if (i < items.lastIndex) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.superset_with_next), Modifier.weight(1f), color = c.text)
                        val linked = item.superset != 0 && items[i + 1].superset == item.superset
                        Switch(linked, { v ->
                            val g = if (v) i + 1 else 0
                            setItems(items.toMutableList().also { l -> l[i] = l[i].copy(superset = g); l[i + 1] = l[i + 1].copy(superset = g) })
                        })
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { vm.pickMode.value = "routine"; nav.navigate("library") }, modifier = Modifier.fillMaxWidth()) {
                Text("＋ " + stringResource(R.string.add_exercise))
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

fun defaultRest(e: com.gymcat.app.core.model.Exercise?, compound: Int, isolation: Int, calis: Int): Int = when {
    e == null -> isolation
    e.type == com.gymcat.app.core.model.ExerciseType.CALISTHENICS -> calis
    e.compound -> compound
    else -> isolation
}
