/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — service/WorkoutService.kt                    ║
║ A "foreground service" = work that keeps running while the screen  ║
║ is off, shown by a notification you can see. Gym Cat uses it for:  ║
║  1. The workout notification with the running time.               ║
║  2. The rest timer: 3-2-1 beeps + vibration when it ends,          ║
║     even if the phone is in your pocket.                           ║
║  3. GPS tracking for outdoor runs (offline; just the GPS chip).    ║
║ WorkoutClock = the shared state the screens read (rest end time,   ║
║ GPS points, distance).                                             ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.gymcat.app.GymCatApp
import com.gymcat.app.MainActivity
import com.gymcat.app.R
import com.gymcat.app.core.calc.GeoMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Live GPS run state. */
data class GpsState(
    val tracking: Boolean = false,
    val startMs: Long = 0,
    val points: List<GeoMath.Point> = emptyList(),
    val miles: Double = 0.0,
)

/** Shared timer/GPS state (screens read it, the service drives it). */
object WorkoutClock {
    val workoutStartMs = MutableStateFlow(0L)
    val restEndMs = MutableStateFlow(0L)      // 0 = no rest running
    val restTotalSec = MutableStateFlow(0)
    val gps = MutableStateFlow(GpsState())

    fun startRest(seconds: Int) {
        if (seconds <= 0) return
        restTotalSec.value = seconds
        restEndMs.value = System.currentTimeMillis() + seconds * 1000L
    }
    fun addRest(seconds: Int) { if (restEndMs.value > 0) restEndMs.value += seconds * 1000L }
    fun skipRest() { restEndMs.value = 0 }
    fun restLeftSec(): Int = if (restEndMs.value == 0L) 0 else ((restEndMs.value - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
}

class WorkoutService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var title = ""
    private var gpsOn = false
    private val locationManager by lazy { getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    private val listener = LocationListener { loc: Location -> onLocation(loc) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.app_name)
                WorkoutClock.workoutStartMs.value = intent.getLongExtra(EXTRA_START, System.currentTimeMillis())
                goForeground(); loop()
            }
            ACTION_GPS_START -> {
                title = intent.getStringExtra(EXTRA_TITLE) ?: title
                goForeground()        // must be called within 5 s of starting, even if GPS is refused
                loop(); startGps()
            }
            ACTION_GPS_STOP -> stopGps()
            ACTION_STOP -> { stopGps(); WorkoutClock.skipRest(); stopSelf() }
        }
        return START_NOT_STICKY
    }

    private fun goForeground() {
        val type = if (gpsOn) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        else ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(), type)
    }

    private fun buildNotification(): Notification {
        ensureChannels(this)
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val rest = WorkoutClock.restEndMs.value
        val b = NotificationCompat.Builder(this, CHANNEL_WORKOUT)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setContentIntent(open)
            .setUsesChronometer(true)
        if (rest > System.currentTimeMillis()) {
            // Rest countdown shown by Android itself (no updates every second needed).
            b.setContentText(getString(R.string.notif_resting)).setWhen(rest).setChronometerCountDown(true)
        } else {
            val gps = WorkoutClock.gps.value
            b.setContentText(if (gps.tracking) getString(R.string.notif_gps, "%.2f".format(gps.miles)) else getString(R.string.notif_training))
                .setWhen(WorkoutClock.workoutStartMs.value.takeIf { it > 0 } ?: System.currentTimeMillis())
        }
        return b.build()
    }

    private fun refresh() {
        getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, buildNotification())
    }

    /** Checks the rest timer 4×/second: beeps at 3-2-1 and buzzes at 0. */
    private var looping = false
    private fun loop() {
        if (looping) return
        looping = true
        scope.launch {
            var lastEnd = 0L; var lastBeep = -1
            val fb = (application as GymCatApp).feedback
            while (isActive) {
                val end = WorkoutClock.restEndMs.value
                if (end != lastEnd) { lastEnd = end; lastBeep = -1; refresh() }
                if (end > 0) {
                    val left = ((end - System.currentTimeMillis() + 999) / 1000).toInt()
                    if (left in 1..3 && left != lastBeep) { fb.beep(); lastBeep = left }
                    if (left <= 0) {
                        fb.beep(long = true); fb.buzz(strong = true); fb.say(getString(R.string.voice_rest_over))
                        WorkoutClock.restEndMs.value = 0
                    }
                }
                delay(250)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startGps() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        gpsOn = true
        WorkoutClock.gps.value = GpsState(tracking = true, startMs = System.currentTimeMillis())
        goForeground()
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 3f, listener, Looper.getMainLooper())
    }

    private fun stopGps() {
        if (!gpsOn) return
        gpsOn = false
        runCatching { locationManager.removeUpdates(listener) }
        WorkoutClock.gps.update { it.copy(tracking = false) }
        refresh()
    }

    private var lastMile = 0
    private fun onLocation(loc: Location) {
        val p = GeoMath.Point(loc.latitude, loc.longitude, loc.time, loc.accuracy)
        WorkoutClock.gps.update { s ->
            val cleaned = GeoMath.clean(s.points + p)
            s.copy(points = cleaned, miles = GeoMath.totalMiles(cleaned))
        }
        val miles = WorkoutClock.gps.value.miles
        if (miles.toInt() > lastMile) {          // spoken split every full mile
            lastMile = miles.toInt()
            val fb = (application as GymCatApp).feedback
            fb.buzz(); fb.say(getString(R.string.voice_mile_split, lastMile))
        }
        refresh()
    }

    override fun onDestroy() {
        stopGps(); scope.cancel(); super.onDestroy()
    }

    companion object {
        const val ACTION_START = "start"; const val ACTION_STOP = "stop"
        const val ACTION_GPS_START = "gps_start"; const val ACTION_GPS_STOP = "gps_stop"
        const val EXTRA_TITLE = "title"; const val EXTRA_START = "startMs"
        const val NOTIF_ID = 42
        const val CHANNEL_WORKOUT = "workout"; const val CHANNEL_REMINDERS = "reminders"

        fun ensureChannels(c: Context) {
            val nm = c.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(NotificationChannel(CHANNEL_WORKOUT, c.getString(R.string.channel_workout), NotificationManager.IMPORTANCE_LOW))
            nm.createNotificationChannel(NotificationChannel(CHANNEL_REMINDERS, c.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT))
        }

        fun start(c: Context, title: String, startMs: Long) = ContextCompat.startForegroundService(c,
            Intent(c, WorkoutService::class.java).setAction(ACTION_START).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_START, startMs))
        fun stop(c: Context) = c.startService(Intent(c, WorkoutService::class.java).setAction(ACTION_STOP))
        fun gpsStart(c: Context, title: String) = ContextCompat.startForegroundService(c,
            Intent(c, WorkoutService::class.java).setAction(ACTION_GPS_START).putExtra(EXTRA_TITLE, title))
        fun gpsStop(c: Context) = c.startService(Intent(c, WorkoutService::class.java).setAction(ACTION_GPS_STOP))
    }
}
