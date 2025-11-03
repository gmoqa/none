package org.none.android.storage

/**
 * Platform-agnostic preferences/settings storage
 * Android: SharedPreferences
 * iOS: UserDefaults
 */
expect class PreferencesProvider {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)

    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)

    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)

    fun remove(key: String)
    fun clear()
}
