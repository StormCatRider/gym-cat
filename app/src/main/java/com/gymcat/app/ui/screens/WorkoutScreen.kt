/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/screens/WorkoutScreen.kt                  ║
║ The live workout. For each exercise:                               ║
║  • header: name, target (3 × 8–12 @ RPE 8), last time, and the     ║
║    RPE-based suggestion ("Try 190 lb") you can tap to apply;       ║
║  • BIG set cards with − / + buttons (prefilled from last time);    ║
║    set type chip (warm-up / working / drop / failure), RPE, note;  ║
║  • ✓ = set done → rest timer starts by itself (vibrates + beeps);  ║
║  • menu: swap (machine busy), skip, move up/down, superset with    ║
║    next, plate calculator.                                         ║
║ Estimated 1RM shows BOTH Epley and Brzycki on finished sets.       ║
║ The screen stays awake (Settings) and a notification shows time.  ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.gymcat.app.R
import com.gymcat.app.core.calc.BodyMath
import com.gymcat.app.core.calc.PlateMath
import com.gymcat.app.core.calc.StrengthMath
import com.gymcat.app.core.model.Equipment
import com.gymcat.app.core.model.LogMode
import com.gymcat.app.core.model.SetType
import com.gymcat.app.data.db.SetEntity
import com.gymcat.app.service.WorkoutClock
import com.gymcat.app.ui.Fmt
import com.gymcat.app.ui.MainViewModel
import com.gymcat.app.ui.components.Cat
import com.gymcat.app.ui.components.CatArt
import com.gymcat.app.ui.components.GCard
import com.gymcat.app.ui.components.Muted
import com.gymcat.app.ui.components.NumberDialog
import com.gymcat.app.ui.components.Pill
import com.gymcat.app.ui.components.Stepper
import com.gymcat.app.ui.theme.GymColors
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreen(vm: MainViewModel, nav: NavHostController) {
    val c = GymColors.current
    val ctx = LocalContext.current
    val active by vm.active.collectAsState()
    val sets by vm.activeSets.collectAsState()
    val s by vm.settings.collectAsState()
    val restEnd by WorkoutClock.restEndMs.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showFinish by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(500) } }

    // Keep the screen awake during the workout (Settings → Keep screen on).
    DisposableEffect(s.keepScreenOn) {
        val w = (ctx as? Activity)?.window
        if (s.keepScreenOn) w?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { w?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    // Ask for notification permission the first time (for the timer notification).
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (s.workoutNotification && android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val w = active
    if (w == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CatArt(Cat.EMPTY, "🎒", 120.dp)
            Muted(stringResource(R.string.no_active_workout))
            TextButton(onClick = { nav.popBackStack() }) { Text(stringResource(R.string.back)) }
        }
        return
    }

    // Restart the timer notification if the app was closed mid-workout (safe to call twice).
    LaunchedEffect(w.id) { com.gymcat.app.service.WorkoutService.start(ctx, w.title, w.startMs) }
    val groups = sets.groupBy { it.exerciseOrder }.toSortedMap()
    Column(Modifier.fillMaxSize()) {
        // ── Top bar: title, running time, finish
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(w.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = c.text, maxLines = 1)
                Muted("⏱ " + Fmt.duration((now - w.startMs) / 1000))
            }
            TextButton(onClick = { showDiscard = true }) { Text(stringResource(R.string.discard), color = c.bad) }
            Button(onClick = { showFinish = true }) { Text(stringResource(R.string.finish)) }
        }

        // ── Rest timer banner
        if (restEnd > now) {
            val left = ((restEnd - now) / 1000).toInt()
            val total = WorkoutClock.restTotalSec.value.coerceAtLeast(1)
            GCard(Modifier.padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CatArt(Cat.REST, "🧃", 44.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Muted(stringResource(R.string.rest))
                        Text(Fmt.duration(left.toLong()), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.text)
                    }
                    TextButton(onClick = { WorkoutClock.addRest(15) }) { Text("+15s") }
                    TextButton(onClick = { WorkoutClock.skipRest() }) { Text(stringResource(R.string.skip)) }
                }
                LinearProgressIndicator(progress = { left / total.toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), color = c.accent)
            }
        } else Row(Modifier.padding(horizontal = 16.dp).horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
            // Manual rest timer (you chose auto + manual)
            listOf(60, 90, 120, 180).forEach { sec ->
                TextButton(onClick = { WorkoutClock.startRest(sec) }) { Text("⏲ " + Fmt.duration(sec.toLong()), fontSize = 12.sp) }
            }
        }

        LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val orders = groups.keys.toList()
            items(orders, key = { it }) { order ->
                val list = groups[order] ?: emptyList()
                val nextSameSuperset = groups[order + 1]?.firstOrNull()?.superset?.let { it != 0 && it == list.firstOrNull()?.superset } == true
                ExerciseBlock(vm, nav, order, list, isLast = order == orders.last(), nextSameSuperset = nextSameSuperset)
            }
            item {
                OutlinedButton(onClick = { vm.pickMode.value = "workout"; nav.navigate("library") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("＋ " + stringResource(R.string.add_exercise))
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showDiscard) AlertDialog(
        onDismissRequest = { showDiscard = false },
        title = { Text(stringResource(R.string.discard_title)) },
        text = { Text(stringResource(R.string.discard_body)) },
        confirmButton = { TextButton(onClick = { showDiscard = false; vm.discardWorkout(); nav.popBackStack() }) { Text(stringResource(R.string.discard), color = c.bad) } },
        dismissButton = { TextButton(onClick = { showDiscard = false }) { Text(stringResource(R.string.cancel)) } },
    )
    val emptyMsg = stringResource(R.string.nothing_logged)
    if (showFinish) FinishDialog(hasRoutine = w.routineId != null, onDismiss = { showFinish = false }) { rating, notes, saveBack ->
        showFinish = false
        vm.finishWorkout(rating, notes, saveBack, emptyMsg) { saved ->
            if (saved) nav.navigate("finish") { popUpTo("home") } else nav.popBackStack("home", inclusive = false)
        }
    }
}

@Composable
private fun ExerciseBlock(vm: MainViewModel, nav: NavHostController, order: Int, sets: List<SetEntity>, isLast: Boolean, nextSameSuperset: Boolean) {
    val c = GymColors.current
    val s by vm.settings.collectAsState()
    val first = sets.firstOrNull() ?: return
    val e = vm.ex(first.exerciseId)
    var menu by remember { mutableStateOf(false) }
    var plates by remember { mutableStateOf<Double?>(null) }
    val es = vm.es
    val inc = if (s.metric) BodyMath.kgToLb(2.5) else 5.0

    // Last session + RPE suggestion
    val last by produceState(emptyList<SetEntity>(), first.exerciseId) { value = vm.repo.dao.lastSessionSets(first.exerciseId) }
    val lastTop = last.filter { it.type != SetType.WARMUP.name }.maxByOrNull { StrengthMath.epley(it.lb, it.reps) }
    val suggestion = if (e?.mode == LogMode.WEIGHT_REPS && lastTop != null)
        StrengthMath.suggestNext(lastTop.lb, lastTop.reps, lastTop.rpe, first.targetMax.takeIf { it > 0 } ?: lastTop.reps, first.targetRpe, inc)
    else null

    GCard(Modifier.animateContentSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CatArt(e?.catArt, e?.emoji ?: "🏋️", 40.dp)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(e?.name(es) ?: first.exerciseId, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = c.text,
                    modifier = Modifier.padding(bottom = 2.dp))
                val target = buildString {
                    val working = sets.count { it.type != SetType.WARMUP.name }
                    if (first.targetMax > 0) append("$working × ${if (first.targetMin != first.targetMax && first.targetMin > 0) "${first.targetMin}–" else ""}${first.targetMax}")
                    first.targetRpe?.let { append(" @ RPE ${Fmt.num(it)}") }
                }
                if (target.isNotBlank()) Muted(target)
            }
            if (first.superset != 0) Pill(stringResource(R.string.superset))
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, null) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.how_to)) }, onClick = { menu = false; nav.navigate("exercise/${first.exerciseId}") })
                DropdownMenuItem(text = { Text(stringResource(R.string.swap_exercise)) }, onClick = { menu = false; vm.pickMode.value = "swap:$order"; nav.navigate("library") })
                DropdownMenuItem(text = { Text(stringResource(R.string.skip_exercise)) }, onClick = { menu = false; vm.skipExercise(order) })
                DropdownMenuItem(text = { Text(stringResource(R.string.move_up)) }, onClick = { menu = false; vm.moveExercise(order, -1) })
                DropdownMenuItem(text = { Text(stringResource(R.string.move_down)) }, onClick = { menu = false; vm.moveExercise(order, +1) })
                if (!isLast) DropdownMenuItem(text = { Text(stringResource(R.string.superset_with_next)) }, onClick = { menu = false; vm.toggleSuperset(order) })
                if (e != null && e.mode == LogMode.WEIGHT_REPS && (e.equipment == Equipment.BARBELL || e.equipment == Equipment.SMITH))
                    DropdownMenuItem(text = { Text(stringResource(R.string.plate_calculator)) }, onClick = {
                        menu = false; plates = sets.firstOrNull { !it.done && it.type != SetType.WARMUP.name }?.lb ?: first.lb })
            }
        }
        // Last time + suggestion chip
        if (lastTop != null) {
            val lastTxt = when (e?.mode) {
                LogMode.WEIGHT_REPS -> "${Fmt.weight(lastTop.lb, s.metric)} × ${lastTop.reps}" + (lastTop.rpe?.let { " @${Fmt.num(it)}" } ?: "")
                LogMode.BODYWEIGHT_REPS -> "${lastTop.reps} reps"
                LogMode.HOLD, LogMode.STRETCH -> Fmt.duration(lastTop.seconds.toLong())
                else -> Fmt.dist(lastTop.miles, s.metric)
            }
            Muted(stringResource(R.string.last_time, lastTxt))
        }
        if (suggestion != null && suggestion != lastTop?.lb) Pill(stringResource(R.string.suggested_weight, Fmt.weight(suggestion, s.metric)),
            modifier = Modifier.padding(top = 4.dp)) {
            sets.filter { !it.done && it.type != SetType.WARMUP.name }.forEach { vm.updateSet(it.copy(lb = suggestion)) }
        }
        Spacer(Modifier.height(8.dp))
        // In a superset, the rest timer waits until you finish the partner exercise's set.
        sets.forEach { set ->
            SetCard(vm, set, e?.mode ?: LogMode.WEIGHT_REPS, e?.equipment == Equipment.DUMBBELL, inc,
                nextSameSuperset = nextSameSuperset)
            Spacer(Modifier.height(8.dp))
        }
        TextButton(onClick = { vm.addSet(order) }) { Text("＋ " + stringResource(R.string.add_set)) }
    }
    plates?.let { PlateDialog(it, s.plateKg, s.metric) { plates = null } }
}

