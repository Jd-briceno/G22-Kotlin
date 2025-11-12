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
import kotlinx.coroutines.flow.first
import org.json.JSONArray

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
    private val lastInterestsKey = stringPreferencesKey("last_selected_interests")

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

    suspend fun getLastSelectedInterests(): List<String> {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                prefs[lastInterestsKey]?.let(::decodeInterests) ?: emptyList()
            }
            .first()
    }

    suspend fun saveLastSelectedInterests(interests: List<String>) {
        dataStore.edit { prefs ->
            if (interests.isEmpty()) {
                prefs.remove(lastInterestsKey)
            } else {
                prefs[lastInterestsKey] = encodeInterests(interests)
            }
        }
    }

    private fun encodeInterests(interests: List<String>): String {
        val jsonArray = JSONArray()
        interests.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    private fun decodeInterests(raw: String): List<String> {
        return try {
            val jsonArray = JSONArray(raw)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val value = jsonArray.optString(index)
                    if (!value.isNullOrEmpty()) {
                        add(value)
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
