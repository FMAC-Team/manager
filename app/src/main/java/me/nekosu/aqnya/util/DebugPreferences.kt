package me.nekosu.aqnya.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

object DebugPreferences {
    private const val PREF_NAME = "debug"
    private const val PREFIX = "flutter."

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun showRulesFlow(context: Context): Flow<Boolean> = 
        context.prefsFlow { getBoolean("${PREFIX}debug_show_rules", false) }

    fun themeModeFlow(context: Context): Flow<Int> = 
        context.prefsFlow { getInt("${PREFIX}theme_mode", 0) }

    fun navBarStyleFlow(context: Context): Flow<Int> = 
        context.prefsFlow { getInt("${PREFIX}nav_bar_style", 2) }

    fun setShowRules(context: Context, value: Boolean) {
        getPrefs(context).edit().putBoolean("${PREFIX}debug_show_rules", value).apply()
    }

    fun setThemeMode(context: Context, mode: Int) {
        getPrefs(context).edit().putInt("${PREFIX}theme_mode", mode).apply()
    }

    fun setNavBarStyle(context: Context, value: Int) {
        getPrefs(context).edit().putInt("${PREFIX}nav_bar_style", value).apply()
    }

    private fun <T> Context.prefsFlow(getValue: SharedPreferences.() -> T): Flow<T> = callbackFlow {
        val prefs = getPrefs(this@prefsFlow)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            trySend(p.getValue())
        }
        
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getValue())
        
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
}
