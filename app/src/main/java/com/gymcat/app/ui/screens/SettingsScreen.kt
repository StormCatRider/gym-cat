/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/screens/SettingsScreen.kt                 ║
║ All switches in one place:                                         ║
║  Look (palette, dark mode) · Units (lb↔kg, mi↔km display; kg       ║
║  plates) · Profile (sex, height, weight, weekly goal) · Workout    ║
║  (rest times, screen on, sounds, vibration, meows, voice) ·        ║
║  Health Connect · Lock (PIN/fingerprint) · Reminders · Backup.     ║
║ Permissions are asked ONLY when you turn on the matching switch.   ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.gymcat.app.BuildConfig
import com.gymcat.app.GymCatApp
import com.gymcat.app.R
import com.gymcat.app.ui.Fmt
import com.gymcat.app.ui.MainViewModel
import com.gymcat.app.ui.components.GCard
import com.gymcat.app.ui.components.Muted
import com.gymcat.app.ui.components.SectionTitle
import com.gymcat.app.ui.components.Stepper
import com.gymcat.app.ui.theme.GymColors
import com.gymcat.app.ui.theme.Palettes
import kotlinx.coroutines.launch

@Composable
private fun SwitchRow(title: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = GymColors.current.text)
            subtitle?.let { Muted(it, size = 12) }
        }
        Switch(checked, onChange)
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel, nav: NavHostController) {
    val c = GymColors.current
    val ctx = LocalContext.current
    val app = ctx.applicationContext as GymCatApp
    val s by vm.settings.collectAsState()
    var pinDialog by remember { mutableStateOf(false) }
    var restoreConfirm by remember { mutableStateOf<android.net.Uri?>(null) }
    val hcUnavailable = stringResource(R.string.hc_unavailable)
    val backupDone = stringResource(R.string.backup_saved)
    val restoreDone = stringResource(R.string.restore_done)
    val restoreBad = stringResource(R.string.restore_failed)
    val backupFailed = stringResource(R.string.backup_failed)

    // Health Connect permission screen (only our 3 permissions).
    val hcLauncher = rememberLauncherForActivityResult(app.repo.health.requestContract()) { granted ->
        val ok = granted.containsAll(app.repo.health.permissions)
        vm.updateSettings { it.copy(healthConnect = ok) }
        if (ok) vm.refreshHealth()
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun askNotifications() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        // Runs in the app's own background scope: it keeps going if you leave Settings,
        // and file work never freezes the screen.
        if (uri != null) app.appScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val ok = runCatching { ctx.contentResolver.openOutputStream(uri)?.use { app.backup.export(it) } != null }.getOrDefault(false)
            if (ok) vm.updateSettings { it.copy(lastBackupMs = System.currentTimeMillis()) }
            vm.toast.value = if (ok) backupDone else backupFailed
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoreConfirm = uri }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) { Text("‹ " + stringResource(R.string.back)) }
            Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.text)
        }

        SectionTitle(stringResource(R.string.look))
        GCard {
            Text(stringResource(R.string.palette), color = c.text)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                Palettes.forEach { p ->
                    Box(Modifier.size(40.dp).clip(CircleShape).background(p.accentLight)
                        .border(if (s.palette == p.id) 3.dp else 0.dp, c.text, CircleShape)
                        .clickable { vm.updateSettings { it.copy(palette = p.id) } })
                }
            }
            Text(stringResource(R.string.dark_mode), color = c.text)
            val modes = listOf("system", "light", "dark")
            val modeNames = stringArrayResource(R.array.theme_modes)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                modes.forEachIndexed { i, m -> FilterChip(s.themeMode == m, { vm.updateSettings { it.copy(themeMode = m) } }, { Text(modeNames[i]) }) }
            }
        }

        // Language: follow the phone, or pick English / Español just for this app.
        SectionTitle(stringResource(R.string.language))
        GCard {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            var lang by remember { mutableStateOf(com.gymcat.app.ui.AppLanguage.get(ctx)) }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("" to stringResource(R.string.lang_phone), "en" to "English", "es" to "Español").forEach { (tag, label) ->
                    FilterChip(lang == tag, {
                        lang = tag
                        (ctx as? android.app.Activity ?: (ctx as? android.content.ContextWrapper)?.baseContext as? android.app.Activity)
                            ?.let { com.gymcat.app.ui.AppLanguage.set(it, tag) }
                    }, { Text(label) })
                }
            }
        }

        SectionTitle(stringResource(R.string.units))
        GCard {
            SwitchRow(stringResource(R.string.metric_units), s.metric, stringResource(R.string.metric_units_sub)) { v -> vm.updateSettings { it.copy(metric = v) } }
            SwitchRow(stringResource(R.string.kg_plates), s.plateKg, stringResource(R.string.kg_plates_sub)) { v -> vm.updateSettings { it.copy(plateKg = v) } }
        }

        SectionTitle(stringResource(R.string.profile))
        GCard {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(s.sex == "MALE", { vm.updateSettings { it.copy(sex = "MALE") } }, { Text(stringResource(R.string.male)) })
                FilterChip(s.sex == "FEMALE", { vm.updateSettings { it.copy(sex = "FEMALE") } }, { Text(stringResource(R.string.female)) })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stepper(stringResource(R.string.height) + " (" + Fmt.lengthUnit(s.metric) + ")", Fmt.lengthNum(s.heightIn, s.metric),
                    { vm.updateSettings { it.copy(heightIn = (it.heightIn - 0.5).coerceAtLeast(40.0)) } },
                    { vm.updateSettings { it.copy(heightIn = (it.heightIn + 0.5).coerceAtMost(96.0)) } },
                    onSet = { v -> vm.updateSettings { it.copy(heightIn = Fmt.toIn(v, it.metric).coerceIn(40.0, 96.0)) } })
                Stepper(stringResource(R.string.body_weight) + " (" + Fmt.weightUnit(s.metric) + ")", Fmt.weightNum(s.bodyLb, s.metric),
                    { vm.updateSettings { it.copy(bodyLb = (it.bodyLb - 1).coerceAtLeast(60.0)) } },
                    { vm.updateSettings { it.copy(bodyLb = it.bodyLb + 1) } },
                    onSet = { v -> vm.updateSettings { it.copy(bodyLb = Fmt.toLb(v, it.metric).coerceIn(60.0, 700.0)) } })
            }
            if (s.healthConnect && s.hcWeightLb > 0) Muted(stringResource(R.string.using_hc_weight, Fmt.weight(s.hcWeightLb, s.metric)), size = 12)
            Stepper(stringResource(R.string.weekly_goal_days), s.weeklyGoal.toString(),
                { vm.updateSettings { it.copy(weeklyGoal = (it.weeklyGoal - 1).coerceAtLeast(1)) } },
                { vm.updateSettings { it.copy(weeklyGoal = (it.weeklyGoal + 1).coerceAtMost(7)) } },
                onSet = { v -> vm.updateSettings { it.copy(weeklyGoal = v.toInt().coerceIn(1, 7)) } })
        }

        SectionTitle(stringResource(R.string.workout_settings))
        GCard {
            Text(stringResource(R.string.default_rest), color = c.text)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stepper(stringResource(R.string.compound), Fmt.duration(s.restCompound.toLong()),
                    { vm.updateSettings { it.copy(restCompound = (it.restCompound - 15).coerceAtLeast(15)) } }, { vm.updateSettings { it.copy(restCompound = it.restCompound + 15) } },
                    isTime = true, onSet = { v -> vm.updateSettings { it.copy(restCompound = v.toInt().coerceIn(15, 1800)) } })
                Stepper(stringResource(R.string.isolation), Fmt.duration(s.restIsolation.toLong()),
                    { vm.updateSettings { it.copy(restIsolation = (it.restIsolation - 15).coerceAtLeast(15)) } }, { vm.updateSettings { it.copy(restIsolation = it.restIsolation + 15) } },
                    isTime = true, onSet = { v -> vm.updateSettings { it.copy(restIsolation = v.toInt().coerceIn(15, 1800)) } })
            }
            Stepper(stringResource(R.string.calisthenics), Fmt.duration(s.restCalisthenics.toLong()),
                { vm.updateSettings { it.copy(restCalisthenics = (it.restCalisthenics - 15).coerceAtLeast(15)) } }, { vm.updateSettings { it.copy(restCalisthenics = it.restCalisthenics + 15) } },
                    isTime = true, onSet = { v -> vm.updateSettings { it.copy(restCalisthenics = v.toInt().coerceIn(15, 1800)) } })
            SwitchRow(stringResource(R.string.keep_screen_on), s.keepScreenOn) { v -> vm.updateSettings { it.copy(keepScreenOn = v) } }
            SwitchRow(stringResource(R.string.workout_notification), s.workoutNotification) { v ->
                if (v) askNotifications(); vm.updateSettings { it.copy(workoutNotification = v) }
            }
            SwitchRow(stringResource(R.string.sounds), s.sounds) { v -> vm.updateSettings { it.copy(sounds = v) } }
            SwitchRow(stringResource(R.string.vibration), s.haptics) { v -> vm.updateSettings { it.copy(haptics = v) } }
            SwitchRow(stringResource(R.string.meows), s.meows, stringResource(R.string.meows_sub)) { v -> vm.updateSettings { it.copy(meows = v) } }
            SwitchRow(stringResource(R.string.voice_cues), s.voice, stringResource(R.string.voice_cues_sub)) { v -> vm.updateSettings { it.copy(voice = v) } }
        }

        SectionTitle(stringResource(R.string.health_connect))
        GCard {
            SwitchRow(stringResource(R.string.hc_switch), s.healthConnect, stringResource(R.string.hc_sub)) { v ->
                if (!v) vm.updateSettings { it.copy(healthConnect = false) }
                else if (!app.repo.health.isAvailable()) vm.toast.value = hcUnavailable
                else hcLauncher.launch(app.repo.health.permissions)
            }
            Muted(stringResource(R.string.hc_double_count), size = 12)
        }

        SectionTitle(stringResource(R.string.privacy_lock))
        GCard {
            SwitchRow(stringResource(R.string.app_lock), s.lockEnabled && s.pinHash.isNotBlank(), stringResource(R.string.app_lock_sub)) { v ->
                if (v) pinDialog = true else vm.updateSettings { it.copy(lockEnabled = false, pinHash = "", pinSalt = "", biometric = false) }
            }
            if (s.lockEnabled) SwitchRow(stringResource(R.string.fingerprint), s.biometric) { v -> vm.updateSettings { it.copy(biometric = v) } }
        }

        SectionTitle(stringResource(R.string.reminders))
        GCard {
            SwitchRow(stringResource(R.string.remind_workout), s.remindWorkout) { v -> if (v) askNotifications(); vm.updateSettings { it.copy(remindWorkout = v) } }
            if (s.remindWorkout) {
                val names = stringArrayResource(R.array.week_days_short)
                val days = s.remindDays.split(',').mapNotNull { it.toIntOrNull() }.toSet()
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (1..7).forEach { d ->
                        FilterChip(d in days, {
                            val nd = if (d in days) days - d else days + d
                            vm.updateSettings { it.copy(remindDays = nd.sorted().joinToString(",")) }
                        }, { Text(names[d - 1], fontSize = 11.sp) })
                    }
                }
                Stepper(stringResource(R.string.time), "%d:%02d".format(s.remindHour, s.remindMinute),
                    { vm.updateSettings { val t = it.remindHour * 60 + it.remindMinute - 30; it.copy(remindHour = (t / 60).mod(24), remindMinute = t.mod(60)) } },
                    { vm.updateSettings { val t = it.remindHour * 60 + it.remindMinute + 30; it.copy(remindHour = (t / 60).mod(24), remindMinute = t.mod(60)) } },
                    // Typed as hour:minute (24 h). parseSeconds reads "18:30" as 18×60+30, so here that number = minutes of the day.
                    isTime = true, onSet = { v ->
                        val typed = v.toInt(); val t = if (typed < 24) typed * 60 else typed      // just "7" = 7:00
                        if (t < 24 * 60) vm.updateSettings { it.copy(remindHour = t / 60, remindMinute = t % 60) }
                    })
            }
            SwitchRow(stringResource(R.string.remind_measure), s.remindMeasure, stringResource(R.string.remind_measure_sub)) { v ->
                if (v) askNotifications(); vm.updateSettings { it.copy(remindMeasure = v) }
            }
        }

        SectionTitle(stringResource(R.string.backup))
        GCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { exportLauncher.launch(app.backup.fileName()) }) { Text(stringResource(R.string.backup_now)) }
                OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }) { Text(stringResource(R.string.restore)) }
            }
            SwitchRow(stringResource(R.string.auto_backup), s.autoBackup, stringResource(R.string.auto_backup_sub)) { v -> vm.updateSettings { it.copy(autoBackup = v) } }
            if (s.lastBackupMs > 0) Muted(stringResource(R.string.last_backup, java.time.Instant.ofEpochMilli(s.lastBackupMs)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()), size = 12)
        }

        SectionTitle(stringResource(R.string.about))
        GCard {
            Text("Gym Cat " + BuildConfig.VERSION_NAME, color = c.text)
            Muted(stringResource(R.string.about_privacy))
            Muted(stringResource(R.string.about_notices), size = 12)
        }
        Spacer(Modifier.height(32.dp))
    }

    if (pinDialog) PinSetupDialog(onDismiss = { pinDialog = false }) { pin ->
        val salt = newSalt()
        vm.updateSettings { it.copy(lockEnabled = true, pinSalt = salt, pinHash = pinHash(pin, salt)) }
        pinDialog = false
    }
    restoreConfirm?.let { uri ->
        AlertDialog(onDismissRequest = { restoreConfirm = null },
            title = { Text(stringResource(R.string.restore)) },
            text = { Text(stringResource(R.string.restore_warning)) },
            confirmButton = { TextButton(onClick = {
                restoreConfirm = null
                app.appScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val ok = runCatching { ctx.contentResolver.openInputStream(uri)?.use { app.backup.restore(it) } ?: false }.getOrDefault(false)
                    vm.toast.value = if (ok) restoreDone else restoreBad
                }
            }) { Text(stringResource(R.string.restore), color = c.bad) } },
            dismissButton = { TextButton(onClick = { restoreConfirm = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun PinSetupDialog(onDismiss: () -> Unit, onSet: (String) -> Unit) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    val ok = a.length in 4..8 && a == b
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_pin)) },
        text = {
            Column {
                OutlinedTextField(a, { a = it.filter(Char::isDigit).take(8) }, label = { Text(stringResource(R.string.pin_4_8)) },
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                OutlinedTextField(b, { b = it.filter(Char::isDigit).take(8) }, label = { Text(stringResource(R.string.pin_repeat)) },
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
            }
        },
        confirmButton = { TextButton(onClick = { onSet(a) }, enabled = ok) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
