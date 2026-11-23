package com.bhardwaj.passkey.domain.credentials

import com.bhardwaj.passkey.domain.autofill.AppTrust
import com.bhardwaj.passkey.domain.autofill.AutofillCredential
import java.security.MessageDigest

/** What PassKey answers first in Android 14's sign-in sheet (6.0.0). */
enum class BeginPlan {
    /** One "Unlock" action, never entry names: the sheet is drawn over another app. */
    UNLOCK_ACTION,

    /** Not a password request (passkeys, other types): offer nothing. */
    NOTHING
}

object CredentialProviderPolicy {
    fun begin(hasPasswordOption: Boolean): BeginPlan =
        if (hasPasswordOption) BeginPlan.UNLOCK_ACTION else BeginPlan.NOTHING

    /**
     * What the sheet may offer. A browser asking on behalf of a website gets nothing (web pages need a trusted-browser
     * list first; the browser's own package name must never stand in for the site). A login without a username is
     * left out: Credential Manager passwords need one, and an entry's name must not be sent in its place.
     */
    fun offerable(credentials: List<AutofillCredential>, forWebsite: Boolean): List<AutofillCredential> =
        if (forWebsite) emptyList() else credentials.filter { !it.username.isNullOrBlank() }

    /** The chosen login, found again from fresh matches (never trusted from the intent alone). */
    fun pick(credentials: List<AutofillCredential>, previewId: Long, username: String?): AutofillCredential? =
        credentials.firstOrNull { it.previewId == previewId && it.username == username }

    /** SHA-256 of each signing certificate, as the autofill trust store records it. */
    fun signerFingerprint(certificates: List<ByteArray>): String? = AppTrust.fingerprint(
        certificates.map { cert ->
            MessageDigest.getInstance("SHA-256").digest(cert).joinToString("") { "%02x".format(it) }
        }
    )
}
