package com.bhardwaj.passkey.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.domain.model.Preview
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailEvents
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailViewModel
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import com.bhardwaj.passkey.utils.UiEvents
import com.bhardwaj.passkey.utils.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 5.6.0 UX fixes: nothing is deleted until the user confirms, a delete can be undone, and a failed save
 * never throws away what the user typed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAndValidationTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakePasskeyRepository()
    private val analytics = RecordingAnalyticsTracker()

    private suspend fun seedEntry(): Preview {
        val id = repository.createPreview(heading = "Bank", category = Category.BANKS, sequence = 3)
        repository.createDetail(previewId = id, question = "PIN", answer = "4321", isSecret = true)
        repository.createDetail(previewId = id, question = "User", answer = "aditya")
        return repository.getPreviewById(id)!!
    }

    private fun detailVm(previewId: Long) =
        DetailViewModel(repository, SavedStateHandle(mapOf("previewId" to previewId)), analytics, { emptySet() })

    // Entry (preview) rules moved to PreviewViewModelTest with the 5.8.0 single-state ViewModel.

    @Test
    fun `fields follow the same rules - confirm before delete, undo, keep input`() = runTest {
        val entry = seedEntry()
        val pin = repository.allDetails.single { it.question == "PIN" }
        val vm = detailVm(entry.id)

        vm.onEvent(DetailEvents.OnSwipedLeft(pin))
        assertEquals(2, repository.allDetails.size)
        vm.onEvent(DetailEvents.OnDeleteClick)
        assertEquals(listOf("User"), repository.allDetails.map { it.question })
        assertEquals(UiEvents.ShowUndo(UiText.StringResource(R.string.field_deleted)), vm.uiEvents.first())
        vm.onEvent(DetailEvents.OnUndoDelete)
        assertEquals(pin, repository.getDetailById(pin.id))

        vm.onEvent(DetailEvents.OnAddDetailClick)
        vm.onEvent(DetailEvents.OnTitleChange("Recovery code"))
        vm.onEvent(DetailEvents.OnSaveClick) // no value typed
        assertTrue(vm.isSheetOpen)
        assertEquals("Recovery code", vm.detailTitle.value)
        assertEquals(UiText.StringResource(R.string.enter_valid_title_n_response), vm.sheetError)
    }
}
