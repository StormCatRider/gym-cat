/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/screens/CardioScreen.kt                   ║
║ Four ways to log cardio (all four were chosen):                    ║
║  • manual    — type minutes + distance;                            ║
║  • timer     — start/stop stopwatch, distance at the end;          ║
║  • intervals — work/rest rounds with 3-2-1 beeps and voice cues;   ║
║  • gps       — outdoor run: live distance, pace, mile splits and   ║
║                the route LINE (offline, no map, no internet).      ║
║ Calories: kcal = MET × kg × hours (MET of the chosen exercise).    ║
║ Pace = time ÷ distance (min per mile).                             ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.gymcat.app.GymCatApp
import com.gymcat.app.R
import com.gymcat.app.core.calc.GeoMath
import com.gymcat.app.service.WorkoutClock
import com.gymcat.app.service.WorkoutService
import com.gymcat.app.ui.Fmt
import com.gymcat.app.ui.MainViewModel
import com.gymcat.app.ui.components.CatArt
import com.gymcat.app.ui.components.GCard
import com.gymcat.app.ui.components.Muted
import com.gymcat.app.ui.components.RouteView
import com.gymcat.app.ui.components.StatBox
import com.gymcat.app.ui.components.Stepper
import com.gymcat.app.ui.theme.GymColors
import kotlinx.coroutines.delay

@Composable
fun CardioScreen(vm: MainViewModel, nav: NavHostController, mode: String) {
    val c = GymColors.current
    val ctx = LocalContext.current
    val exId by vm.cardioExercise.collectAsState()
    LaunchedEffect(mode) { if (mode == "gps" && vm.cardioExercise.value == "treadmill_run") vm.cardioExercise.value = "outdoor_run" }
    // Keep the screen on while timing.
    DisposableEffect(Unit) {
        val w = (ctx as? Activity)?.window
        w?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { w?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    val e = vm.ex(exId)
    val titleRes = when (mode) { "gps" -> R.string.cardio_gps; "timer" -> R.string.cardio_timer; "intervals" -> R.string.cardio_intervals; else -> R.string.cardio_manual }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) { Text("‹ " + stringResource(R.string.back)) }
            Text(stringResource(titleRes), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = c.text)
        }
        GCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CatArt(e?.catArt, e?.emoji ?: "🏃", 48.dp)
                Text(vm.exName(exId), Modifier.weight(1f).padding(start = 8.dp), fontWeight = FontWeight.SemiBold, color = c.text)
                TextButton(onClick = { vm.pickMode.value = "cardio"; nav.navigate("library") }) { Text(stringResource(R.string.change)) }
            }
        }
        when (mode) {
            "gps" -> GpsPanel(vm, nav, exId)
            "timer" -> TimerPanel(vm, nav, exId)
            "intervals" -> IntervalPanel(vm, nav, exId)
            else -> ManualPanel(vm, nav, exId)
        }
    }
}

