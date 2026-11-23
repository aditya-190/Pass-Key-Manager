package com.bhardwaj.passkey.domain.repository

import kotlinx.coroutines.flow.Flow

/** What the backup reminder needs to remember. Times are wall-clock milliseconds; null means "never". */
data class BackupReminderState(
    val lastBackupAt: Long? = null,
    val firstSeenAt: Long? = null,
    val snoozedUntil: Long? = null
)

interface BackupReminderStore {
    val state: Flow<BackupReminderState>

    /** A backup file was written successfully. */
    suspend fun recordBackup(at: Long)

    /** The first time the reminder logic ran on this install; kept if already set. */
    suspend fun markFirstSeen(at: Long)

    suspend fun snooze(until: Long)
}
