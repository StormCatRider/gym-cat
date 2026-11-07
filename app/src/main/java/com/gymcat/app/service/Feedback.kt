/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — service/Feedback.kt                          ║
║ Sounds, vibration, voice and meows — each has a switch in Settings.║
║  • beep()   : short tone (3-2-1 countdown, end of rest/interval)   ║
║  • buzz()   : vibration (set done, timer end, record)              ║
║  • say()    : spoken cue with the phone's own text-to-speech       ║
║               (works offline, English or Spanish like the phone)   ║
║  • meow()   : plays one of YOUR meow files if you added any:       ║
║               app/src/main/res/raw/meow_1.ogg … meow_9.ogg          ║
║               (none added yet → stays silent, no error)            ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale

class Feedback(private val context: Context) {
    // Switches mirrored from Settings (updated by the Application class).
    @Volatile var sounds = true
    @Volatile var haptics = true
    @Volatile var meows = true
    @Volatile var voice = false

    private val tone by lazy { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull() }
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    fun beep(long: Boolean = false) {
        if (!sounds) return
        tone?.startTone(if (long) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP, if (long) 400 else 150)
    }

    fun buzz(strong: Boolean = false) {
        if (!haptics) return
        // Android 12+ uses VibratorManager; Android 10–11 use the older Vibrator service.
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        val effect = if (strong) VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120), -1)
        else VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
        runCatching { vibrator?.vibrate(effect) }
    }

    fun say(text: String) {
        if (!voice) return
        val t = tts
        if (t == null) {
            tts = TextToSpeech(context) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) { tts?.language = Locale.getDefault(); tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "gc") }
            }
        } else if (ttsReady) t.speak(text, TextToSpeech.QUEUE_ADD, null, "gc")
    }

    /** Finds meow_1 … meow_9 in res/raw by name (so the app builds even with no files). */
    private val meowIds: List<Int> by lazy { findMeows() }

    // (The "suppress" note can't sit on a "by lazy" value, so the search lives in its own function.)
    @SuppressLint("DiscouragedApi")
    private fun findMeows(): List<Int> =
        (1..9).map { context.resources.getIdentifier("meow_$it", "raw", context.packageName) }.filter { it != 0 }

    /** chance = probability (0..1) so "random after a set" doesn't meow every time. */
    fun meow(chance: Double = 1.0) {
        if (!meows || !sounds || meowIds.isEmpty() || Math.random() > chance) return
        runCatching {
            MediaPlayer.create(context, meowIds.random())?.apply { setOnCompletionListener { it.release() }; start() }
        }
    }

    fun shutdown() { tts?.shutdown(); tts = null }
}
