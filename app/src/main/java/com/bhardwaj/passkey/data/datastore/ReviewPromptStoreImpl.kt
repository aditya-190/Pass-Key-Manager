package com.bhardwaj.passkey.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.bhardwaj.passkey.review.ReviewPromptState
import com.bhardwaj.passkey.review.ReviewPromptStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Two timestamps and a counter in the preferences DataStore; nothing about the vault. */
class ReviewPromptStoreImpl(private val dataStore: DataStore<Preferences>) : ReviewPromptStore {
    private val firstSeenKey = longPreferencesKey("review_first_seen_at")
    private val lastAskedKey = longPreferencesKey("review_last_asked_at")
    private val fillsKey = longPreferencesKey("review_autofill_fills")

    override val state: Flow<ReviewPromptState> = dataStore.data.map {
        ReviewPromptState(
            firstSeenAt = it[firstSeenKey],
            lastAskedAt = it[lastAskedKey],
            autofillFills =
            it[fillsKey] ?: 0
        )
    }

    override suspend fun markFirstSeen(at: Long) {
        dataStore.edit { if (it[firstSeenKey] == null) it[firstSeenKey] = at }
    }

    /** Never moves back: after the clock is turned back, an earlier time must not shorten the cooldown. */
    override suspend fun recordAsked(at: Long) {
        dataStore.edit { it[lastAskedKey] = maxOf(it[lastAskedKey] ?: at, at) }
    }

    override suspend fun recordFill() {
        // Capped: only "enough" matters, and the counter must never overflow.
        dataStore.edit { it[fillsKey] = ((it[fillsKey] ?: 0) + 1).coerceAtMost(FILL_CAP) }
    }

    private companion object {
        const val FILL_CAP = 1_000L
    }
}
