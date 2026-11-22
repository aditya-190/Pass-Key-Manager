package com.bhardwaj.passkey.banner

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bhardwaj.passkey.config.AppConfig
import com.bhardwaj.passkey.config.ConfigKeys
import com.bhardwaj.passkey.core.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The announcement to show now, from Remote Config `announcement_json` minus the ones the user dismissed
 * (persisted in DataStore, so a dismissal survives restarts). Re-evaluated whenever config or dismissals
 * change, and on every new collection (screen entry), which is when time windows are checked.
 */
class AnnouncementRepository(
    private val config: Flow<AppConfig>,
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
    private val versionCode: Long
) {
    val dismissedIds: Flow<Set<String>> =
        dataStore.data.map { prefs -> prefs[DISMISSED].orEmpty().split(SEPARATOR).filter { it.isNotEmpty() }.toSet() }

    val current: Flow<Announcement?> =
        combine(
            config.map { it[ConfigKeys.ANNOUNCEMENT_JSON] }.distinctUntilChanged().map(AnnouncementParser::parse),
            dismissedIds
        ) { announcements, dismissed ->
            AnnouncementEligibility.pick(announcements, clock.nowMs(), versionCode, dismissed)
        }.distinctUntilChanged()

    suspend fun dismiss(id: String) {
        dataStore.edit { prefs ->
            val existing = prefs[DISMISSED].orEmpty().split(SEPARATOR).filter { it.isNotEmpty() && it != id }
            // Newest last; keep a bounded history so the preference can't grow forever.
            prefs[DISMISSED] = (existing + id).takeLast(MAX_REMEMBERED).joinToString(SEPARATOR)
        }
    }

    private companion object {
        val DISMISSED = stringPreferencesKey("announcement_dismissed_ids")
        const val SEPARATOR = "\n"
        const val MAX_REMEMBERED = 50
    }
}
