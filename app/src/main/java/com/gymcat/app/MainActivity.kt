/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — MainActivity.kt                              ║
║ The one screen-window of the app. It:                              ║
║  • applies the theme (palette + dark mode);                        ║
║  • shows the lock screen when the app lock is on (re-locks after   ║
║    60 s in the background, same as Gym & Ñam);                     ║
║  • holds the navigation map: which route shows which screen;       ║
║  • draws the bottom bar: Home · Routines · (+) · Progress · Body;  ║
║  • opens shared routine files from friends.                        ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymcat.app.ui.MainViewModel
import com.gymcat.app.ui.screens.AddSheet
import com.gymcat.app.ui.screens.BodyScreen
import com.gymcat.app.ui.screens.CardioScreen
import com.gymcat.app.ui.screens.CustomExerciseScreen
import com.gymcat.app.ui.screens.ExerciseDetailScreen
import com.gymcat.app.ui.screens.FinishScreen
import com.gymcat.app.ui.screens.HistoryScreen
import com.gymcat.app.ui.screens.HomeScreen
import com.gymcat.app.ui.screens.LibraryScreen
import com.gymcat.app.ui.screens.LockScreen
import com.gymcat.app.ui.screens.OnboardingScreen
import com.gymcat.app.ui.screens.PrivacyScreen
import com.gymcat.app.ui.screens.ProgressScreen
import com.gymcat.app.ui.screens.QrScanScreen
import com.gymcat.app.ui.screens.QrShowScreen
import com.gymcat.app.ui.screens.RoutineEditorScreen
import com.gymcat.app.ui.screens.RoutinesScreen
import com.gymcat.app.ui.screens.SettingsScreen
import com.gymcat.app.ui.screens.StretchScreen
import com.gymcat.app.ui.screens.WorkoutDetailScreen
import com.gymcat.app.ui.screens.WorkoutScreen
import com.gymcat.app.ui.theme.GymCatTheme
import com.gymcat.app.ui.theme.GymColors

class MainActivity : FragmentActivity() {
    private val vm: MainViewModel by viewModels { MainViewModel.Factory(application) }

    // Lock state lives in the ViewModel so turning the phone sideways doesn't re-lock the app.
    private var pendingShared by mutableStateOf<String?>(null)
    private var startNext by mutableStateOf(false)              // widget "Start" button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val ready by vm.settingsReady.collectAsState()
            val s = ready ?: com.gymcat.app.data.AppSettings()
            // Lock off → mark unlocked, so turning the lock ON in Settings doesn't lock you out right away.
            LaunchedEffect(ready?.lockEnabled, ready?.pinHash) {
                if (ready != null && !(s.lockEnabled && s.pinHash.isNotBlank())) vm.locked.value = false
            }
            GymCatTheme(s.palette, s.themeMode) {
                when {
                    ready == null -> Box(Modifier.fillMaxSize())
                    intent?.action == "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" ||
                        intent?.action == "android.intent.action.VIEW_PERMISSION_USAGE" -> PrivacyScreen { finish() }
                    !s.onboarded -> OnboardingScreen(vm)
                    else -> Box(Modifier.fillMaxSize()) {
                        // The app stays underneath (so your workout screen is still there after
                        // unlocking); the lock screen covers it completely while locked.
                        AppRoot(vm, pendingShared, startNext, { startNext = false }) { pendingShared = null }
                        val locked by vm.locked.collectAsState()
                        if (s.lockEnabled && s.pinHash.isNotBlank() && locked) LockScreen(this@MainActivity, vm) { vm.locked.value = false }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent); handleIntent(intent)
    }

    /** A friend sent a .gymcat file → read its text and offer to import. */
    private fun handleIntent(i: Intent?) {
        if (i?.action == com.gymcat.app.widget.ACTION_START_NEXT) { startNext = true; return }
        val uri: Uri? = when (i?.action) {
            Intent.ACTION_VIEW -> i.data
            Intent.ACTION_SEND -> if (android.os.Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else @Suppress("DEPRECATION") i.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }
        val text = i?.getStringExtra(Intent.EXTRA_TEXT)
        pendingShared = when {
            uri != null -> runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes().take(200_000).toByteArray().toString(Charsets.UTF_8) } }.getOrNull()
            text?.startsWith("GYMCAT1:") == true -> text
            else -> pendingShared
        }
    }

    // Re-lock after 60 s in the background (rotation doesn't count: isChangingConfigurations).
    override fun onStop() { super.onStop(); if (!isChangingConfigurations) vm.backgroundAt = System.currentTimeMillis() }
    override fun onStart() {
        super.onStart()
        if (vm.backgroundAt > 0 && System.currentTimeMillis() - vm.backgroundAt > 60_000) vm.locked.value = true
        vm.backgroundAt = 0
    }
}

