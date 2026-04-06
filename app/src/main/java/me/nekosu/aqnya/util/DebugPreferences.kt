package me.nekosu.aqnya.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.debugDataStore: DataStore<Preferences> by preferencesDataStore(name = "debug_prefs")

object DebugPreferences {
    val KEY_SHOW_RULES = booleanPreferencesKey("debug_show_rules")
    val KEY_THEME_MODE = intPreferencesKey("theme_mode")

    fun showRulesFlow(context: Context): Flow<Boolean> = context.debugDataStore.data.map { it[KEY_SHOW_RULES] ?: false }

    fun themeModeFlow(context: Context): Flow<Int> = context.debugDataStore.data.map { it[KEY_THEME_MODE] ?: 0 }

    suspend fun setShowRules(
        context: Context,
        value: Boolean,
    ) {
        context.debugDataStore.edit { it[KEY_SHOW_RULES] = value }
    }

    suspend fun setThemeMode(
        context: Context,
        mode: Int,
    ) {
        context.debugDataStore.edit { it[KEY_THEME_MODE] = mode }
    }
}
