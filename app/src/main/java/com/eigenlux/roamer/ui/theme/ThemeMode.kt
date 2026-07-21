package com.eigenlux.roamer.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/** Theme mode preference option. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Resolves active dark mode status for the selected [ThemeMode]. */
@Composable
fun ThemeMode.resolveDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** Storage helper for persisting the selected theme mode preference. */
object ThemeModeStore {
    private const val PREFS = "roamer_prefs"
    private const val KEY = "theme_mode"

    fun load(ctx: Context): ThemeMode =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM

    fun save(ctx: Context, mode: ThemeMode) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, mode.name).apply()
    }
}
