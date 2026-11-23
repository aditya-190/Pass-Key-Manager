package com.bhardwaj.passkey.review

import com.bhardwaj.passkey.core.Clock
import com.bhardwaj.passkey.testing.FakeReviewPromptStore
import com.bhardwaj.passkey.testing.FixedConfigProvider
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeReviewViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val now = 1_800_000_000_000L

    private fun prompter(store: FakeReviewPromptStore) =
        ReviewPrompter(store, Clock { now }, FixedConfigProvider(), RecordingAnalyticsTracker())

    @Test
    fun `Home records the first look`() = runTest {
        val store = FakeReviewPromptStore()
        HomeReviewViewModel(prompter(store))
        assertEquals(now, store.flow.value.firstSeenAt)
    }

    @Test
    fun `after enough autofills, opening Home asks once`() = runTest {
        val store = FakeReviewPromptStore(ReviewPromptState(firstSeenAt = now - 10 * 86_400_000L, autofillFills = 3))
        val vm = HomeReviewViewModel(prompter(store))
        assertEquals(Unit, vm.requests.first())
        assertEquals(now, store.flow.value.lastAskedAt)
    }
}
