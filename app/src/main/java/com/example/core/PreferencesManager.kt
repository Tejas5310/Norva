package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.novaraDataStore: DataStore<Preferences> by preferencesDataStore(name = "novara_prefs")

enum class ThemeModePreference {
    SYSTEM,
    DARK,
    LIGHT
}

data class UserLocalPreferences(
    val themeMode: ThemeModePreference = ThemeModePreference.DARK,
    val preferredLanguage: String = "English",
    val feedDiversity: Int = 55
)

class PreferencesManager(context: Context) {
    private val dataStore = context.applicationContext.novaraDataStore

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val FEED_DIVERSITY = intPreferencesKey("feed_diversity")
    }

    val preferencesFlow: Flow<UserLocalPreferences> = dataStore.data.map { prefs ->
        val modeRaw = prefs[Keys.THEME_MODE] ?: ThemeModePreference.DARK.name
        val mode = ThemeModePreference.entries.firstOrNull { it.name == modeRaw }
            ?: ThemeModePreference.DARK
        UserLocalPreferences(
            themeMode = mode,
            preferredLanguage = prefs[Keys.PREFERRED_LANGUAGE] ?: "English",
            feedDiversity = (prefs[Keys.FEED_DIVERSITY] ?: 55).coerceIn(0, 100)
        )
    }

    suspend fun setThemeMode(mode: ThemeModePreference) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setPreferredLanguage(language: String) {
        dataStore.edit { it[Keys.PREFERRED_LANGUAGE] = language }
    }

    suspend fun setFeedDiversity(diversity: Int) {
        dataStore.edit { it[Keys.FEED_DIVERSITY] = diversity.coerceIn(0, 100) }
    }
}
