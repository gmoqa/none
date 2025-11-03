package org.none.android.storage

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of PreferencesProvider using NSUserDefaults
 */
actual class PreferencesProvider {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return if (userDefaults.objectForKey(key) != null) {
            userDefaults.boolForKey(key)
        } else {
            defaultValue
        }
    }

    actual fun putBoolean(key: String, value: Boolean) {
        userDefaults.setBool(value, key)
        userDefaults.synchronize()
    }

    actual fun getString(key: String, defaultValue: String): String {
        return userDefaults.stringForKey(key) ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        userDefaults.setObject(value, key)
        userDefaults.synchronize()
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return if (userDefaults.objectForKey(key) != null) {
            userDefaults.integerForKey(key).toInt()
        } else {
            defaultValue
        }
    }

    actual fun putInt(key: String, value: Int) {
        userDefaults.setInteger(value.toLong(), key)
        userDefaults.synchronize()
    }

    actual fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
        userDefaults.synchronize()
    }

    actual fun clear() {
        val appDomain = platform.Foundation.NSBundle.mainBundle.bundleIdentifier
        if (appDomain != null) {
            userDefaults.removePersistentDomainForName(appDomain)
            userDefaults.synchronize()
        }
    }
}
