/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/AppLanguage.kt                            ║
║ Lets you pick the app's language inside the app:                   ║
║   ""   = follow the phone                                          ║
║   "en" = English,  "es" = Español                                  ║
║ Android 13+ : uses Android's own per-app language (it remembers    ║
║               the choice and reopens the screen by itself).        ║
║ Android 10–12: saves the choice in a tiny file and wraps the app   ║
║               with that language each time it opens.               ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object AppLanguage {
    private const val FILE = "app_language"
    private const val KEY = "tag"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** The chosen language: "", "en" or "es". */
    fun get(ctx: Context): String =
        if (Build.VERSION.SDK_INT >= 33) {
            val list = ctx.getSystemService(LocaleManager::class.java).applicationLocales
            if (list.isEmpty) "" else list[0].language
        } else prefs(ctx).getString(KEY, "") ?: ""

    /** Saves the choice and redraws the screen in the new language. */
    fun set(activity: Activity, tag: String) {
        if (get(activity) == tag) return
        if (Build.VERSION.SDK_INT >= 33) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
            // Android reopens the screen by itself.
        } else {
            prefs(activity).edit().putString(KEY, tag).commit()
            if (tag.isEmpty()) Locale.setDefault(phoneLocale())
            activity.recreate()
        }
    }

    /** Android 10–12 only: gives back the context in the chosen language. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val tag = prefs(base).getString(KEY, "") ?: ""
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)               // so number formats and vm.es follow too
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    /** The phone's own language (to go back to "follow the phone" on Android 10–12). */
    private fun phoneLocale(): Locale = android.content.res.Resources.getSystem().configuration.locales[0]
}
