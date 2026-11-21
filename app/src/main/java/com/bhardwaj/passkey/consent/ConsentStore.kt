package com.bhardwaj.passkey.consent

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.bhardwaj.passkey.analytics.ConsentState
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

interface ConsentStore {
    val consent: Flow<ConsentState>

    /** Records an explicit choice; afterwards [ConsentState.decided] is true. */
    suspend fun save(analytics: Boolean, crash: Boolean)
}

class DataStoreConsentStore(private val dataStore: DataStore<Preferences>) : ConsentStore {
    override val consent: Flow<ConsentState> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            ConsentState(
                decided = prefs[DECIDED] ?: false,
                analytics = prefs[ANALYTICS] ?: false,
                crash = prefs[CRASH] ?: false
            )
        }

    override suspend fun save(analytics: Boolean, crash: Boolean) {
        dataStore.edit { prefs ->
            prefs[ANALYTICS] = analytics
            prefs[CRASH] = crash
            prefs[DECIDED] = true
        }
    }

    private companion object {
        val DECIDED = booleanPreferencesKey("consent_decided")
        val ANALYTICS = booleanPreferencesKey("consent_analytics")
        val CRASH = booleanPreferencesKey("consent_crash")
    }
}
