/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — data/SettingsStore.kt                        ║
║ Saves your settings (palette, units, profile, switches) in a small ║
║ private file using DataStore. AppSettings = all settings with      ║
║ their DEFAULT values. Privacy defaults match Gym & Ñam:            ║
║ Health Connect OFF, lock OFF, reminders OFF, auto-backup OFF.      ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "gymcat_settings")

data class AppSettings(
    val onboarded: Boolean = false,
    // Look
    val palette: String = "blue",          // blue | purple | green | orange
    val themeMode: String = "system",      // system | light | dark
    // Units (storage is always lb/mi/in; these only change the display)
    val metric: Boolean = false,
    val plateKg: Boolean = false,
    // Profile
    val bodyLb: Double = 170.0,
    val sex: String = "MALE",
    val heightIn: Double = 68.0,
    val level: String = "BEGINNER",
    val goal: String = "GENERAL",
    val equipment: String = "FULL_GYM",
    val weeklyGoal: Int = 3,               // training days per week for the streak
    // Workout
    val restCompound: Int = 150,           // 2:30
    val restIsolation: Int = 90,           // 1:30
    val restCalisthenics: Int = 90,        // 1:30
    val keepScreenOn: Boolean = true,
    val workoutNotification: Boolean = true,
    val sounds: Boolean = true,
    val haptics: Boolean = true,
    val meows: Boolean = true,             // plays only if you added meow files
    val voice: Boolean = false,
    // Health Connect
    val healthConnect: Boolean = false,
    val hcWeightLb: Double = 0.0,          // last body weight read from Health Connect
    val hcWeightDay: Long = 0,
    // Lock
    val lockEnabled: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = "",
    val biometric: Boolean = false,
    // Reminders
    val remindWorkout: Boolean = false,
    val remindDays: String = "1,3,5",      // 1 = Monday … 7 = Sunday
    val remindHour: Int = 18,
    val remindMinute: Int = 0,
    val remindMeasure: Boolean = false,
    // Backup
    val autoBackup: Boolean = false,
    val lastBackupMs: Long = 0,
    val deloadSnoozeDay: Long = 0,         // "Not now" on the deload card hides it until this epochDay
) {
    /** Body weight to use in formulas: Health Connect value when on and known, else yours. */
    val effectiveBodyLb: Double get() = if (healthConnect && hcWeightLb > 0) hcWeightLb else bodyLb
}

