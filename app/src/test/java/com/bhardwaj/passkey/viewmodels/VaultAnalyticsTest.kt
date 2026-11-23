package com.bhardwaj.passkey.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.bhardwaj.passkey.analytics.AppEventNames
import com.bhardwaj.passkey.analytics.AppParams
import com.bhardwaj.passkey.data.local.entity.Details
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailEvents
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailViewModel
import com.bhardwaj.passkey.presentation.screens.preview_screen.PreviewEvents
import com.bhardwaj.passkey.presentation.screens.preview_screen.PreviewViewModel
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import com.bhardwaj.passkey.utils.Categories
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The vault screens report *that* something happened, never *what*. Every test that types content
 * also asserts the content does not appear anywhere in the logged params.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VaultAnalyticsTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val analytics = RecordingAnalyticsTracker()
    private val repository = FakePasskeyRepository()

    private fun loggedValues() = analytics.events.flatMap { it.params.values }.map { it.toString() }

    @Test
    fun `creating an entry logs its category only`() = runTest {
        val vm = PreviewViewModel(repository, SavedStateHandle(), analytics)
        vm.onEvent(PreviewEvents.OnBottomNavigationClick(Categories.MAILS.name))
        vm.onEvent(PreviewEvents.OnAddPreviewClick)
        vm.onEvent(PreviewEvents.OnHeadingChange("my-secret-bank"))
        vm.onEvent(PreviewEvents.OnSaveClick)

        assertEquals(1, repository.allPreviews.size)
        val event = analytics.events.single()
        assertEquals(AppEventNames.ENTRY_CREATED, event.name)
        assertEquals("mails", event.params[AppParams.CATEGORY])
        assertFalse(loggedValues().any { it.contains("secret") })
    }

    @Test
    fun `renaming an entry is not counted as a new entry`() = runTest {
        val id = repository.upsertPreview(
            com.bhardwaj.passkey.data.local.entity.Preview(heading = "Old", categoryName = Categories.BANKS)
        )
        val vm = PreviewViewModel(repository, SavedStateHandle(), analytics)
        vm.onEvent(PreviewEvents.OnChangeClick(repository.getPreviewById(id)!!))
        vm.onEvent(PreviewEvents.OnHeadingChange("New"))
        vm.onEvent(PreviewEvents.OnSaveClick)
        assertEquals("New", repository.getPreviewById(id)!!.heading)
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun `generating a password logs a length bucket, not the password`() = runTest {
        val vm = DetailViewModel(repository, SavedStateHandle(mapOf("previewId" to 1L)), analytics, { emptySet() })
        vm.onEvent(DetailEvents.OnGeneratePasswordClick)

        val generated = vm.detailResponse.value
        assertEquals(12, generated.length)
        val event = analytics.events.single()
        assertEquals(AppEventNames.PASSWORD_GENERATED, event.name)
        assertEquals("12-15", event.params[AppParams.LENGTH_BUCKET])
        assertFalse(loggedValues().any { it.contains(generated) })
    }

    @Test
    fun `saving a field logs add versus edit without content`() = runTest {
        val vm = DetailViewModel(repository, SavedStateHandle(mapOf("previewId" to 1L)), analytics, { emptySet() })
        vm.onEvent(DetailEvents.OnAddDetailClick)
        vm.onEvent(DetailEvents.OnTitleChange("Password"))
        vm.onEvent(DetailEvents.OnDescriptionChange("hunter2"))
        vm.onEvent(DetailEvents.OnSaveClick)

        assertEquals(1, repository.allDetails.size)
        assertEquals(AppEventNames.FIELD_SAVED, analytics.events.single().name)
        assertEquals(false, analytics.events.single().params[AppParams.IS_EDIT])
        assertFalse(loggedValues().any { it.contains("hunter2") || it.contains("Password") })
    }

    @Test
    fun `copying a secret logs that it was sensitive, not the value`() = runTest {
        val vm = DetailViewModel(repository, SavedStateHandle(mapOf("previewId" to 1L)), analytics, { emptySet() })
        vm.onEvent(
            DetailEvents.OnLongPress(
                Details(detailsId = 1, previewId = 1, question = "PIN", answer = "4321").answer
            )
        )
        assertEquals(AppEventNames.VALUE_COPIED, analytics.events.single().name)
        assertEquals(true, analytics.events.single().params[AppParams.SENSITIVE])
        assertFalse(loggedValues().any { it.contains("4321") })
    }
}
