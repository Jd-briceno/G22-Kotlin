package com.g22.orbitsoundkotlin.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

val Context.userPreferencesStore: DataStore<Preferences>
    get() = dataStore

data class RememberSettings(
    val rememberMe: Boolean = false,
    val email: String = ""
)

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    private val rememberKey = booleanPreferencesKey("remember_me")
    private val emailKey = stringPreferencesKey("remember_email")

    val rememberSettings: Flow<RememberSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            RememberSettings(
                rememberMe = prefs[rememberKey] ?: false,
                email = prefs[emailKey] ?: ""
            )
        }

    suspend fun updateRememberMe(remember: Boolean, email: String) {
        dataStore.edit { prefs ->
            if (remember) {
                prefs[rememberKey] = true
                prefs[emailKey] = email
            } else {
                prefs.remove(rememberKey)
                prefs.remove(emailKey)
            }
        }
    }
}
