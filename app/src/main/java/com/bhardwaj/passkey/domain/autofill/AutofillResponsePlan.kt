package com.bhardwaj.passkey.domain.autofill

/**
 * What an autofill response contains. Android's FillResponse is a stub on the JVM, so the decision lives here.
 *
 * Found on a real phone: neither response ever offered to save, so Android never asked "Save to PassKey?" for a
 * login PassKey did not know yet. Both now carry a save offer whenever the form has a username and a password field;
 * saving itself still needs an unlocked vault (PasskeyAutofillService.onSaveRequest).
 */
data class AutofillResponsePlan(val datasets: Boolean, val offerSave: Boolean) {
    companion object {
        /** The "Unlock to fill" suggestion: no datasets before authentication, never any entry names. */
        fun forAuthentication(canSave: Boolean) = AutofillResponsePlan(datasets = false, offerSave = canSave)

        /** After authentication: null (no response) only when there is nothing to fill and nothing to save. */
        fun afterUnlock(matches: Int, canSave: Boolean): AutofillResponsePlan? = when {
            matches > 0 -> AutofillResponsePlan(datasets = true, offerSave = canSave)
            canSave -> AutofillResponsePlan(datasets = false, offerSave = true)
            else -> null
        }
    }
}