/**
 * One tap of − or + on a weight. In lb: ±5 lb. In kg: ±2.5 kg and SNAP to the 2.5 kg grid,
 * so 135 lb (61.2 kg) + tap → 62.5 kg (137.8 lb), not 63.7 kg.
 * allowNegative = bodyweight sets (negative = band/machine assistance).
 */
private fun stepWeight(lb: Double, dir: Int, metric: Boolean, allowNegative: Boolean): Double {
    val next = if (metric) {
        val kg = BodyMath.lbToKg(lb)
        val snapped = if (dir > 0) kotlin.math.floor(kg / 2.5 + 1e-6) * 2.5 + 2.5 else kotlin.math.ceil(kg / 2.5 - 1e-6) * 2.5 - 2.5
        BodyMath.kgToLb(snapped)
    } else {
        if (dir > 0) kotlin.math.floor(lb / 5 + 1e-6) * 5 + 5 else kotlin.math.ceil(lb / 5 - 1e-6) * 5 - 5
    }
    return if (allowNegative) next else next.coerceAtLeast(0.0)
}

@Composable
private fun SetCard(vm: MainViewModel, set: SetEntity, mode: LogMode, dumbbell: Boolean, incLb: Double, nextSameSuperset: Boolean) {
    val c = GymColors.current
    val s by vm.settings.collectAsState()
    var edit by remember { mutableStateOf<String?>(null) }      // which number is being typed
    var noteOpen by remember { mutableStateOf(set.note.isNotBlank()) }
    val bg = if (set.done) c.accent.copy(alpha = 0.14f) else c.track
    val types = SetType.entries
    val typeLabels = listOf(stringResource(R.string.set_warmup), stringResource(R.string.set_working),
        stringResource(R.string.set_drop), stringResource(R.string.set_failure))

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(bg).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("#${set.setIndex}", fontWeight = FontWeight.Bold, color = c.text)
            Spacer(Modifier.width(8.dp))
            val ti = types.indexOfFirst { it.name == set.type }.coerceAtLeast(0)
            Pill(typeLabels[ti], color = if (ti == 0) c.warn else c.accent) {
                vm.updateSet(set.copy(type = types[(ti + 1) % types.size].name))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { vm.deleteSet(set) }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete), tint = c.text2) }
            Button(onClick = { vm.completeSet(set, nextSameSuperset) }, shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (set.done) c.good else c.accent)) {
                Icon(Icons.Filled.Check, stringResource(R.string.done), tint = Color.White)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val wUnit = Fmt.weightUnit(s.metric)
            when (mode) {
                LogMode.WEIGHT_REPS -> {
                    Stepper((if (dumbbell) stringResource(R.string.per_dumbbell) + " " else "") + wUnit, Fmt.weightNum(set.lb, s.metric),
                        { vm.updateSet(set.copy(lb = stepWeight(set.lb, -1, s.metric, false))) }, { vm.updateSet(set.copy(lb = stepWeight(set.lb, +1, s.metric, false))) },
                        onValueClick = { edit = "lb" })
                    Stepper(stringResource(R.string.reps), set.reps.toString(),
                        { vm.updateSet(set.copy(reps = (set.reps - 1).coerceAtLeast(0))) }, { vm.updateSet(set.copy(reps = set.reps + 1)) },
                        onValueClick = { edit = "reps" })
                }
                LogMode.BODYWEIGHT_REPS -> {
                    Stepper("± $wUnit", (if (set.lb > 0) "+" else "") + Fmt.weightNum(set.lb, s.metric),
                        { vm.updateSet(set.copy(lb = stepWeight(set.lb, -1, s.metric, true))) }, { vm.updateSet(set.copy(lb = stepWeight(set.lb, +1, s.metric, true))) },
                        onValueClick = { edit = "lb" })
                    Stepper(stringResource(R.string.reps), set.reps.toString(),
                        { vm.updateSet(set.copy(reps = (set.reps - 1).coerceAtLeast(0))) }, { vm.updateSet(set.copy(reps = set.reps + 1)) },
                        onValueClick = { edit = "reps" })
                }
                LogMode.HOLD, LogMode.STRETCH -> {
                    if (mode == LogMode.HOLD) Stepper("± $wUnit", Fmt.weightNum(set.lb, s.metric),
                        { vm.updateSet(set.copy(lb = stepWeight(set.lb, -1, s.metric, true))) }, { vm.updateSet(set.copy(lb = stepWeight(set.lb, +1, s.metric, true))) }, onValueClick = { edit = "lb" })
                    Stepper(stringResource(R.string.seconds), set.seconds.toString(),
                        { vm.updateSet(set.copy(seconds = (set.seconds - 5).coerceAtLeast(0))) }, { vm.updateSet(set.copy(seconds = set.seconds + 5)) },
                        onValueClick = { edit = "sec" })
                }
                LogMode.CARDIO -> {
                    Stepper(stringResource(R.string.minutes), (set.seconds / 60).toString(),
                        { vm.updateSet(set.copy(seconds = (set.seconds - 60).coerceAtLeast(0))) }, { vm.updateSet(set.copy(seconds = set.seconds + 60)) },
                        onValueClick = { edit = "min" })
                    Stepper(Fmt.distUnit(s.metric), Fmt.distNum(set.miles, s.metric),
                        { vm.updateSet(set.copy(miles = (set.miles - 0.1).coerceAtLeast(0.0))) }, { vm.updateSet(set.copy(miles = set.miles + 0.1)) },
                        onValueClick = { edit = "mi" })
                }
            }
        }
        if (mode == LogMode.WEIGHT_REPS || mode == LogMode.BODYWEIGHT_REPS) {
            // RPE chips (how hard it felt). RIR = 10 − RPE.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(listOf(6.0, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0)) { r ->
                    FilterChip(selected = set.rpe == r, onClick = { vm.updateSet(set.copy(rpe = if (set.rpe == r) null else r)) },
                        label = { Text("RPE ${Fmt.num(r)}", fontSize = 11.sp) })
                }
            }
        }
        if (set.done && mode == LogMode.WEIGHT_REPS && set.reps > 0) {
            val load = set.lb
            Muted(stringResource(R.string.e1rm_both, Fmt.weight(StrengthMath.epley(load, set.reps), s.metric),
                Fmt.weight(StrengthMath.brzycki(load, set.reps), s.metric)), size = 12)
        }
        if (noteOpen) OutlinedTextField(set.note, { vm.updateSet(set.copy(note = it.take(200))) }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.note)) }, singleLine = true)
        else TextButton(onClick = { noteOpen = true }) { Text("📝 " + stringResource(R.string.add_note), fontSize = 12.sp) }
    }

    edit?.let { field ->
        val current = when (field) {
            "lb" -> Fmt.weightNum(set.lb, s.metric); "reps" -> set.reps.toString(); "sec" -> set.seconds.toString()
            "min" -> (set.seconds / 60).toString(); else -> Fmt.distNum(set.miles, s.metric)
        }
        NumberDialog(current, onDismiss = { edit = null }) { v ->
            edit = null
            vm.updateSet(when (field) {
                "lb" -> set.copy(lb = Fmt.toLb(v, s.metric))
                "reps" -> set.copy(reps = v.toInt().coerceAtLeast(0))
                "sec" -> set.copy(seconds = v.toInt().coerceAtLeast(0))
                "min" -> set.copy(seconds = (v * 60).toInt().coerceAtLeast(0))
                else -> set.copy(miles = Fmt.toMi(v, s.metric).coerceAtLeast(0.0))
            })
        }
    }
}

