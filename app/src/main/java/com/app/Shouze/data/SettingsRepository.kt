package com.app.shouze.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val amoledBlack: Boolean = false,
    val hasSeenOnboarding: Boolean = false
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("media_tracker_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _settings.value = loadSettings()
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean("dynamic_color", enabled).apply()
        _settings.value = loadSettings()
    }

    fun setAmoledBlack(enabled: Boolean) {
        prefs.edit().putBoolean("amoled_black", enabled).apply()
        _settings.value = loadSettings()
    }

    fun setHasSeenOnboarding(seen: Boolean) {
        prefs.edit().putBoolean("has_seen_onboarding", seen).apply()
        _settings.value = loadSettings()
    }

    private fun loadSettings(): AppSettings {
        val themeName = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
            ?: ThemeMode.SYSTEM.name
        return AppSettings(
            themeMode = try {
                ThemeMode.valueOf(themeName)
            } catch (_: Exception) {
                ThemeMode.SYSTEM
            },
            useDynamicColor = prefs.getBoolean("dynamic_color", true),
            amoledBlack = prefs.getBoolean("amoled_black", false),
            hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)
        )
    }
}
