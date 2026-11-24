package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.UserRole
import com.example.ui.theme.TextScaleOption
import com.example.ui.theme.ThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.carePrefsDataStore by preferencesDataStore(name = "care_connect_prefs")

data class LocalAppPreferences(
    val onboardingCompleted: Boolean = false,
    val selectedRole: UserRole = UserRole.ELDER,
    val themePreset: ThemePreset = ThemePreset.CLASSIC_PASTEL,
    val darkMode: Boolean = false,
    val textScale: TextScaleOption = TextScaleOption.NORMAL,
    val remindersEnabled: Boolean = true
)

class UserPrefsDataStore(context: Context) {
    private val dataStore = context.applicationContext.carePrefsDataStore

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SELECTED_ROLE = stringPreferencesKey("selected_role")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val TEXT_SCALE = stringPreferencesKey("text_scale")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
    }

    val preferencesFlow: Flow<LocalAppPreferences> = dataStore.data.map { prefs ->
        LocalAppPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            selectedRole = UserRole.fromWire(prefs[Keys.SELECTED_ROLE]),
            themePreset = ThemePreset.fromId(prefs[Keys.THEME_PRESET]),
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            textScale = TextScaleOption.fromId(prefs[Keys.TEXT_SCALE]),
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: true
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setSelectedRole(role: UserRole) {
        dataStore.edit { it[Keys.SELECTED_ROLE] = role.wire }
    }

    suspend fun setThemePreset(preset: ThemePreset) {
        dataStore.edit { it[Keys.THEME_PRESET] = preset.id }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setTextScale(scale: TextScaleOption) {
        dataStore.edit { it[Keys.TEXT_SCALE] = scale.id }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }
    }
}
