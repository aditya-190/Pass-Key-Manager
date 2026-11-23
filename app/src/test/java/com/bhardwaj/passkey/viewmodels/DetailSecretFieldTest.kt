package com.bhardwaj.passkey.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailIntent
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailViewModel
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import com.bhardwaj.passkey.utils.PasswordAnalyzer
import com.bhardwaj.passkey.utils.SecretKeywords
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    private fun TestScope.vm() =
        DetailViewModel(repository, SavedStateHandle(mapOf("previewId" to 1L)), RecordingAnalyticsTracker(), keywords)
            .also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} } }

    @Test
    fun `the switch follows the title until the user sets it`() = runTest {
        val vm = vm()
        vm.onIntent(DetailIntent.AddClicked)
        vm.onIntent(DetailIntent.QuestionChanged("Contrasena del banco"))
        assertTrue(vm.state.value.editor!!.isSecret)
        vm.onIntent(DetailIntent.QuestionChanged("Username"))
        assertFalse(vm.state.value.editor!!.isSecret)

        vm.onIntent(DetailIntent.SecretToggled(true))
        vm.onIntent(DetailIntent.QuestionChanged("Email"))
        assertTrue("a manual choice is never overridden", vm.state.value.editor!!.isSecret)
    }

    @Test
    fun `a generated password is always a secret`() = runTest {
        val vm = vm()
        vm.onIntent(DetailIntent.AddClicked)
        vm.onIntent(DetailIntent.QuestionChanged("Note"))
        vm.onIntent(DetailIntent.GenerateClicked)
        assertTrue(vm.state.value.editor!!.isSecret)
    }

    @Test
    fun `the choice is saved with the field`() = runTest {
        val vm = vm()
        vm.onIntent(DetailIntent.AddClicked)
        vm.onIntent(DetailIntent.QuestionChanged("Username"))
        vm.onIntent(DetailIntent.AnswerChanged("aditya"))
        vm.onIntent(DetailIntent.SaveClicked)
        vm.onIntent(DetailIntent.AddClicked)
        vm.onIntent(DetailIntent.QuestionChanged("PIN"))
        vm.onIntent(DetailIntent.AnswerChanged("4321"))
        vm.onIntent(DetailIntent.SaveClicked)

        assertEquals(false, repository.allDetails.single { it.question == "Username" }.isSecret)
        assertEquals(true, repository.allDetails.single { it.question == "PIN" }.isSecret)
    }

    @Test
    fun `editing a stored secret keeps it secret, and turning it off sticks`() = runTest {
        val id = repository.createDetail(previewId = 1, question = "Wi-Fi", answer = "x", isSecret = true)
        val vm = vm()
        vm.onIntent(DetailIntent.EditClicked(repository.getDetailById(id)!!))
        assertTrue(vm.state.value.editor!!.isSecret)

        vm.onIntent(DetailIntent.SecretToggled(false))
        vm.onIntent(DetailIntent.SaveClicked)
        assertEquals(false, repository.getDetailById(id)!!.isSecret)
    }

    @Test
    fun `closing the sheet resets the switch for the next field`() = runTest {
        val vm = vm()
        vm.onIntent(DetailIntent.AddClicked)
        vm.onIntent(DetailIntent.SecretToggled(true))
        vm.onIntent(DetailIntent.EditorDismissed)
        vm.onIntent(DetailIntent.AddClicked)
        assertFalse(vm.state.value.editor!!.isSecret)
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
