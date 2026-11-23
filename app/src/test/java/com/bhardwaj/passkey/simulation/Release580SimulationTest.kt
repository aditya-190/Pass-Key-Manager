package com.bhardwaj.passkey.simulation

import androidx.lifecycle.SavedStateHandle
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.domain.model.PasswordCharacterClass
import com.bhardwaj.passkey.domain.model.PasswordPolicy
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailIntent
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailViewModel
import com.bhardwaj.passkey.presentation.screens.preview_screen.PreviewIntent
import com.bhardwaj.passkey.presentation.screens.preview_screen.PreviewViewModel
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import java.util.Random
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
 * Feature workflow step 3 for 5.8.0 phase 4a: seeded random intent sequences (rapid taps, stale indices,
 * swipes during edits, multi-script and 10k-character input) through the single-state ViewModels. Invariants
 * are checked after every intent; each failure names its seed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Release580SimulationTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val symbols = "abcXYZ019 ,;\"'\n\t€ñहिন্দীعربي中文🔐😀-_".codePoints().toArray()
        .map { String(Character.toChars(it)) }

    private fun Random.text(): String = when (nextInt(8)) {
        0 -> ""
        1 -> "   "
        2 -> buildString { repeat(10_000) { append(symbols[nextInt(symbols.size)]) } }
        else -> buildString { repeat(nextInt(24) + 1) { append(symbols[nextInt(symbols.size)]) } }
    }

    private fun TestScope.subscribe(flow: kotlinx.coroutines.flow.Flow<*>) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }

    @Test
    fun `random intent sequences on the vault home keep every invariant`() = runTest {
        var steps = 0
        repeat(SEEDS) { seed ->
            val rnd = Random(seed.toLong())
            val repo = FakePasskeyRepository()
            repeat(rnd.nextInt(6)) { i ->
                val id = repo.createPreview("Seed $i", Category.entries[rnd.nextInt(4)], i.toLong())
                repeat(rnd.nextInt(3)) { repo.createDetail(id, "q$it", "a$it") }
            }
            val vm = PreviewViewModel(repo, SavedStateHandle(), RecordingAnalyticsTracker())
            val job = subscribe(vm.state)
            var confirmed = false
            val originalIds = repo.allPreviews.map { it.id }.toSet()

            repeat(rnd.nextInt(40)) {
                val pick = kotlin.random.Random(seed * 100 + it)
                val intent = rnd.previewIntent(vm.state.value.items, pick) ?: return@repeat
                if (intent == PreviewIntent.DeleteConfirmed && vm.state.value.pendingDelete != null) confirmed = true
                vm.onIntent(intent)
                steps++

                val state = vm.state.value
                val ids = repo.allPreviews.map { p -> p.id }.toSet()
                assertTrue("seed $seed orphaned field", repo.allDetails.all { d -> d.previewId in ids })
                assertFalse("seed $seed pending row shown", state.items.any { p -> p.id == state.pendingDelete?.id })
                assertTrue("seed $seed wrong category", state.items.all { p -> p.category == state.category })
                assertEquals("seed $seed unsorted", state.items.sortedBy { p -> p.sequence }, state.items)
                assertTrue(
                    "seed $seed blank heading stored",
                    repo.allPreviews.all { p -> p.heading.isNotBlank() && p.heading == p.heading.trim() }
                )
                if (state.editor == null) assertTrue(state.editor?.error == null)
            }
            if (!confirmed) {
                assertTrue(
                    "seed $seed lost an entry without confirming",
                    repo.allPreviews.map {
                        it.id
                    }.containsAll(originalIds)
                )
            }
            job.cancel()
        }
        assertTrue(steps > SEEDS)
    }

    @Test
    fun `random intent sequences on an entry's fields keep every invariant`() = runTest {
        repeat(SEEDS) { seed ->
            val rnd = Random(seed.toLong() * 31)
            val repo = FakePasskeyRepository()
            val previewId = repo.createPreview("Entry", Category.BANKS)
            repeat(rnd.nextInt(5)) { repo.createDetail(previewId, "q$it", "a$it", sequence = it.toLong()) }
            val handle = SavedStateHandle(mapOf("previewId" to previewId))
            val vm = DetailViewModel(repo, handle, RecordingAnalyticsTracker(), { setOf("pin", "password") })
            val job = subscribe(vm.state)

            repeat(rnd.nextInt(40)) {
                val items = vm.state.value.items
                val intent = when (rnd.nextInt(16)) {
                    0 -> DetailIntent.QueryChanged(if (rnd.nextBoolean()) "" else rnd.text().take(2))
                    1 -> DetailIntent.AddClicked
                    2 -> items.randomOrNull(kotlin.random.Random(seed + it))?.let { d -> DetailIntent.EditClicked(d) }
                    3 -> DetailIntent.QuestionChanged(rnd.text())
                    4 -> DetailIntent.AnswerChanged(rnd.text())
                    5 -> DetailIntent.SecretToggled(rnd.nextBoolean())
                    6 -> DetailIntent.SaveClicked
                    7 -> DetailIntent.EditorDismissed
                    8 -> items.randomOrNull(kotlin.random.Random(seed - it))?.let { d ->
                        DetailIntent.SwipedToDelete(d)
                    }
                    9 -> DetailIntent.DeleteConfirmed
                    10 -> DetailIntent.UndoDelete
                    11 -> DetailIntent.Moved(rnd.nextInt(7) - 1, rnd.nextInt(7) - 1)
                    12 -> DetailIntent.LengthChanged(rnd.nextInt(100) - 20)
                    13 -> DetailIntent.CharacterClassToggled(
                        PasswordCharacterClass.entries[rnd.nextInt(4)],
                        rnd.nextBoolean()
                    )
                    14 -> DetailIntent.GenerateClicked
                    else -> DetailIntent.DeleteCancelled
                } ?: return@repeat
                vm.onIntent(intent)

                val state = vm.state.value
                assertTrue(
                    "seed $seed policy length",
                    state.policy.length in PasswordPolicy.MIN_LENGTH..PasswordPolicy.MAX_LENGTH
                )
                assertTrue("seed $seed empty policy", state.policy.classes.isNotEmpty())
                assertFalse("seed $seed pending row shown", state.items.any { d -> d.id == state.pendingDelete?.id })
                assertTrue("seed $seed field left its entry", repo.allDetails.all { d -> d.previewId == previewId })
                assertTrue(
                    "seed $seed untrimmed or blank field stored",
                    repo.allDetails.all { d ->
                        d.question.isNotBlank() &&
                            d.answer.isNotBlank() &&
                            d.answer == d.answer.trim()
                    }
                )
                // The editor can hold a secret; it must never reach the saved-state bundle.
                assertEquals("seed $seed saved state grew", setOf("previewId"), handle.keys())
            }
            job.cancel()
        }
    }

    private fun Random.previewIntent(
        items: List<com.bhardwaj.passkey.domain.model.Preview>,
        pick: kotlin.random.Random
    ): PreviewIntent? = when (nextInt(14)) {
        0 -> PreviewIntent.CategorySelected(Category.entries[nextInt(4)])
        1 -> PreviewIntent.QueryChanged(if (nextBoolean()) "" else text().take(3))
        2 -> PreviewIntent.AddClicked
        3 -> items.randomOrNull(pick)?.let { PreviewIntent.EditClicked(it) }
        4 -> PreviewIntent.HeadingChanged(text())
        5 -> PreviewIntent.SaveClicked
        6 -> PreviewIntent.EditorDismissed
        7 -> items.randomOrNull(pick)?.let { PreviewIntent.SwipedToDelete(it) }
        8 -> PreviewIntent.DeleteConfirmed
        9 -> PreviewIntent.DeleteCancelled
        10 -> PreviewIntent.UndoDelete
        11 -> PreviewIntent.Moved(nextInt(8) - 1, nextInt(8) - 1)
        12 -> PreviewIntent.ItemLongPressed(text())
        else -> PreviewIntent.ItemClicked(nextLong())
    }

    private companion object {
        const val SEEDS = 1_000
    }
}
