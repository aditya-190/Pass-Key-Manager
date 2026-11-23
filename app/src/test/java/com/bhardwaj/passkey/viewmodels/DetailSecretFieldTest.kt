package com.bhardwaj.passkey.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.bhardwaj.passkey.data.local.entity.Details
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailEvents
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailViewModel
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import com.bhardwaj.passkey.utils.PasswordAnalyzer
import com.bhardwaj.passkey.utils.SecretKeywords
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The value field is either a secret (password keyboard, never learned) or ordinary text (normal keyboard,
 * Enter key and suggestions for usernames and notes). 5.5.2 forced the password keyboard on every value.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DetailSecretFieldTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakePasskeyRepository()
    private val keywords = SecretKeywords { setOf("password", "contraseña", "pin") }

    private fun vm() =
        DetailViewModel(repository, SavedStateHandle(mapOf("previewId" to 1L)), RecordingAnalyticsTracker(), keywords)

    @Test
    fun `the switch follows the title until the user sets it`() {
        val vm = vm()
        vm.onEvent(DetailEvents.OnAddDetailClick)
        vm.onEvent(DetailEvents.OnTitleChange("Contrasena del banco"))
        assertTrue(vm.isSecretField.value)
        vm.onEvent(DetailEvents.OnTitleChange("Username"))
        assertFalse(vm.isSecretField.value)

        vm.onEvent(DetailEvents.OnSecretToggle(true))
        vm.onEvent(DetailEvents.OnTitleChange("Email"))
        assertTrue("a manual choice is never overridden", vm.isSecretField.value)
    }

    @Test
    fun `a generated password is always a secret`() {
        val vm = vm()
        vm.onEvent(DetailEvents.OnAddDetailClick)
        vm.onEvent(DetailEvents.OnTitleChange("Note"))
        vm.onEvent(DetailEvents.OnGeneratePasswordClick)
        assertTrue(vm.isSecretField.value)
    }

    @Test
    fun `the choice is saved with the field`() = runTest {
        val vm = vm()
        vm.onEvent(DetailEvents.OnAddDetailClick)
        vm.onEvent(DetailEvents.OnTitleChange("Username"))
        vm.onEvent(DetailEvents.OnDescriptionChange("aditya"))
        vm.onEvent(DetailEvents.OnSaveClick)
        vm.onEvent(DetailEvents.OnAddDetailClick)
        vm.onEvent(DetailEvents.OnTitleChange("PIN"))
        vm.onEvent(DetailEvents.OnDescriptionChange("4321"))
        vm.onEvent(DetailEvents.OnSaveClick)

        assertEquals(false, repository.allDetails.single { it.question == "Username" }.isSecret)
        assertEquals(true, repository.allDetails.single { it.question == "PIN" }.isSecret)
    }

    @Test
    fun `editing a stored secret keeps it secret, and turning it off sticks`() = runTest {
        val id = repository.upsertDetails(Details(previewId = 1, question = "Wi-Fi", answer = "x", isSecret = true))
        val vm = vm()
        vm.onEvent(DetailEvents.OnChangeClick(repository.getDetailById(id)!!))
        assertTrue(vm.isSecretField.value)

        vm.onEvent(DetailEvents.OnSecretToggle(false))
        vm.onEvent(DetailEvents.OnSaveClick)
        assertEquals(false, repository.getDetailById(id)!!.isSecret)
    }

    @Test
    fun `closing the sheet resets the switch for the next field`() {
        val vm = vm()
        vm.onEvent(DetailEvents.OnAddDetailClick)
        vm.onEvent(DetailEvents.OnSecretToggle(true))
        vm.onEvent(DetailEvents.OnDismissBottomSheet)
        vm.onEvent(DetailEvents.OnAddDetailClick)
        assertFalse(vm.isSecretField.value)
    }

    @Test
    fun `keyword matching ignores case and accents`() {
        val words = setOf("contraseña", "PIN")
        assertTrue(PasswordAnalyzer.looksSecret("CONTRASENA", words))
        assertTrue(PasswordAnalyzer.looksSecret("Bank pin", words))
        assertFalse(PasswordAnalyzer.looksSecret("Username", words))
        assertFalse(PasswordAnalyzer.looksSecret("anything", setOf(" ")))
    }
}
