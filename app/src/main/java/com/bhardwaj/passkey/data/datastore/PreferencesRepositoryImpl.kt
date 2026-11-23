package com.bhardwaj.passkey.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bhardwaj.passkey.domain.model.AutoLockTimeout
import com.bhardwaj.passkey.domain.model.ThemeMode
import com.bhardwaj.passkey.domain.repository.PreferencesRepository
import com.bhardwaj.passkey.utils.Constants.Companion.AUTO_LOCK_TIMEOUT
import com.bhardwaj.passkey.utils.Constants.Companion.CURRENT_LANGUAGE
import com.bhardwaj.passkey.utils.Constants.Companion.ONBOARDING_COMPLETE
import com.bhardwaj.passkey.utils.Constants.Companion.PASSKEY_PREFS
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * The app's single preferences file. Declared once at file scope: a `preferencesDataStore` delegate on a
 * class instance creates a second DataStore over the same file if the class is ever instantiated twice,
 * which DataStore rejects at runtime. Consent, update and banner state share this file through DI.
 */
val Context.passkeyDataStore: DataStore<Preferences> by preferencesDataStore(PASSKEY_PREFS)

/** The DataStore is injected (not looked up from a Context) so tests can hand in a temporary file. */
class PreferencesRepositoryImpl @Inject constructor(private val dataStore: DataStore<Preferences>) :
    PreferencesRepository {
    private val onboardingKey = booleanPreferencesKey(ONBOARDING_COMPLETE)
    private val languageKey = stringPreferencesKey(CURRENT_LANGUAGE)
    private val autoLockKey = longPreferencesKey(AUTO_LOCK_TIMEOUT)

    private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    override val onboardingCompleted: Flow<Boolean> = preferences.map { it[onboardingKey] == true }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[onboardingKey] = completed }
    }

    override val selectedLanguageTag: Flow<String?> = preferences.map { it[languageKey] }

    override suspend fun setSelectedLanguageTag(tag: String) {
        dataStore.edit { it[languageKey] = tag }
    }

    override val autoLockTimeout: Flow<AutoLockTimeout> =
        preferences.map { AutoLockTimeout.fromMillis(it[autoLockKey]) }

    override suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        dataStore.edit { it[autoLockKey] = timeout.millis }
    }

    private val themeKey = stringPreferencesKey(THEME_MODE)

    override val themeMode: Flow<ThemeMode> = preferences.map { ThemeMode.fromNameOrSystem(it[themeKey]) }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[themeKey] = mode.name }
    }

    private companion object {
        const val THEME_MODE = "theme_mode"
    }
}
