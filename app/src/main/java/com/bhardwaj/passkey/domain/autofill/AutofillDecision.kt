package com.bhardwaj.passkey.domain.autofill

/**
 * What a fill request gets. Deliberately not a function of whether the vault is open: an unlocked vault (the grace
 * period after any unlock, or auto-lock "Never") used to hand logins to any matching app without asking. Every
 * fill now goes through an authentication entry that carries no entry names.
 */
enum class AutofillDecision {
    None,
    Authenticate;

    companion object {
        fun forRequest(isFillable: Boolean): AutofillDecision = if (isFillable) Authenticate else None
    }
}
