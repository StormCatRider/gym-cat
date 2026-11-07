/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/screens/StretchScreen.kt                  ║
║ Guided stretching (your choice): each stretch is held for X        ║
║ seconds, then the app moves to the next one by itself with a beep, ║
║ a vibration and (if on) a spoken "Next: …".                        ║
║ Starts with a full-body list; add/remove stretches or change the   ║
║ seconds before you start.                                          ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gymcat.app.GymCatApp
import com.gymcat.app.R
import com.gymcat.app.ui.Fmt
import com.gymcat.app.ui.MainViewModel
import com.gymcat.app.ui.components.CatArt
import com.gymcat.app.ui.components.GCard
import com.gymcat.app.ui.components.Muted
import com.gymcat.app.ui.components.Stepper
import com.gymcat.app.ui.theme.GymColors
import kotlinx.coroutines.delay

private val DEFAULT_STRETCHES = listOf(
    "st_hamstring_fold", "st_quad_standing", "st_hip_flexor", "st_figure4", "st_calf_wall",
    "st_chest_doorway", "st_cross_body_shoulder", "st_child_pose", "st_cat_cow",
)

@Composable
fun StretchScreen(vm: MainViewModel, nav: NavHostController) {
    val c = GymColors.current
    val ctx = LocalContext.current
    val fb = (ctx.applicationContext as GymCatApp).feedback
    val list by vm.stretchList.collectAsState()
    var running by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(0) }
    var left by remember { mutableIntStateOf(0) }
    var startMs by remember { mutableLongStateOf(0L) }
    var finished by remember { mutableStateOf(false) }
    val es = vm.es
    val nextWord = stringResource(R.string.next_up)
    val title = stringResource(R.string.guided_stretch)

    LaunchedEffect(Unit) { if (vm.stretchList.value.isEmpty()) vm.stretchList.value = DEFAULT_STRETCHES.map { it to 30 } }
    DisposableEffect(Unit) {
        val w = (ctx as? Activity)?.window
        w?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { w?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        startMs = System.currentTimeMillis()
        for (i in list.indices) {
            index = i
            fb.beep(long = true); fb.buzz(); fb.say("$nextWord ${vm.exName(list[i].first)}")
            left = list[i].second
            delay(1500)                                   // a moment to get into position
            while (left > 0) { delay(1000); left--; if (left in 1..3) fb.beep() }
        }
        fb.beep(long = true); fb.buzz(strong = true)
        running = false; finished = true
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) { Text("‹ " + stringResource(R.string.back)) }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = c.text)
        }
        when {
            running -> {
                val (id, total) = list[index]
                val e = vm.ex(id)
                GCard {
                    Muted(stringResource(R.string.round_of, index + 1, list.size))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CatArt(e?.catArt, e?.emoji ?: "🧘", 96.dp)
                        Text(vm.exName(id), Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = c.text)
                    }
                    Text(Fmt.duration(left.toLong()), fontSize = 60.sp, fontWeight = FontWeight.Bold, color = c.text)
                    LinearProgressIndicator(progress = { 1f - left / total.toFloat().coerceAtLeast(1f) }, Modifier.fillMaxWidth(), color = c.accent)
                    Spacer(Modifier.height(8.dp))
                    Text(e?.howTo(es) ?: "", color = c.text)
                }
                OutlinedButton(onClick = { running = false; finished = true }) { Text(stringResource(R.string.stop)) }
            }
            finished -> {
                CatArt(com.gymcat.app.ui.components.Cat.FINISHED, "🐱", 120.dp)
                Text(stringResource(R.string.stretch_done), color = c.text)
                Button(onClick = {
                    val done = list.take(index + 1)
                    vm.saveStretch(done, startMs, title) { vm.stretchList.value = emptyList(); nav.popBackStack() }
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
            }
            else -> {
                Muted(stringResource(R.string.total) + ": " + Fmt.duration(list.sumOf { it.second }.toLong()))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(list) { i, (id, sec) ->
                        GCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(vm.exName(id), Modifier.weight(1f), color = c.text)
                                Stepper(stringResource(R.string.seconds), sec.toString(),
                                    { vm.stretchList.value = list.toMutableList().also { it[i] = id to (sec - 5).coerceAtLeast(10) } },
                                    { vm.stretchList.value = list.toMutableList().also { it[i] = id to sec + 5 } },
                                    onSet = { v -> vm.stretchList.value = list.toMutableList().also { it[i] = id to v.toInt().coerceIn(10, 600) } })
                                IconButton(onClick = { vm.stretchList.value = list.toMutableList().also { it.removeAt(i) } }) {
                                    Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                                }
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { vm.pickMode.value = "stretch"; nav.navigate("library") }, modifier = Modifier.fillMaxWidth()) {
                    Text("＋ " + stringResource(R.string.add_stretch))
                }
                Button(onClick = { running = true }, enabled = list.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start)) }
            }
        }
    }
}