@Composable
private fun ManualPanel(vm: MainViewModel, nav: NavHostController, exId: String) {
    val s by vm.settings.collectAsState()
    var minutes by remember { mutableStateOf("30") }
    var dist by remember { mutableStateOf("") }
    OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(4) }, label = { Text(stringResource(R.string.minutes)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(dist, { dist = Fmt.cleanDecimal(it, maxLen = 6) },
        label = { Text(stringResource(R.string.distance) + " (" + Fmt.distUnit(s.metric) + ")") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
    val m = minutes.toIntOrNull() ?: 0
    val mi = Fmt.toMi(dist.toDoubleOrNull() ?: 0.0, s.metric)
    GeoMath.paceMinPerMile(m * 60L, mi)?.let { Muted(stringResource(R.string.pace) + ": " + Fmt.pace(it * 60, s.metric)) }
    Button(onClick = {
        val end = System.currentTimeMillis()
        vm.saveCardio(exId, end - m * 60_000L, end, m * 60, mi, "") { nav.popBackStack() }
    }, enabled = m > 0, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
}

@Composable
private fun TimerPanel(vm: MainViewModel, nav: NavHostController, exId: String) {
    val c = GymColors.current
    val s by vm.settings.collectAsState()
    var startMs by remember { mutableLongStateOf(0L) }
    var accumulated by remember { mutableLongStateOf(0L) }      // ms before the last pause
    var running by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var stopped by remember { mutableStateOf(false) }
    var dist by remember { mutableStateOf("") }
    var firstStart by remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) { while (running) { now = System.currentTimeMillis(); delay(250) } }
    val elapsed = accumulated + if (running) now - startMs else 0

    Text(Fmt.duration(elapsed / 1000), fontSize = 56.sp, fontWeight = FontWeight.Bold, color = c.text, modifier = Modifier.fillMaxWidth())
    if (!stopped) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            if (running) { accumulated += System.currentTimeMillis() - startMs; running = false }
            else { startMs = System.currentTimeMillis(); if (firstStart == 0L) firstStart = startMs; running = true }
        }) { Text(if (running) stringResource(R.string.pause) else stringResource(R.string.start)) }
        OutlinedButton(onClick = {
            if (running) { accumulated += System.currentTimeMillis() - startMs; running = false }
            stopped = true
        }, enabled = elapsed > 0) { Text(stringResource(R.string.stop)) }
    } else {
        OutlinedTextField(dist, { dist = Fmt.cleanDecimal(it, maxLen = 6) },
            label = { Text(stringResource(R.string.distance) + " (" + Fmt.distUnit(s.metric) + ")") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            vm.saveCardio(exId, firstStart, System.currentTimeMillis(), (accumulated / 1000).toInt(),
                Fmt.toMi(dist.toDoubleOrNull() ?: 0.0, s.metric), "") { nav.popBackStack() }
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
    }
}

/**
 * Interval timer: rounds × (work + rest). Beeps at 3-2-1, long beep + vibration at each change,
 * spoken "Work" / "Rest" if voice cues are on.
 * Example: 8 rounds × (20 s work + 10 s rest) = 8 × 30 s = 4:00 total.
 */
@Composable
private fun IntervalPanel(vm: MainViewModel, nav: NavHostController, exId: String) {
    val c = GymColors.current
    val ctx = LocalContext.current
    val fb = (ctx.applicationContext as GymCatApp).feedback
    var work by remember { mutableIntStateOf(30) }
    var rest by remember { mutableIntStateOf(30) }
    var rounds by remember { mutableIntStateOf(8) }
    var running by remember { mutableStateOf(false) }
    var round by remember { mutableIntStateOf(1) }
    var isWork by remember { mutableStateOf(true) }
    var left by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var startMs by remember { mutableLongStateOf(0L) }
    val workWord = stringResource(R.string.work); val restWord = stringResource(R.string.rest); val doneWord = stringResource(R.string.done)

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        startMs = System.currentTimeMillis()
        for (r in 1..rounds) {
            round = r
            for (phaseWork in listOf(true, false)) {
                if (!phaseWork && r == rounds) continue          // no rest after the last round
                isWork = phaseWork
                fb.beep(long = true); fb.buzz(strong = true); fb.say(if (phaseWork) workWord else restWord)
                left = if (phaseWork) work else rest
                while (left > 0) {
                    delay(1000); left--
                    if (left in 1..3) fb.beep()
                }
            }
        }
        fb.beep(long = true); fb.buzz(strong = true); fb.say(doneWord)
        running = false; done = true
    }

    if (!running && !done) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stepper(stringResource(R.string.work), Fmt.duration(work.toLong()), { work = (work - 5).coerceAtLeast(5) }, { work += 5 })
            Stepper(stringResource(R.string.rest), Fmt.duration(rest.toLong()), { rest = (rest - 5).coerceAtLeast(0) }, { rest += 5 })
        }
        Stepper(stringResource(R.string.rounds), rounds.toString(), { rounds = (rounds - 1).coerceAtLeast(1) }, { rounds++ })
        Muted(stringResource(R.string.total) + ": " + Fmt.duration((rounds * work + (rounds - 1) * rest).toLong()))
        Button(onClick = { running = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start)) }
    } else if (running) {
        GCard {
            Text(stringResource(R.string.round_of, round, rounds), color = c.text2)
            Text(if (isWork) workWord else restWord, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = if (isWork) c.accent else c.good)
            Text(Fmt.duration(left.toLong()), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = c.text)
        }
        OutlinedButton(onClick = { running = false; done = true }) { Text(stringResource(R.string.stop)) }
    } else {
        Text(stringResource(R.string.intervals_done), color = c.text)
        Button(onClick = {
            val end = System.currentTimeMillis()
            vm.saveCardio(exId, startMs, end, ((end - startMs) / 1000).toInt(), 0.0, "") { nav.popBackStack() }
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
    }
}

@Composable
private fun GpsPanel(vm: MainViewModel, nav: NavHostController, exId: String) {
    val c = GymColors.current
    val ctx = LocalContext.current
    val s by vm.settings.collectAsState()
    val gps by WorkoutClock.gps.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        granted = r[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(gps.tracking) { while (gps.tracking) { now = System.currentTimeMillis(); delay(1000) } }
    val title = vm.exName(exId)
    val elapsedSec = if (gps.startMs > 0) ((if (gps.tracking) now else (gps.points.lastOrNull()?.timeMs ?: now)) - gps.startMs) / 1000 else 0

    if (!granted) {
        GCard {
            Text(stringResource(R.string.gps_permission_title), fontWeight = FontWeight.SemiBold, color = c.text)
            Muted(stringResource(R.string.gps_permission_body))
            Button(onClick = { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) {
                Text(stringResource(R.string.allow))
            }
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatBox(stringResource(R.string.time), Fmt.duration(elapsedSec), Modifier.weight(1f))
        StatBox(stringResource(R.string.distance), Fmt.dist(gps.miles, s.metric), Modifier.weight(1f))
        StatBox(stringResource(R.string.pace), GeoMath.paceMinPerMile(elapsedSec, gps.miles)?.let { Fmt.pace(it * 60, s.metric) } ?: "—", Modifier.weight(1f))
    }
    RouteView(GeoMath.project(gps.points))
    val splits = GeoMath.splitsSec(gps.points)
    if (splits.isNotEmpty()) GCard {
        Text(stringResource(R.string.splits), fontWeight = FontWeight.SemiBold, color = c.text)
        splits.forEachIndexed { i, sec -> Text("${i + 1} ${Fmt.distUnit(false)}: ${Fmt.duration(sec)}", color = c.text) }
    }
    when {
        // Not started yet (startMs = 0). Once started, Stop always leads to Save / Discard,
        // even if the GPS never got a fix, so the background service is always closed.
        !gps.tracking && gps.startMs == 0L -> Button(onClick = {
            if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                notif.launch(Manifest.permission.POST_NOTIFICATIONS)
            WorkoutService.gpsStart(ctx, title)
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_run)) }
        gps.tracking -> OutlinedButton(onClick = { WorkoutService.gpsStop(ctx) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.stop)) }
        else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val pts = gps.points
                vm.saveCardio(exId, gps.startMs, pts.lastOrNull()?.timeMs ?: System.currentTimeMillis(), elapsedSec.toInt(), gps.miles,
                    GeoMath.encode(pts)) {
                    WorkoutClock.gps.value = com.gymcat.app.service.GpsState(); WorkoutService.stop(ctx); nav.popBackStack()
                }
            }) { Text(stringResource(R.string.save)) }
            TextButton(onClick = { WorkoutClock.gps.value = com.gymcat.app.service.GpsState(); WorkoutService.stop(ctx) }) {
                Text(stringResource(R.string.discard), color = c.bad)
            }
        }
    }
    Spacer(Modifier.height(24.dp))
}