/** Main app frame: bottom bar + all screens. */
@Composable
fun AppRoot(vm: MainViewModel, shared: String?, startNext: Boolean, onStartHandled: () -> Unit, onSharedHandled: () -> Unit) {
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    val toast by vm.toast.collectAsState()
    var showAdd by rememberSaveable { mutableStateOf(false) }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    val tabs = listOf("home", "routines", "progress", "body")
    val c = GymColors.current
    val importedFmt = stringResource(R.string.imported_routine)
    val badCode = stringResource(R.string.import_failed)

    LaunchedEffect(toast) { toast?.let { snack.showSnackbar(it); vm.toast.value = null } }
    // Widget "Start": next program day, else first routine, else just open the workout screen.
    LaunchedEffect(startNext) {
        if (!startNext) return@LaunchedEffect
        onStartHandled()
        when {
            vm.active.value != null -> nav.navigate("workout")
            vm.program.value != null -> vm.startProgramDay { nav.navigate("workout") }
            vm.routines.value.isNotEmpty() -> vm.startRoutine(vm.routines.value.first().id) { nav.navigate("workout") }
        }
    }
    LaunchedEffect(shared) {
        if (shared != null) {
            vm.importCode(shared) { name -> vm.toast.value = if (name != null) importedFmt.format(name) else badCode }
            onSharedHandled()
        }
    }

    Scaffold(
        containerColor = c.bg,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (route in tabs) BottomBar(nav, route) { showAdd = true }
        },
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { HomeScreen(vm, nav) }
            composable("routines") { RoutinesScreen(vm, nav) }
            composable("progress") { ProgressScreen(vm, nav) }
            composable("body") { BodyScreen(vm) }
            composable("settings") { SettingsScreen(vm, nav) }
            composable("library") { LibraryScreen(vm, nav) }
            composable("exercise/{id}") { ExerciseDetailScreen(vm, nav, it.arguments?.getString("id") ?: "") }
            composable("custom/{id}") { CustomExerciseScreen(vm, nav, it.arguments?.getString("id") ?: "new") }
            composable("routine") { RoutineEditorScreen(vm, nav) }
            composable("workout") { WorkoutScreen(vm, nav) }
            composable("finish") { FinishScreen(vm, nav) }
            composable("cardio/{mode}") { CardioScreen(vm, nav, it.arguments?.getString("mode") ?: "manual") }
            composable("stretch") { StretchScreen(vm, nav) }
            composable("history") { HistoryScreen(vm, nav) }
            composable("workoutDetail/{id}") { WorkoutDetailScreen(vm, nav, it.arguments?.getString("id")?.toLongOrNull() ?: 0) }
            composable("qr/{rid}") { QrShowScreen(vm, nav, it.arguments?.getString("rid")?.toLongOrNull() ?: 0) }
            composable("scan") { QrScanScreen(vm, nav) }
        }
    }
    if (showAdd) AddSheet(vm, nav) { showAdd = false }
}

@Composable
private fun BottomBar(nav: NavHostController, route: String, onPlus: () -> Unit) {
    val c = GymColors.current
    fun go(r: String) = nav.navigate(r) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }
    val colors = NavigationBarItemDefaults.colors(indicatorColor = c.accent.copy(alpha = 0.2f))
    NavigationBar(containerColor = c.card) {
        NavigationBarItem(route == "home", { go("home") }, { Icon(Icons.Filled.Home, null) }, label = { Text(stringResource(R.string.tab_home)) }, colors = colors)
        NavigationBarItem(route == "routines", { go("routines") }, { Icon(Icons.Filled.FitnessCenter, null) }, label = { Text(stringResource(R.string.tab_routines)) }, colors = colors)
        Box(Modifier.weight(1f).padding(top = 6.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
            FloatingActionButton(onClick = onPlus, shape = CircleShape, containerColor = c.accent, modifier = Modifier.size(58.dp)) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.start_workout), tint = androidx.compose.ui.graphics.Color.White)
            }
        }
        NavigationBarItem(route == "progress", { go("progress") }, { Icon(Icons.Filled.BarChart, null) }, label = { Text(stringResource(R.string.tab_progress)) }, colors = colors)
        NavigationBarItem(route == "body", { go("body") }, { Icon(Icons.Filled.Accessibility, null) }, label = { Text(stringResource(R.string.tab_body)) }, colors = colors)
    }
}