/**
 * Plate calculator: per side = (target − bar) ÷ 2, filled heaviest-first.
 * lb set: 45 bar, 45/35/25/10/5/2.5 plates. kg set (Settings): 20 bar, 25/20/15/10/5/2.5/1.25.
 */
@Composable
fun PlateDialog(targetLb: Double, kgPlates: Boolean, metric: Boolean, onDismiss: () -> Unit) {
    var target by remember { mutableStateOf(targetLb) }
    val res = if (kgPlates) PlateMath.perSide(BodyMath.lbToKg(target), PlateMath.KG_BAR, PlateMath.KG_PLATES)
    else PlateMath.perSide(target, PlateMath.LB_BAR, PlateMath.LB_PLATES)
    val unit = if (kgPlates) "kg" else "lb"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plate_calculator)) },
        text = {
            Column {
                Stepper(stringResource(R.string.target), Fmt.weight(target, metric),
                    { target = (target - 5).coerceAtLeast(0.0) }, { target += 5 },
                    editValue = Fmt.weightNum(target, metric), onSet = { v -> target = Fmt.toLb(v, metric).coerceIn(0.0, 2000.0) })
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.bar_is, "${Fmt.num(if (kgPlates) PlateMath.KG_BAR else PlateMath.LB_BAR)} $unit"))
                Text(stringResource(R.string.per_side) + ": " + if (res.perSide.isEmpty()) "—" else res.perSide.joinToString(" + ") { Fmt.num(it) } + " $unit",
                    fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (res.leftover > 0.01) Muted(stringResource(R.string.plates_leftover, Fmt.num(res.leftover) + " $unit"))
                Muted(stringResource(R.string.plates_total, Fmt.num(res.achieved) + " $unit"))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}

/** End-of-workout: rating (1–5), notes, and "save changes back to the routine". */
@Composable
private fun FinishDialog(hasRoutine: Boolean, onDismiss: () -> Unit, onFinish: (Int, String, Boolean) -> Unit) {
    var rating by remember { mutableStateOf(4) }
    var notes by remember { mutableStateOf("") }
    var saveBack by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.finish_title)) },
        text = {
            Column {
                Muted(stringResource(R.string.how_did_it_feel))
                Row { listOf("😫", "😕", "🙂", "😀", "🤩").forEachIndexed { i, emo ->
                    TextButton(onClick = { rating = i + 1 }) { Text(emo, fontSize = if (rating == i + 1) 30.sp else 22.sp) }
                } }
                OutlinedTextField(notes, { notes = it.take(500) }, label = { Text(stringResource(R.string.notes)) }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(saveBack, { saveBack = it })
                    Text(if (hasRoutine) stringResource(R.string.save_changes_routine) else stringResource(R.string.save_as_routine))
                }
            }
        },
        confirmButton = { Button(onClick = { onFinish(rating, notes, saveBack) }) { Text(stringResource(R.string.finish)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
