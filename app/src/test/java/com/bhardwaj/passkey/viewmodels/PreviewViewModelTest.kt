package com.bhardwaj.passkey.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.analytics.AppEventNames
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.presentation.screens.preview_screen.PreviewEffect
import com.bhardwaj.passkey.presentation.screens.preview_screen.PreviewIntent
import com.bhardwaj.passkey.presentation.screens.preview_screen.PreviewViewModel
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import com.bhardwaj.passkey.utils.Constants.Companion.PREVIEW_CATEGORY_NAME
import com.bhardwaj.passkey.utils.UiText
import kotlinx.coroutines.CompletableDeferred
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

/** Single-state MVI contract of the vault home (5.8.0). Plain JVM, against an in-memory repository. */
@OptIn(ExperimentalCoroutinesApi::class)
class PreviewViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakePasskeyRepository()
    private val analytics = RecordingAnalyticsTracker()

    private fun TestScope.vm(handle: SavedStateHandle = SavedStateHandle()) =
        PreviewViewModel(repository, handle, analytics).also { vm ->
            // The state is shared WhileSubscribed, like the screen; keep a subscriber for the test's lifetime.
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        }

    @Test
    fun `the list loads per category and only then reports empty`() = runTest {
        repository.createPreview("Bank A", Category.BANKS)
        repository.createPreview("Mail A", Category.MAILS)
        val vm = vm()
        assertFalse(vm.state.value.isLoading)
        assertEquals(listOf("Bank A"), vm.state.value.items.map { it.heading })

        vm.onIntent(PreviewIntent.CategorySelected(Category.OTHERS))
        assertTrue(vm.state.value.items.isEmpty())
        assertTrue(vm.state.value.showEmptyState)
    }

    @Test
    fun `an unknown saved category falls back instead of crashing`() = runTest {
        val vm = vm(SavedStateHandle(mapOf(PREVIEW_CATEGORY_NAME to "NOT_A_CATEGORY")))
        assertEquals(Category.OTHERS, vm.state.value.category)
    }

    @Test
    fun `search filters case-insensitively and switching category clears it`() = runTest {
        repository.createPreview("Gmail", Category.BANKS)
        repository.createPreview("Bank", Category.BANKS)
        val vm = vm()
        vm.onIntent(PreviewIntent.QueryChanged("GMA"))
        assertEquals(listOf("Gmail"), vm.state.value.items.map { it.heading })
        assertFalse("a search with no hits is not the empty vault", vm.state.value.showEmptyState)

        vm.onIntent(PreviewIntent.CategorySelected(Category.APPS))
        assertEquals("", vm.state.value.query)
    }

    @Test
    fun `swiping hides the row but writes nothing until confirmed, and cancel brings it back`() = runTest {
        val id = repository.createPreview("Bank", Category.BANKS)
        val vm = vm()
        val row = vm.state.value.items.single()

        vm.onIntent(PreviewIntent.SwipedToDelete(row))
        assertEquals(row, vm.state.value.pendingDelete)
        assertTrue(vm.state.value.items.isEmpty())
        assertEquals(id, repository.allPreviews.single().id)

        vm.onIntent(PreviewIntent.DeleteCancelled)
        assertNull(vm.state.value.pendingDelete)
        assertEquals(listOf(row), vm.state.value.items)
    }

    @Test
    fun `confirming deletes the entry with its fields and Undo restores both`() = runTest {
        val id = repository.createPreview("Bank", Category.BANKS)
        repository.createDetail(id, "PIN", "4321", isSecret = true)
        val vm = vm()
        val before = repository.allDetails

        vm.onIntent(PreviewIntent.SwipedToDelete(vm.state.value.items.single()))
        vm.onIntent(PreviewIntent.DeleteConfirmed)
        assertTrue(repository.allPreviews.isEmpty())
        assertTrue("no orphaned fields", repository.allDetails.isEmpty())
        assertEquals(PreviewEffect.ShowUndo(UiText.StringResource(R.string.entry_deleted)), vm.effects.first())

        vm.onIntent(PreviewIntent.UndoDelete)
        assertEquals(id, repository.allPreviews.single().id)
        assertEquals(before, repository.allDetails)
    }

    @Test
    fun `a rejected heading keeps the sheet open with what was typed`() = runTest {
        repository.createPreview("Gmail", Category.BANKS)
        val vm = vm()
        vm.onIntent(PreviewIntent.AddClicked)
        vm.onIntent(PreviewIntent.HeadingChanged("   "))
        vm.onIntent(PreviewIntent.SaveClicked)
        assertEquals(UiText.StringResource(R.string.enter_valid_heading), vm.state.value.editor?.error)

        vm.onIntent(PreviewIntent.HeadingChanged("gmail "))
        assertNull("typing clears the error", vm.state.value.editor?.error)
        vm.onIntent(PreviewIntent.SaveClicked)
        assertEquals(UiText.StringResource(R.string.heading_exists), vm.state.value.editor?.error)
        assertEquals("gmail ", vm.state.value.editor?.heading)
        assertEquals(1, repository.allPreviews.size)
    }

    @Test
    fun `saving an edit without renaming is not a clash`() = runTest {
        repository.createPreview("Gmail", Category.BANKS)
        val vm = vm()
        vm.onIntent(PreviewIntent.EditClicked(vm.state.value.items.single()))
        assertTrue(vm.state.value.editor!!.isEdit)
        vm.onIntent(PreviewIntent.SaveClicked)
        assertNull(vm.state.value.editor)
        assertEquals("Gmail", repository.allPreviews.single().heading)
    }

    @Test
    fun `a new entry is saved trimmed in the current category and counted`() = runTest {
        val vm = vm()
        vm.onIntent(PreviewIntent.CategorySelected(Category.MAILS))
        vm.onIntent(PreviewIntent.AddClicked)
        vm.onIntent(PreviewIntent.HeadingChanged("  Work mail "))
        vm.onIntent(PreviewIntent.SaveClicked)
        assertNull(vm.state.value.editor)
        val saved = repository.allPreviews.single()
        assertEquals("Work mail" to Category.MAILS, saved.heading to saved.category)
        assertTrue(AppEventNames.ENTRY_CREATED in analytics.names)
    }

    @Test
    fun `moves renumber the visible category and ignore stale indices`() = runTest {
        val a = repository.createPreview("A", Category.BANKS, sequence = 0)
        val b = repository.createPreview("B", Category.BANKS, sequence = 1)
        val other = repository.createPreview("X", Category.APPS, sequence = 0)
        val vm = vm()

        vm.onIntent(PreviewIntent.Moved(fromIndex = 5, toIndex = 0))
        vm.onIntent(PreviewIntent.Moved(fromIndex = 1, toIndex = 0))
        assertEquals(listOf("B", "A"), vm.state.value.items.map { it.heading })
        assertEquals(1L, repository.allPreviews.single { it.id == a }.sequence)
        assertEquals(0L, repository.allPreviews.single { it.id == b }.sequence)
        assertEquals(0L, repository.allPreviews.single { it.id == other }.sequence)
    }

    @Test
    fun `navigation and copy are effects`() = runTest {
        val vm = vm()
        vm.onIntent(PreviewIntent.ItemClicked(7))
        assertEquals(PreviewEffect.Navigate(NavRoute.Details(7)), vm.effects.first())
        vm.onIntent(PreviewIntent.SettingsClicked)
        assertEquals(PreviewEffect.Navigate(NavRoute.Settings), vm.effects.first())
        vm.onIntent(PreviewIntent.ItemLongPressed("Gmail"))
        assertEquals(PreviewEffect.CopyToClipboard("Gmail", isSensitive = false), vm.effects.first())
        assertTrue(AppEventNames.VALUE_COPIED in analytics.names)
    }

    @Test
    fun `a second Delete tap while the first is running is ignored, so Undo keeps the fields`() = runTest {
        // Attack-pass #1: the dialog stayed up during the write and a second tap replaced the Undo copy.
        val id = repository.createPreview("Bank", Category.BANKS)
        repository.createDetail(id, "PIN", "4321")
        val vm = vm()
        val gate = CompletableDeferred<Unit>()
        repository.writeGate = { gate.await() }

        vm.onIntent(PreviewIntent.SwipedToDelete(vm.state.value.items.single()))
        vm.onIntent(PreviewIntent.DeleteConfirmed)
        assertNull("the dialog closes at once", vm.state.value.pendingDelete)
        vm.onIntent(PreviewIntent.DeleteConfirmed)
        gate.complete(Unit)
        assertEquals(1, repository.transactions)

        vm.onIntent(PreviewIntent.UndoDelete)
        assertEquals(listOf("PIN"), repository.allDetails.map { it.question })
    }

    @Test
    fun `a second Save tap while the first is running does not create a duplicate`() = runTest {
        // Attack-pass #2.
        val vm = vm()
        val gate = CompletableDeferred<Unit>()
        repository.writeGate = { gate.await() }
        vm.onIntent(PreviewIntent.AddClicked)
        vm.onIntent(PreviewIntent.HeadingChanged("Bank"))
        vm.onIntent(PreviewIntent.SaveClicked)
        vm.onIntent(PreviewIntent.SaveClicked)
        gate.complete(Unit)
        assertEquals(1, repository.allPreviews.size)
        assertNull(vm.state.value.editor)
    }

    @Test
    fun `saving an edit of an entry that vanished keeps what was typed as a new entry`() = runTest {
        // Attack-pass #4: the sheet used to close and drop the edit silently.
        val id = repository.createPreview("Old", Category.BANKS)
        val vm = vm()
        vm.onIntent(PreviewIntent.EditClicked(vm.state.value.items.single()))
        repository.deletePreview(repository.getPreviewById(id)!!)
        vm.onIntent(PreviewIntent.HeadingChanged("Renamed"))
        vm.onIntent(PreviewIntent.SaveClicked)
        assertEquals(listOf("Renamed"), repository.allPreviews.map { it.heading })
    }
}
