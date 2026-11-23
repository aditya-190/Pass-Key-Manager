package com.bhardwaj.passkey.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bhardwaj.passkey.data.local.DataStoreSource
import com.bhardwaj.passkey.utils.Constants.Companion.CURRENT_LANGUAGE
import com.bhardwaj.passkey.utils.Constants.Companion.ONBOARDING_COMPLETE
import com.bhardwaj.passkey.utils.Constants.Companion.PASSKEY_PREFS
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * The app's single preferences file. Declared once at top level: a `preferencesDataStore` delegate on a
 * class instance creates a second DataStore over the same file if the class is ever instantiated twice,
 * which DataStore rejects at runtime.
 */
val Context.passkeyDataStore: DataStore<Preferences> by preferencesDataStore(PASSKEY_PREFS)

class DataStoreRepository @Inject constructor(private val dataSource: DataStore<Preferences>) : DataStoreSource {
    companion object {
        val onBoardingKey = booleanPreferencesKey(name = ONBOARDING_COMPLETE)
        val currentLanguageKey = stringPreferencesKey(name = CURRENT_LANGUAGE)
    }

    override suspend fun <T> readPreference(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        dataSource.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val result = preferences[key] ?: defaultValue
            result
        }

    override suspend fun <T> savePreference(key: Preferences.Key<T>, value: T) {
        dataSource.edit { preferences -> preferences[key] = value }
    }

    override suspend fun <T> removePreference(key: Preferences.Key<T>) {
        dataSource.edit { it.remove(key) }
    }

    override suspend fun clearAllPreference() {
        dataSource.edit { preferences -> preferences.clear() }
    }
}
