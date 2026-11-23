package com.bhardwaj.passkey.data.security

/**
 * When the unlock prompt may appear on its own. Driven from the lock screen's ON_RESUME / ON_STOP, so a screen
 * composed while the app is in the background prompts only once it is really in front.
 *
 * Auto-prompting is limited to the first showing, or after the user really left the app since the last attempt:
 * cancelling the system PIN screen resumes the app on API 28, and prompting again then would never end.
 * The on-screen Unlock button is always available unless a prompt is already up.
 */
class UnlockPromptPolicy {
    private var inFlight = false
    private var attempts = 0
    private var leftAppSinceAttempt = false

    fun onPromptStarted() {
        inFlight = true
        attempts++
        leftAppSinceAttempt = false
    }

    fun onPromptFinished() {
        inFlight = false
    }

    /** A stop while the prompt is up is the system PIN screen covering the app, not the user leaving. */
    fun onStopped() {
        if (!inFlight) leftAppSinceAttempt = true
    }

    fun shouldPromptOnResume(isLocked: Boolean): Boolean =
        isLocked && !inFlight && (attempts == 0 || leftAppSinceAttempt)

    fun canPromptManually(isLocked: Boolean): Boolean = isLocked && !inFlight
}
