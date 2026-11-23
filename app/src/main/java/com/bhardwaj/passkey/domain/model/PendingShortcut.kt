package com.bhardwaj.passkey.domain.model

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * Hands an icon shortcut from MainActivity to Home, which runs it once the vault is open. Memory only: a shortcut
 * is a one-off request, not something to restore. Observable, because the app may already be on Home when the
 * shortcut arrives (MainActivity is singleTask).
 */
@Singleton
class PendingShortcut @Inject constructor() {
    private val pending = MutableStateFlow<ShortcutAction?>(null)

    val requests: StateFlow<ShortcutAction?> = pending.asStateFlow()

    /** The latest request wins: tapping Add, then Search before unlocking, means Search. */
    fun offer(action: ShortcutAction) {
        pending.value = action
    }

    /** Returns the request once. */
    fun take(): ShortcutAction? = pending.getAndUpdate { null }
}
