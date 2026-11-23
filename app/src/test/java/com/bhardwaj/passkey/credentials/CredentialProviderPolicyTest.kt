package com.bhardwaj.passkey.credentials

import com.bhardwaj.passkey.domain.autofill.AppTrust
import com.bhardwaj.passkey.domain.autofill.AutofillCredential
import com.bhardwaj.passkey.domain.credentials.BeginPlan
import com.bhardwaj.passkey.domain.credentials.CredentialProviderPolicy
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 6.0.0: PassKey in Android 14's sign-in sheet follows the same rules as autofill. */
class CredentialProviderPolicyTest {
    @Test
    fun `a password request gets one unlock action, anything else nothing`() {
        assertEquals(BeginPlan.UNLOCK_ACTION, CredentialProviderPolicy.begin(hasPasswordOption = true))
        assertEquals(BeginPlan.NOTHING, CredentialProviderPolicy.begin(hasPasswordOption = false))
    }

    @Test
    fun `the chosen login is found again from fresh matches`() {
        val a = AutofillCredential(1, "Shop", "me", "p1")
        val b = AutofillCredential(1, "Shop", "you", "p2")
        val c = AutofillCredential(2, "Shop work", null, "p3")
        assertEquals(b, CredentialProviderPolicy.pick(listOf(a, b, c), previewId = 1, username = "you"))
        assertEquals(c, CredentialProviderPolicy.pick(listOf(a, b, c), previewId = 2, username = null))
        assertNull("no longer a match", CredentialProviderPolicy.pick(listOf(a, b), previewId = 9, username = "me"))
    }

    @Test
    fun `the caller's signer is fingerprinted exactly like autofill's`() {
        val cert = byteArrayOf(1, 2, 3)
        val hex = MessageDigest.getInstance("SHA-256").digest(cert).joinToString("") { "%02x".format(it) }
        assertEquals(AppTrust.fingerprint(listOf(hex)), CredentialProviderPolicy.signerFingerprint(listOf(cert)))
        assertNull(CredentialProviderPolicy.signerFingerprint(emptyList()))
    }

    @Test
    fun `a browser asking for a website gets nothing, and logins without a username are left out`() {
        // 6.0.0 attack pass: Chrome's own package name must not stand in for the site it is asking about, and the
        // sheet must never send an entry's name as a username.
        val withUser = AutofillCredential(1, "Shop", "me", "p1")
        val noUser = AutofillCredential(2, "PIN only", null, "1234")
        assertEquals(listOf(withUser), CredentialProviderPolicy.offerable(listOf(withUser, noUser), forWebsite = false))
        assertEquals(
            emptyList<AutofillCredential>(),
            CredentialProviderPolicy.offerable(listOf(withUser), forWebsite = true)
        )
    }
}
