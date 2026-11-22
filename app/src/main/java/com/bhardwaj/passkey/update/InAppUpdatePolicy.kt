package com.bhardwaj.passkey.update

/** What Google Play reports for this install, mapped from `AppUpdateInfo` so the policy stays pure. */
data class PlayUpdateInfo(
    val updateAvailable: Boolean,
    val immediateAllowed: Boolean,
    val flexibleAllowed: Boolean,
    /** An IMMEDIATE update was started earlier and must be resumed (UpdateAvailability 3). */
    val immediateInProgress: Boolean = false,
    /** A FLEXIBLE update finished downloading and waits for `completeUpdate()`. */
    val flexibleDownloaded: Boolean = false
)

enum class InAppUpdateAction {
    NONE,

    /** Play's full-screen update UI. Hard updates only. The gate screen stays underneath as the fallback. */
    START_IMMEDIATE,

    /** Background download with Play's own consent dialog. Soft updates only. */
    START_FLEXIBLE,

    /** A flexible download is ready: show "Restart to update". */
    COMPLETE_FLEXIBLE,

    /** Hard update without Play support (sideload, emulator without Play, API kill switch): blocking screen. */
    SHOW_GATE,

    /** Soft update without Play support: our dismissible sheet that links to the store. */
    SHOW_SOFT_PROMPT
}

/** Hard → IMMEDIATE, falling back to the gate. Soft → FLEXIBLE, falling back to the soft prompt. */
object InAppUpdatePolicy {
    fun choose(decision: UpdateDecision, play: PlayUpdateInfo?, inAppEnabled: Boolean): InAppUpdateAction {
        val usable = if (inAppEnabled) play else null
        return when (decision) {
            UpdateDecision.Hard -> when {
                usable == null -> InAppUpdateAction.SHOW_GATE
                usable.immediateInProgress -> InAppUpdateAction.START_IMMEDIATE
                usable.updateAvailable && usable.immediateAllowed -> InAppUpdateAction.START_IMMEDIATE
                else -> InAppUpdateAction.SHOW_GATE
            }
            is UpdateDecision.Soft -> when {
                usable?.flexibleDownloaded == true -> InAppUpdateAction.COMPLETE_FLEXIBLE
                usable == null -> InAppUpdateAction.SHOW_SOFT_PROMPT
                usable.updateAvailable && usable.flexibleAllowed -> InAppUpdateAction.START_FLEXIBLE
                else -> InAppUpdateAction.SHOW_SOFT_PROMPT
            }
            UpdateDecision.None ->
                if (usable?.flexibleDownloaded == true) InAppUpdateAction.COMPLETE_FLEXIBLE else InAppUpdateAction.NONE
            UpdateDecision.Maintenance -> InAppUpdateAction.NONE
        }
    }
}

/** Analytics value for an update-flow activity result (RESULT_OK = -1, RESULT_CANCELED = 0, else failed). */
fun inAppUpdateResultName(resultCode: Int): String = when (resultCode) {
    -1 -> "ok"
    0 -> "cancelled"
    else -> "failed"
}
