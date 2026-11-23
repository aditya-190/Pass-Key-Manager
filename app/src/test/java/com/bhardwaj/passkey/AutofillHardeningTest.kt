package com.bhardwaj.passkey

import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.analytics.EventValidator
import com.bhardwaj.passkey.domain.autofill.AppTrust
import com.bhardwaj.passkey.domain.autofill.AutofillDecision
import com.bhardwaj.passkey.domain.autofill.AutofillSaveMerger
import com.bhardwaj.passkey.domain.autofill.SaveOp
import com.bhardwaj.passkey.domain.model.Detail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 5.8.0 attack-pass fixes #1, #2, #3 and #6 for autofill. */
class AutofillHardeningTest {
    private fun detail(id: Long, question: String, answer: String, sequence: Long, isSecret: Boolean = false) =
        Detail(id, previewId = 1, question = question, answer = answer, sequence = sequence, isSecret = isSecret)

    // #1
    @Test
    fun `a fillable form always asks for authentication, whatever the vault state`() {
        assertEquals(AutofillDecision.Authenticate, AutofillDecision.forRequest(isFillable = true))
        assertEquals(AutofillDecision.None, AutofillDecision.forRequest(isFillable = false))
    }

    // #2
    @Test
    fun `saving a changed password updates the existing secret instead of adding a second one`() {
        val existing = listOf(detail(1, "User", "me", 0), detail(2, "Password", "old", 1, isSecret = true))
        val ops = AutofillSaveMerger.plan(existing, username = "me", password = "new", secretKeywords = emptySet())
        assertEquals(listOf(SaveOp.Update(existing[1].copy(answer = "new"))), ops)
    }

    @Test
    fun `saving the same login again changes nothing`() {
        val existing = listOf(detail(1, "User", "me", 0), detail(2, "Password", "pw", 1, isSecret = true))
        assertTrue(AutofillSaveMerger.plan(existing, "me", "pw", emptySet()).isEmpty())
    }

    @Test
    fun `an older entry without the secret flag is matched by keyword and marked secret`() {
        val existing = listOf(detail(1, "Email", "me@x.com", 0), detail(2, "Contraseña", "old", 1))
        val ops = AutofillSaveMerger.plan(existing, "me@x.com", "new", secretKeywords = setOf("contraseña"))
        assertEquals(listOf(SaveOp.Update(existing[1].copy(answer = "new", isSecret = true))), ops)
    }

    @Test
    fun `a new entry gets one username and one password field`() {
        val ops = AutofillSaveMerger.plan(emptyList(), "me", "pw", emptySet())
        assertEquals(
            listOf(
                SaveOp.Create(AutofillSaveMerger.USERNAME_LABEL, "me", isSecret = false, sequence = 0),
                SaveOp.Create(AutofillSaveMerger.PASSWORD_LABEL, "pw", isSecret = true, sequence = 1)
            ),
            ops
        )
    }

    @Test
    fun `a blank username never creates or overwrites a field`() {
        val existing = listOf(detail(1, "User", "me", 0), detail(2, "Password", "pw", 1, isSecret = true))
        assertTrue(AutofillSaveMerger.plan(existing, "  ", "pw", emptySet()).isEmpty())
    }

    // #3
    @Test
    fun `an app is trusted on first use and refused if its signer ever changes`() {
        assertEquals(AppTrust.Decision.FirstUse, AppTrust.decide(stored = null, actual = "aa"))
        assertEquals(AppTrust.Decision.Trusted, AppTrust.decide(stored = "aa", actual = "aa"))
        assertEquals(AppTrust.Decision.Mismatch, AppTrust.decide(stored = "aa", actual = "bb"))
        assertEquals("an unreadable signer is never trusted", AppTrust.Decision.Mismatch, AppTrust.decide("aa", null))
        assertEquals(AppTrust.Decision.Mismatch, AppTrust.decide(null, null))
    }

    @Test
    fun `several signers are fingerprinted the same way in any order`() {
        assertEquals(AppTrust.fingerprint(listOf("b", "a")), AppTrust.fingerprint(listOf("a", "b")))
        assertTrue(AppTrust.fingerprint(emptyList()) == null)
    }

    // #6
    @Test
    fun `autofill and authenticator events are valid and carry only enums`() {
        val events = listOf(
            AppEvents.autofillUnlockShown(),
            AppEvents.autofillFilled(matches = 0),
            AppEvents.autofillFilled(matches = 1),
            AppEvents.autofillFilled(matches = 7),
            AppEvents.autofillSaved(AppEvents.AutofillSaveResult.UPDATED),
            AppEvents.authenticatorAdded(fromLink = true)
        )
        events.forEach { assertTrue(it.name, EventValidator.validate(it).isEmpty()) }
        assertEquals(listOf("none", "one", "many"), events.subList(1, 4).map { it.params.values.single() })
        assertEquals("updated", events[4].params.values.single())
        assertEquals("link", events[5].params.values.single())
    }
}
