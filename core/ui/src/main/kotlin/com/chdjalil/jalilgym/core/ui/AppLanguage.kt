package com.chdjalil.jalilgym.core.ui

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The app's own language, independent of the phone's. Android 13+ stores it
 * as the per-app language (so it also shows in system settings and covers
 * notifications and widgets); older versions keep it here and apply it to each
 * context through [wrap].
 */
object AppLanguage {
    /** "" follows the phone's language. */
    val supported = listOf("", "en", "fr", "ar")

    private const val PREFS = "jalilgym_language"
    private const val KEY = "tag"

    fun current(context: Context): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        runCatching {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales?.takeIf { !it.isEmpty }?.get(0)?.language
        }.getOrNull().orEmpty()
    } else {
        stored(context)
    }

    /** Switches the app to [tag] and redraws the screen. */
    fun set(context: Context, tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The system restarts the activity in the new language.
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).commit()
            context.findActivity()?.recreate()
        }
    }

    /** Call from attachBaseContext of the Application and each Activity (needed before Android 13 only). */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = stored(base).ifEmpty { return base }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(config)
    }

    private fun stored(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
