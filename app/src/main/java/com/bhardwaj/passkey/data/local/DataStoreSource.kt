package com.bhardwaj.passkey.data.local

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

interface DataStoreSource {
    suspend fun <T> readPreference(key: Preferences.Key<T>, defaultValue: T): Flow<T>
    suspend fun <T> savePreference(key: Preferences.Key<T>, value: T)
    suspend fun <T> removePreference(key: Preferences.Key<T>)
    suspend fun clearAllPreference()
}