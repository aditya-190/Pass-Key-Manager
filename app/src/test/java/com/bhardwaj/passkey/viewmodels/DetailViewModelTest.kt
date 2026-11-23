package com.bhardwaj.passkey.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.analytics.AppEventNames
import com.bhardwaj.passkey.analytics.AppParams
import com.bhardwaj.passkey.domain.model.PasswordCharacterClass
import com.bhardwaj.passkey.domain.model.PasswordPolicy
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailEffect
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailIntent
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailViewModel
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import com.bhardwaj.passkey.utils.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Single-state MVI contract of an entry's fields (5.8.0). */
@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakePasskeyRepository()
    private val analytics = RecordingAnalyticsTracker()

    private fun TestScope.vm(previewId: Long = 1) =
        DetailViewModel(repository, SavedStateHandle(mapOf("previewId" to previewId)), analytics, { setOf("pin") })
            .also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} } }

    @Test
    fun `fields load for this entry only and search matches title or value`() = runTest {
        repository.createDetail(1, "User", "aditya", sequence = 0)
        repository.createDetail(1, "Note", "gate code 99", sequence = 1)
        repository.createDetail(2, "Other", "x")
        val vm = vm()
        assertFalse(vm.state.value.isLoading)
        assertEquals(listOf("User", "Note"), vm.state.value.items.map { it.question })

        vm.onIntent(DetailIntent.QueryChanged("GATE"))
        assertEquals(listOf("Note"), vm.state.value.items.map { it.question })
        assertFalse(vm.state.value.showEmptyState)
    }

    @Test
    fun `swipe asks first, confirm deletes, Undo restores the same row`() = runTest {
        val id = repository.createDetail(1, "PIN", "4321", isSecret = true)
        val vm = vm()
        val pin = vm.state.value.items.single()

        vm.onIntent(DetailIntent.SwipedToDelete(pin))
        assertTrue(vm.state.value.items.isEmpty())
        assertEquals(1, repository.allDetails.size)
        vm.onIntent(DetailIntent.DeleteCancelled)
        assertEquals(listOf(pin), vm.state.value.items)

        vm.onIntent(DetailIntent.SwipedToDelete(pin))
        vm.onIntent(DetailIntent.DeleteConfirmed)
        assertTrue(repository.allDetails.isEmpty())
        assertEquals(DetailEffect.ShowUndo(UiText.StringResource(R.string.field_deleted)), vm.effects.first())
        vm.onIntent(DetailIntent.UndoDelete)
        assertEquals(pin, repository.getDetailById(id))
    }

    @Test
    fun `an incomplete field keeps the sheet and what was typed`() = runTest {
        val vm = vm()
        vm.onIntent(DetailIntent.AddClicked)
        vm.onIntent(DetailIntent.QuestionChanged("Recovery code"))
        vm.onIntent(DetailIntent.SaveClicked)
        val editor = vm.state.value.editor!!
        assertEquals("Recovery code", editor.question)
        assertEquals(UiText.StringResource(R.string.enter_valid_title_n_response), editor.error)
        assertTrue(repository.allDetails.isEmpty())

        vm.onIntent(DetailIntent.AnswerChanged("  abc-123 "))
        assertNull("typing clears the error", vm.state.value.editor!!.error)
        vm.onIntent(DetailIntent.SaveClicked)
        assertNull(vm.state.value.editor)
        assertEquals("abc-123", repository.allDetails.single().answer)
    }

    @Test
    fun `an edit updates the same row and is logged as an edit`() = runTest {
        val id = repository.createDetail(1, "User", "old")
        val vm = vm()
        vm.onIntent(DetailIntent.EditClicked(vm.state.value.items.single()))
        assertTrue(vm.state.value.editor!!.isEdit)
        vm.onIntent(DetailIntent.AnswerChanged("new"))
        vm.onIntent(DetailIntent.SaveClicked)
        assertEquals("new", repository.getDetailById(id)!!.answer)
        assertEquals(1, repository.allDetails.size)
        val saved = analytics.events.single { it.name == AppEventNames.FIELD_SAVED }
        assertEquals(true, saved.params[AppParams.IS_EDIT])
    }

    @Test
    fun `the generator follows a typed policy that can never be empty or out of range`() = runTest {
        val vm = vm()
        vm.onIntent(DetailIntent.AddClicked)
        vm.onIntent(DetailIntent.LengthChanged(99))
        assertEquals(PasswordPolicy.MAX_LENGTH, vm.state.value.policy.length)
        vm.onIntent(DetailIntent.LengthChanged(1))
        assertEquals(PasswordPolicy.MIN_LENGTH, vm.state.value.policy.length)
        vm.onIntent(DetailIntent.LengthChanged(20))

        PasswordCharacterClass.entries.forEach { vm.onIntent(DetailIntent.CharacterClassToggled(it, false)) }
        assertEquals(setOf(PasswordCharacterClass.LOWERCASE), vm.state.value.policy.classes)

        vm.onIntent(DetailIntent.GenerateClicked)
        val editor = vm.state.value.editor!!
        assertEquals(20, editor.answer.length)
        assertTrue(editor.answer.all { it.isLowerCase() })
        assertTrue("a generated value is a secret", editor.isSecret)
    }

    @Test
    fun `moves renumber the full list and are ignored while searching`() = runTest {
        val a = repository.createDetail(1, "A", "1", sequence = 0)
        val b = repository.createDetail(1, "B", "2", sequence = 1)
        val vm = vm()
        vm.onIntent(DetailIntent.QueryChanged("A"))
        vm.onIntent(DetailIntent.Moved(0, 0))
        vm.onIntent(DetailIntent.QueryChanged(""))
        vm.onIntent(DetailIntent.Moved(fromIndex = 1, toIndex = 0))
        assertEquals(1L, repository.getDetailById(a)!!.sequence)
        assertEquals(0L, repository.getDetailById(b)!!.sequence)
        vm.onIntent(DetailIntent.Moved(fromIndex = 9, toIndex = 0))
    }

    @Test
    fun `long-pressing a value copies it as sensitive`() = runTest {
        val vm = vm()
        vm.onIntent(DetailIntent.LongPressed("4321"))
        assertEquals(DetailEffect.CopyToClipboard("4321", isSensitive = true), vm.effects.first())
    }
}
