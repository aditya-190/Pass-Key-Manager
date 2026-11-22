package com.bhardwaj.passkey.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.local.entity.Details
import com.bhardwaj.passkey.data.local.entity.Preview
import com.bhardwaj.passkey.domain.events.DetailEvents
import com.bhardwaj.passkey.domain.events.PreviewEvents
import com.bhardwaj.passkey.domain.viewModels.DetailViewModel
import com.bhardwaj.passkey.domain.viewModels.PreviewViewModel
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import com.bhardwaj.passkey.utils.Categories
import com.bhardwaj.passkey.utils.UiEvents
import com.bhardwaj.passkey.utils.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        val id = repository.upsertPreview(Preview(heading = "Bank", categoryName = Categories.BANKS, sequence = 3))
        repository.upsertDetails(Details(previewId = id, question = "PIN", answer = "4321", isSecret = true))
        repository.upsertDetails(Details(previewId = id, question = "User", answer = "aditya"))
        return repository.getPreviewById(id)!!
    }

    private fun previewVm() = PreviewViewModel(repository, SavedStateHandle(), analytics)

    private fun detailVm(previewId: Long) =
        DetailViewModel(repository, SavedStateHandle(mapOf("previewId" to previewId)), analytics, { emptySet() })

    @Test
    fun `swiping an entry only asks and cancelling leaves the vault untouched`() = runTest {
        val entry = seedEntry()
        val vm = previewVm()
        vm.onEvent(PreviewEvents.OnSwipedLeft(entry))
        assertTrue(vm.isAlertOpen)
        assertEquals("nothing deleted before confirming", 1, repository.allPreviews.size)

        vm.onEvent(PreviewEvents.OnCancelClick)
        assertEquals(false, vm.isAlertOpen)
        assertEquals(listOf(entry), repository.allPreviews)
        assertEquals(2, repository.allDetails.size)
    }

    @Test
    fun `confirming deletes the entry with its fields, and undo brings both back unchanged`() = runTest {
        val entry = seedEntry()
        val before = repository.allDetails.sortedBy { it.detailsId }
        val vm = previewVm()
        vm.onEvent(PreviewEvents.OnSwipedLeft(entry))
        vm.onEvent(PreviewEvents.OnDeleteClick)

        assertTrue(repository.allPreviews.isEmpty())
        assertTrue("no orphaned fields", repository.allDetails.isEmpty())
        assertEquals(UiEvents.ShowUndo(UiText.StringResource(R.string.entry_deleted)), vm.uiEvents.first())

        vm.onEvent(PreviewEvents.OnUndoDelete)
        assertEquals(listOf(entry), repository.allPreviews)
        assertEquals(before, repository.allDetails.sortedBy { it.detailsId })
    }

    @Test
    fun `a blank heading keeps the sheet open with an error and saves nothing`() = runTest {
        val vm = previewVm()
        vm.onEvent(PreviewEvents.OnAddPreviewClick)
        vm.onEvent(PreviewEvents.OnHeadingChange("   "))
        vm.onEvent(PreviewEvents.OnSaveClick)
        assertTrue(vm.isSheetOpen)
        assertEquals(UiText.StringResource(R.string.enter_valid_heading), vm.sheetError)
        assertTrue(repository.allPreviews.isEmpty())
    }

    @Test
    fun `a duplicate heading keeps what was typed and explains why`() = runTest {
        seedEntry()
        val vm = previewVm()
        vm.onEvent(PreviewEvents.OnAddPreviewClick)
        vm.onEvent(PreviewEvents.OnHeadingChange("Bank"))
        vm.onEvent(PreviewEvents.OnSaveClick)
        assertTrue(vm.isSheetOpen)
        assertEquals("Bank", vm.previewHeading.value)
        assertEquals(UiText.StringResource(R.string.heading_exists), vm.sheetError)

        vm.onEvent(PreviewEvents.OnHeadingChange("Bank 2"))
        assertNull("typing clears the error", vm.sheetError)
    }

    @Test
    fun `fields follow the same rules - confirm before delete, undo, keep input`() = runTest {
        val entry = seedEntry()
        val pin = repository.allDetails.single { it.question == "PIN" }
        val vm = detailVm(entry.previewId!!)

        vm.onEvent(DetailEvents.OnSwipedLeft(pin))
        assertEquals(2, repository.allDetails.size)
        vm.onEvent(DetailEvents.OnDeleteClick)
        assertEquals(listOf("User"), repository.allDetails.map { it.question })
        assertEquals(UiEvents.ShowUndo(UiText.StringResource(R.string.field_deleted)), vm.uiEvents.first())
        vm.onEvent(DetailEvents.OnUndoDelete)
        assertEquals(pin, repository.getDetailById(pin.detailsId!!))

        vm.onEvent(DetailEvents.OnAddDetailClick)
        vm.onEvent(DetailEvents.OnTitleChange("Recovery code"))
        vm.onEvent(DetailEvents.OnSaveClick) // no value typed
        assertTrue(vm.isSheetOpen)
        assertEquals("Recovery code", vm.detailTitle.value)
        assertEquals(UiText.StringResource(R.string.enter_valid_title_n_response), vm.sheetError)
    }
}
