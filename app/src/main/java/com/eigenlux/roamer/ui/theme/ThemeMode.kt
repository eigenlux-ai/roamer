package com.eigenlux.roamer.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/** Theme mode: follow system / force light / force dark. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Resolve to whether dark (for SYSTEM, reads the system setting). @Composable: reads the system dark state. */
@Composable
fun ThemeMode.resolveDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** Persist theme preference (SharedPreferences; apply is fine in a foreground process). */
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