class SettingsStore(private val context: Context) {
    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val palette = stringPreferencesKey("palette")
        val themeMode = stringPreferencesKey("themeMode")
        val metric = booleanPreferencesKey("metric")
        val plateKg = booleanPreferencesKey("plateKg")
        val bodyLb = doublePreferencesKey("bodyLb")
        val sex = stringPreferencesKey("sex")
        val heightIn = doublePreferencesKey("heightIn")
        val level = stringPreferencesKey("level")
        val goal = stringPreferencesKey("goal")
        val equipment = stringPreferencesKey("equipment")
        val weeklyGoal = intPreferencesKey("weeklyGoal")
        val restCompound = intPreferencesKey("restCompound")
        val restIsolation = intPreferencesKey("restIsolation")
        val restCalisthenics = intPreferencesKey("restCalisthenics")
        val keepScreenOn = booleanPreferencesKey("keepScreenOn")
        val workoutNotification = booleanPreferencesKey("workoutNotification")
        val sounds = booleanPreferencesKey("sounds")
        val haptics = booleanPreferencesKey("haptics")
        val meows = booleanPreferencesKey("meows")
        val voice = booleanPreferencesKey("voice")
        val healthConnect = booleanPreferencesKey("healthConnect")
        val hcWeightLb = doublePreferencesKey("hcWeightLb")
        val hcWeightDay = longPreferencesKey("hcWeightDay")
        val lockEnabled = booleanPreferencesKey("lockEnabled")
        val pinHash = stringPreferencesKey("pinHash")
        val pinSalt = stringPreferencesKey("pinSalt")
        val biometric = booleanPreferencesKey("biometric")
        val remindWorkout = booleanPreferencesKey("remindWorkout")
        val remindDays = stringPreferencesKey("remindDays")
        val remindHour = intPreferencesKey("remindHour")
        val remindMinute = intPreferencesKey("remindMinute")
        val remindMeasure = booleanPreferencesKey("remindMeasure")
        val autoBackup = booleanPreferencesKey("autoBackup")
        val lastBackupMs = longPreferencesKey("lastBackupMs")
        val deloadSnoozeDay = longPreferencesKey("deloadSnoozeDay")
    }

    private fun read(p: Preferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            onboarded = p[K.onboarded] ?: d.onboarded,
            palette = p[K.palette] ?: d.palette,
            themeMode = p[K.themeMode] ?: d.themeMode,
            metric = p[K.metric] ?: d.metric,
            plateKg = p[K.plateKg] ?: d.plateKg,
            bodyLb = p[K.bodyLb] ?: d.bodyLb,
            sex = p[K.sex] ?: d.sex,
            heightIn = p[K.heightIn] ?: d.heightIn,
            level = p[K.level] ?: d.level,
            goal = p[K.goal] ?: d.goal,
            equipment = p[K.equipment] ?: d.equipment,
            weeklyGoal = p[K.weeklyGoal] ?: d.weeklyGoal,
            restCompound = p[K.restCompound] ?: d.restCompound,
            restIsolation = p[K.restIsolation] ?: d.restIsolation,
            restCalisthenics = p[K.restCalisthenics] ?: d.restCalisthenics,
            keepScreenOn = p[K.keepScreenOn] ?: d.keepScreenOn,
            workoutNotification = p[K.workoutNotification] ?: d.workoutNotification,
            sounds = p[K.sounds] ?: d.sounds,
            haptics = p[K.haptics] ?: d.haptics,
            meows = p[K.meows] ?: d.meows,
            voice = p[K.voice] ?: d.voice,
            healthConnect = p[K.healthConnect] ?: d.healthConnect,
            hcWeightLb = p[K.hcWeightLb] ?: d.hcWeightLb,
            hcWeightDay = p[K.hcWeightDay] ?: d.hcWeightDay,
            lockEnabled = p[K.lockEnabled] ?: d.lockEnabled,
            pinHash = p[K.pinHash] ?: d.pinHash,
            pinSalt = p[K.pinSalt] ?: d.pinSalt,
            biometric = p[K.biometric] ?: d.biometric,
            remindWorkout = p[K.remindWorkout] ?: d.remindWorkout,
            remindDays = p[K.remindDays] ?: d.remindDays,
            remindHour = p[K.remindHour] ?: d.remindHour,
            remindMinute = p[K.remindMinute] ?: d.remindMinute,
            remindMeasure = p[K.remindMeasure] ?: d.remindMeasure,
            autoBackup = p[K.autoBackup] ?: d.autoBackup,
            lastBackupMs = p[K.lastBackupMs] ?: d.lastBackupMs,
            deloadSnoozeDay = p[K.deloadSnoozeDay] ?: d.deloadSnoozeDay,
        )
    }

    private fun write(p: MutablePreferences, s: AppSettings) {
        p[K.onboarded] = s.onboarded; p[K.palette] = s.palette; p[K.themeMode] = s.themeMode
        p[K.metric] = s.metric; p[K.plateKg] = s.plateKg; p[K.bodyLb] = s.bodyLb; p[K.sex] = s.sex
        p[K.heightIn] = s.heightIn; p[K.level] = s.level; p[K.goal] = s.goal; p[K.equipment] = s.equipment
        p[K.weeklyGoal] = s.weeklyGoal; p[K.restCompound] = s.restCompound; p[K.restIsolation] = s.restIsolation
        p[K.restCalisthenics] = s.restCalisthenics; p[K.keepScreenOn] = s.keepScreenOn
        p[K.workoutNotification] = s.workoutNotification; p[K.sounds] = s.sounds; p[K.haptics] = s.haptics
        p[K.meows] = s.meows; p[K.voice] = s.voice; p[K.healthConnect] = s.healthConnect
        p[K.hcWeightLb] = s.hcWeightLb; p[K.hcWeightDay] = s.hcWeightDay; p[K.lockEnabled] = s.lockEnabled
        p[K.pinHash] = s.pinHash; p[K.pinSalt] = s.pinSalt; p[K.biometric] = s.biometric
        p[K.remindWorkout] = s.remindWorkout; p[K.remindDays] = s.remindDays; p[K.remindHour] = s.remindHour
        p[K.remindMinute] = s.remindMinute; p[K.remindMeasure] = s.remindMeasure
        p[K.autoBackup] = s.autoBackup; p[K.lastBackupMs] = s.lastBackupMs; p[K.deloadSnoozeDay] = s.deloadSnoozeDay
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { read(it) }

    suspend fun current(): AppSettings = flow.first()

    /** Change settings: update { it.copy(palette = "green") } */
    suspend fun update(change: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p -> write(p, change(read(p))) }
    }
}
