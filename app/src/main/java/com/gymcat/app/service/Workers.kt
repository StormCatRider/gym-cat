/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — service/Workers.kt                           ║
║ Background jobs run by Android's WorkManager (even if the app is   ║
║ closed):                                                           ║
║  • DailyReminderWorker — once a day at your chosen time:           ║
║      "Time to train" on your workout days, and on the 1st of each  ║
║      month "Measure day" (tape + photos).                          ║
║  • AutoBackupWorker — once a week, backup zip to Downloads/GymCat. ║
║ Scheduler.apply() turns them on/off to match Settings.             ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gymcat.app.GymCatApp
import com.gymcat.app.MainActivity
import com.gymcat.app.R
import com.gymcat.app.data.AppSettings
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class DailyReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as GymCatApp
        val s = app.repo.settings.current()
        val today = LocalDate.now()
        if (s.remindWorkout) {
            val days = s.remindDays.split(',').mapNotNull { it.trim().toIntOrNull() }
            val trainedToday = app.repo.dao.finishedWorkoutsNow().any { it.date == today.toEpochDay() }
            if (today.dayOfWeek.value in days && !trainedToday)
                notify(applicationContext, 1, R.string.remind_workout_title, R.string.remind_workout_text)
        }
        if (s.remindMeasure && today.dayOfMonth == 1)
            notify(applicationContext, 2, R.string.remind_measure_title, R.string.remind_measure_text)
        return Result.success()
    }
}

class AutoBackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as GymCatApp
        if (!app.repo.settings.current().autoBackup) return Result.success()
        val ok = app.backup.autoBackupToDownloads()
        if (ok) app.repo.settings.update { it.copy(lastBackupMs = System.currentTimeMillis()) }
        return if (ok) Result.success() else Result.retry()
    }
}

fun notify(c: Context, id: Int, title: Int, text: Int) {
    if (ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    WorkoutService.ensureChannels(c)
    val open = PendingIntent.getActivity(c, id, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
    val n = NotificationCompat.Builder(c, WorkoutService.CHANNEL_REMINDERS)
        .setSmallIcon(R.drawable.ic_launcher_monochrome)
        .setContentTitle(c.getString(title)).setContentText(c.getString(text))
        .setAutoCancel(true).setContentIntent(open).build()
    NotificationManagerCompat.from(c).notify(100 + id, n)
}

object Scheduler {
    private const val DAILY = "gymcat_daily_reminder"
    private const val BACKUP = "gymcat_weekly_backup"

    fun apply(c: Context, s: AppSettings) {
        val wm = WorkManager.getInstance(c)
        if (s.remindWorkout || s.remindMeasure) {
            // First run at the chosen time (today if still ahead, else tomorrow), then every 24 h.
            val now = LocalDateTime.now()
            var first = now.toLocalDate().atTime(s.remindHour, s.remindMinute)
            if (!first.isAfter(now)) first = first.plusDays(1)
            val delayMin = Duration.between(now, first).toMinutes()
            val req = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMin, TimeUnit.MINUTES).build()
            wm.enqueueUniquePeriodicWork(DAILY, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else wm.cancelUniqueWork(DAILY)

        if (s.autoBackup) {
            val req = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS).build()
            wm.enqueueUniquePeriodicWork(BACKUP, ExistingPeriodicWorkPolicy.KEEP, req)
        } else wm.cancelUniqueWork(BACKUP)
    }
}
