package com.bhardwaj.passkey.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.bhardwaj.passkey.domain.repository.BackupReminderState
import com.bhardwaj.passkey.domain.repository.BackupReminderStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Three timestamps in the app's preferences DataStore; nothing about the vault's content. */
class BackupReminderStoreImpl(private val dataStore: DataStore<Preferences>) : BackupReminderStore {
    private val lastBackupKey = longPreferencesKey("backup_last_at")
    private val firstSeenKey = longPreferencesKey("backup_reminder_first_seen_at")
    private val snoozedUntilKey = longPreferencesKey("backup_reminder_snoozed_until")

    override val state: Flow<BackupReminderState> = dataStore.data.map {
        BackupReminderState(
            lastBackupAt = it[lastBackupKey],
            firstSeenAt = it[firstSeenKey],
            snoozedUntil = it[snoozedUntilKey]
        )
    }

    override suspend fun recordBackup(at: Long) {
        dataStore.edit { it[lastBackupKey] = at }
    }

    override suspend fun markFirstSeen(at: Long) {
        dataStore.edit { if (it[firstSeenKey] == null) it[firstSeenKey] = at }
    }

    override suspend fun snooze(until: Long) {
        dataStore.edit { it[snoozedUntilKey] = until }
    }
}
