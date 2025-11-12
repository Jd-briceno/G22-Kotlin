package com.g22.orbitsoundkotlin.data.local

import android.content.Context
import android.content.SharedPreferences

data class RememberMeEntry(
    val remember: Boolean,
    val email: String
)

class RememberMeStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): RememberMeEntry {
        val remember = prefs.getBoolean(KEY_REMEMBER, false)
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        return RememberMeEntry(remember = remember, email = email)
    }

    fun save(remember: Boolean, email: String) {
        prefs.edit()
            .putBoolean(KEY_REMEMBER, remember)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_REMEMBER)
            .remove(KEY_EMAIL)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "auth_remember_me"
        const val KEY_REMEMBER = "remember"
        const val KEY_EMAIL = "email"
    }
}

