/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — GymCatApp.kt                                 ║
║ The Application = the first thing Android creates. It builds the   ║
║ app's shared "tools" once (database, settings, repository, Health  ║
║ Connect, backup, sounds) and hands them to every screen. This is   ║
║ "manual dependency injection" — same style as Gym & Ñam.           ║
║ It also keeps the sound switches and the background jobs in sync   ║
║ with Settings.                                                     ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app

import android.app.Application
import com.gymcat.app.data.BackupManager
import com.gymcat.app.data.ExerciseLibrary
import com.gymcat.app.data.HealthConnectManager
import com.gymcat.app.data.Repository
import com.gymcat.app.data.SettingsStore
import com.gymcat.app.data.db.GymDatabase
import com.gymcat.app.service.Feedback
import com.gymcat.app.service.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class GymCatApp : Application() {
    lateinit var repo: Repository; private set
    lateinit var backup: BackupManager; private set
    lateinit var feedback: Feedback; private set
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Android 10–12: open the app in the language picked inside the app (see ui/AppLanguage.kt).
    override fun attachBaseContext(base: android.content.Context) = super.attachBaseContext(com.gymcat.app.ui.AppLanguage.wrap(base))

    override fun onCreate() {
        super.onCreate()
        val db = GymDatabase.create(this)
        val settings = SettingsStore(this)
        repo = Repository(db, ExerciseLibrary(this), settings, HealthConnectManager(this))
        backup = BackupManager(this, db)
        feedback = Feedback(this)

        // Keep custom exercises in the library lookup.
        appScope.launch { repo.dao.customExercises().collect { repo.library.setCustom(it) } }
        // Keep sound switches + background jobs in sync with Settings.
        appScope.launch {
            settings.flow.collect { s ->
                feedback.sounds = s.sounds; feedback.haptics = s.haptics; feedback.meows = s.meows; feedback.voice = s.voice
            }
        }
        appScope.launch {
            settings.flow.map { listOf(it.remindWorkout, it.remindMeasure, it.remindDays, it.remindHour, it.remindMinute, it.autoBackup) }
                .distinctUntilChanged().collect { Scheduler.apply(this@GymCatApp, settings.current()) }
        }
        // Refresh body weight from Health Connect when the app starts (only if turned on).
        appScope.launch { repo.syncWeight() }
    }
}
