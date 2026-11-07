/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/screens/OnboardingScreen.kt               ║
║ First launch, 6 short steps (your choices):                        ║
║  welcome → experience level → goal → days per week + equipment →   ║
║  body (sex, height, weight — or read weight from Gym & Ñam) →      ║
║  suggested program (you can pick another or none).                 ║
║ Easy wording for friends who are new to the gym.                   ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymcat.app.GymCatApp
import com.gymcat.app.R
import com.gymcat.app.core.model.EquipmentAccess
import com.gymcat.app.core.model.Goal
import com.gymcat.app.core.model.Level
import com.gymcat.app.core.programs.Programs
import com.gymcat.app.ui.Fmt
import com.gymcat.app.ui.MainViewModel
import com.gymcat.app.ui.components.Cat
import com.gymcat.app.ui.components.CatArt
import com.gymcat.app.ui.components.GCard
import com.gymcat.app.ui.components.Muted
import com.gymcat.app.ui.components.Stepper
import com.gymcat.app.ui.theme.GymColors

@Composable
fun OnboardingScreen(vm: MainViewModel) {
    val c = GymColors.current
    val app = LocalContext.current.applicationContext as GymCatApp
    val s by vm.settings.collectAsState()
    var step by remember { mutableIntStateOf(0) }
    var level by remember { mutableStateOf(Level.BEGINNER) }
    var goal by remember { mutableStateOf(Goal.GENERAL) }
    var days by remember { mutableIntStateOf(3) }
    var access by remember { mutableStateOf(EquipmentAccess.FULL_GYM) }
    var sexFemale by remember { mutableStateOf(false) }
    var height by remember { mutableStateOf(68.0) }
    var weight by remember { mutableStateOf(170.0) }
    val suggested = Programs.suggest(access, cardioFocus = false)
    var program by remember(suggested.id) { mutableStateOf<String?>(suggested.id) }
    val es = vm.es
    val last = 5

    val hcLauncher = rememberLauncherForActivityResult(app.repo.health.requestContract()) { granted ->
        if (granted.containsAll(app.repo.health.permissions)) { vm.updateSettings { it.copy(healthConnect = true) }; vm.refreshHealth() }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        LinearProgressIndicator(progress = { step / last.toFloat() }, Modifier.fillMaxWidth(), color = c.accent)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            CatArt(if (step == 0) Cat.WELCOME else Cat.THINKING, if (step == 0) "👋" else "🤔", 120.dp)
            when (step) {
                0 -> {
                    Text(stringResource(R.string.welcome_title), fontWeight = FontWeight.Bold, fontSize = 26.sp, color = c.text)
                    Muted(stringResource(R.string.welcome_body))
                }
                1 -> Choice(stringResource(R.string.q_level), stringArrayResource(R.array.levels).toList(), level.ordinal) { level = Level.entries[it] }
                2 -> Choice(stringResource(R.string.q_goal), stringArrayResource(R.array.goals).toList(), goal.ordinal) { goal = Goal.entries[it] }
                3 -> {
                    Text(stringResource(R.string.q_days), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = c.text)
                    Stepper(stringResource(R.string.days), days.toString(), { days = (days - 1).coerceAtLeast(1) }, { days = (days + 1).coerceAtMost(7) })
                    Choice(stringResource(R.string.q_equipment), stringArrayResource(R.array.equipment_access).toList(), access.ordinal) { access = EquipmentAccess.entries[it] }
                }
                4 -> {
                    Text(stringResource(R.string.q_body), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = c.text)
                    Muted(stringResource(R.string.q_body_why))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(!sexFemale, { sexFemale = false }, { Text(stringResource(R.string.male)) })
                        FilterChip(sexFemale, { sexFemale = true }, { Text(stringResource(R.string.female)) })
                    }
                    Stepper(stringResource(R.string.height) + " (in)", Fmt.num(height), { height -= 0.5 }, { height += 0.5 })
                    Stepper(stringResource(R.string.body_weight) + " (lb)", Fmt.num(weight), { weight -= 1 }, { weight += 1 })
                    if (app.repo.health.isAvailable()) OutlinedButton(onClick = { hcLauncher.launch(app.repo.health.permissions) }) {
                        Text(stringResource(R.string.read_from_gymnam))
                    }
                    if (s.healthConnect && s.hcWeightLb > 0) Muted(stringResource(R.string.using_hc_weight, Fmt.weight(s.hcWeightLb, false)))
                }
                else -> {
                    Text(stringResource(R.string.q_program), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = c.text)
                    Programs.all.forEach { p ->
                        GCard(onClick = { program = p.id }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text((if (program == p.id) "✅ " else "") + p.name(es), Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = c.text)
                                if (p.id == suggested.id) Muted(stringResource(R.string.suggested))
                            }
                            Muted(p.desc(es))
                        }
                    }
                    TextButton(onClick = { program = null }) { Text((if (program == null) "✅ " else "") + stringResource(R.string.no_program_free)) }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.back)) }
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                if (step < last) step++ else {
                    vm.updateSettings {
                        it.copy(onboarded = true, level = level.name, goal = goal.name, weeklyGoal = days, equipment = access.name,
                            sex = if (sexFemale) "FEMALE" else "MALE", heightIn = height, bodyLb = weight)
                    }
                    program?.let { vm.activateProgram(it) }
                }
            }) { Text(if (step < last) stringResource(R.string.next) else stringResource(R.string.lets_go)) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Choice(question: String, options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    val c = GymColors.current
    Text(question, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = c.text)
    options.forEachIndexed { i, o ->
        GCard(onClick = { onPick(i) }) { Text((if (i == selected) "✅ " else "") + o, color = c.text) }
    }
}
