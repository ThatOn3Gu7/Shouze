package com.app.shouze.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class UpdateFrequency { EVERY_LAUNCH, WEEKLY, BI_WEEKLY, NEVER }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val amoledBlack: Boolean = false,
    val hasSeenOnboarding: Boolean = false,
    val username: String = "",
    val profilePictureUri: String? = null,
    val updateFrequency: UpdateFrequency = UpdateFrequency.WEEKLY
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

    fun setUsername(name: String) {
        prefs.edit().putString("username", name).apply()
        _settings.value = loadSettings()
    }

    fun setProfilePicture(uri: String?) {
        prefs.edit().putString("profile_picture_uri", uri).apply()
        _settings.value = loadSettings()
    }

    fun setUpdateFrequency(frequency: UpdateFrequency) {
        prefs.edit().putString("update_frequency", frequency.name).apply()
        _settings.value = loadSettings()
    }

    private fun loadSettings(): AppSettings {
        val themeName = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
            ?: ThemeMode.SYSTEM.name
        val frequencyName = prefs.getString("update_frequency", UpdateFrequency.WEEKLY.name)
            ?: UpdateFrequency.WEEKLY.name
        return AppSettings(
            themeMode = try {
                ThemeMode.valueOf(themeName)
            } catch (_: Exception) {
                ThemeMode.SYSTEM
            },
            useDynamicColor = prefs.getBoolean("dynamic_color", true),
            amoledBlack = prefs.getBoolean("amoled_black", false),
            hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false),
            username = prefs.getString("username", "") ?: "",
            profilePictureUri = prefs.getString("profile_picture_uri", null),
            updateFrequency = try {
                UpdateFrequency.valueOf(frequencyName)
            } catch (_: Exception) {
                UpdateFrequency.WEEKLY
            }
        )
    }
}
