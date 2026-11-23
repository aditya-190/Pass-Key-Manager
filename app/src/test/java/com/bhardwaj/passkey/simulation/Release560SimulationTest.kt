package com.bhardwaj.passkey.simulation

import androidx.lifecycle.SavedStateHandle
import com.bhardwaj.passkey.banner.AnnouncementRoutes
import com.bhardwaj.passkey.data.backup.BackupCodec
import com.bhardwaj.passkey.data.backup.ImportMode
import com.bhardwaj.passkey.data.security.LockPolicy
import com.bhardwaj.passkey.domain.model.AutoLockTimeout
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.presentation.screens.preview_screen.PreviewIntent
import com.bhardwaj.passkey.presentation.screens.preview_screen.PreviewViewModel
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import com.bhardwaj.passkey.utils.PasswordAnalyzer
import java.util.Random
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Feature workflow step 3 for 5.6.0: seeded runs through backup, lock, delete/undo and remote-input logic.
 * Every assertion carries its seed. Encrypted cases are fewer because each one runs 600k PBKDF2 rounds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Release560SimulationTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val alphabet = "abcXYZ019 ,;\"'\n\t€ñçहिন্দীعربي中文日本語🔐😀-_/\\[]{}"

    // Whole code points: picking single chars split emoji surrogate pairs into lone surrogates, which are not
    // valid Unicode, cannot be typed, and turn into '?' in any UTF-8 file.
    private val symbols = alphabet.codePoints().toArray().map { String(Character.toChars(it)) }

    private fun Random.text(max: Int) = buildString {
        repeat(nextInt(max) + 1) { append(symbols[nextInt(symbols.size)]) }
    }

    private suspend fun randomVault(rnd: Random): FakePasskeyRepository = FakePasskeyRepository().apply {
        val headings = mutableSetOf<Pair<String, Category>>()
        repeat(rnd.nextInt(12)) { i ->
            val category = Category.entries[rnd.nextInt(Category.entries.size)]
            val heading = "${rnd.text(30).trim().ifEmpty { "h" }} $i"
            if (!headings.add(heading to category)) return@repeat
            val id = createPreview(heading = heading, category = category, sequence = i.toLong())
            repeat(rnd.nextInt(6)) { j ->
                createDetail(
                    previewId = id,
                    question = "${rnd.text(20).trim().ifEmpty { "q" }} $j",
                    answer = rnd.text(200),
                    sequence = j.toLong(),
                    isSecret = rnd.nextBoolean()
                )
            }
        }
    }

    private fun FakePasskeyRepository.snapshot() = allPreviews.map { p ->
        Triple(
            p.heading.trim() to p.category,
            p.sequence,
            allDetails.filter { it.previewId == p.id }
                .map { Triple(it.question.trim(), it.answer, it.isSecret) }
                .sortedBy { it.first }
        )
    }.sortedBy { it.first.first }

    @Test
    fun `encrypted backups of random multi-script vaults restore exactly`() = runTest(timeout = 5.minutes) {
        repeat(40) { seed ->
            val rnd = Random(seed.toLong())
            val source = randomVault(rnd)
            val password = rnd.text(24).toCharArray()
            val bytes = BackupCodec(source).encode(password.copyOf(), 1L, "sim")
            val target = FakePasskeyRepository()
            val codec = BackupCodec(target)
            codec.restore(codec.decode(bytes, password.copyOf()).getOrThrow(), ImportMode.MERGE)
            val expected = source.snapshot()
            val actual = target.snapshot()
            expected.zip(actual).firstOrNull { (e, a) -> e != a }?.let { (e, a) ->
                throw AssertionError(
                    "seed $seed first diff\nexpected=${e.toString().map {
                        it.code
                    }}\n  actual=${a.toString().map { it.code }}"
                )
            }
            assertEquals("seed $seed sizes", expected.size, actual.size)
        }
    }

    @Test
    fun `tampered encrypted backups always fail cleanly and never touch the vault`() = runTest(timeout = 5.minutes) {
        val rnd = Random(7)
        val password = "sim password".toCharArray()
        val bytes = BackupCodec(randomVault(rnd)).encode(password.copyOf(), 1L, "sim")
        repeat(30) { seed ->
            val r = Random(seed.toLong())
            val tampered = bytes.copyOf().also { it[r.nextInt(it.size)] = (r.nextInt(256) - 128).toByte() }
            if (tampered.contentEquals(bytes)) return@repeat
            val vault = FakePasskeyRepository()
            val result = BackupCodec(vault).decode(tampered, password.copyOf())
            assertTrue("seed $seed decoded a tampered file", result.isFailure)
            assertTrue("seed $seed", vault.allPreviews.isEmpty())
        }
    }

    @Test
    fun `random legacy CSV input never crashes and never stores oversized or blank fields`() = runTest {
        repeat(2_000) { seed ->
            val rnd = Random(seed.toLong())
            val csv = buildString {
                append("category,heading,question,answer\n")
                repeat(rnd.nextInt(8)) {
                    append(listOf("BANKS", "MAILS", "x", "", "others").random(kotlin.random.Random(seed + it)))
                    repeat(rnd.nextInt(5)) { append(',').append(rnd.text(if (rnd.nextInt(20) == 0) 400 else 20)) }
                    append('\n')
                }
            }
            val vault = FakePasskeyRepository()
            val codec = BackupCodec(vault)
            val parsed = codec.decode(csv.toByteArray(), null).getOrNull() ?: return@repeat
            codec.restore(parsed, ImportMode.MERGE)
            vault.allPreviews.forEach { assertTrue("seed $seed", it.heading.isNotBlank() && it.heading.length <= 256) }
            vault.allDetails.forEach { assertTrue("seed $seed", it.question.isNotBlank() && it.question.length <= 256) }
        }
    }

    @Test
    fun `random lock timelines obey the policy invariants`() {
        repeat(10_000) { seed ->
            val rnd = Random(seed.toLong())
            val timeout = AutoLockTimeout.entries[rnd.nextInt(AutoLockTimeout.entries.size)]
            val wasUnlocked = rnd.nextBoolean()
            val backgroundedAt = rnd.nextInt(1_000_000).toLong()
            val now = backgroundedAt + rnd.nextInt(800_000) - 100_000
            val lock = LockPolicy.lockOnReturn(timeout, wasUnlocked, backgroundedAt, now)
            val away = now - backgroundedAt
            val expected = wasUnlocked && timeout != AutoLockTimeout.NEVER && away >= 0 && away >= timeout.millis
            assertEquals("seed $seed", expected, lock)

            val idle = rnd.nextInt(700_000).toLong()
            val idleLock = LockPolicy.lockWhenIdle(timeout, wasUnlocked, idle)
            if (timeout == AutoLockTimeout.NEVER || timeout == AutoLockTimeout.IMMEDIATELY) {
                assertFalse("seed $seed", idleLock)
            }
        }
    }

    @Test
    fun `random delete, cancel and undo sequences never orphan fields or delete without confirming`() = runTest {
        repeat(300) { seed ->
            val rnd = Random(seed.toLong())
            val repo = randomVault(rnd)
            val original = repo.snapshot()
            val vm = PreviewViewModel(repo, SavedStateHandle(), RecordingAnalyticsTracker())
            val subscriber = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
            var confirmedDeletes = 0
            repeat(rnd.nextInt(15)) {
                val previews = repo.allPreviews
                when (rnd.nextInt(4)) {
                    0 -> previews.randomOrNull(kotlin.random.Random(seed + it))?.let {
                        vm.onIntent(PreviewIntent.SwipedToDelete(it))
                    }
                    1 -> vm.onIntent(PreviewIntent.DeleteCancelled)
                    2 -> if (vm.state.value.pendingDelete != null) {
                        confirmedDeletes++
                        vm.onIntent(PreviewIntent.DeleteConfirmed)
                    }
                    else -> vm.onIntent(PreviewIntent.UndoDelete)
                }
                val ids = repo.allPreviews.map { it.id }.toSet()
                assertTrue("seed $seed orphan", repo.allDetails.all { it.previewId in ids })
            }
            if (confirmedDeletes == 0) assertEquals("seed $seed changed without confirm", original, repo.snapshot())
            subscriber.cancel()
        }
    }

    @Test
    fun `random remote strings never become a navigation target unless allowlisted`() {
        val allowed =
            setOf(
                "settings_page",
                "about_page",
                "legal_page/PRIVACY",
                "legal_page/TERMS",
                "consent_page?fromSettings=true"
            )
        repeat(5_000) { seed ->
            val rnd = Random(seed.toLong())
            val route = if (rnd.nextInt(10) == 0) allowed.random(kotlin.random.Random(seed)) else rnd.text(30)
            val safe = AnnouncementRoutes.safeRoute(route)
            if (safe != null) assertTrue("seed $seed '$route'", route.trim() in allowed)
            assertFalse(PasswordAnalyzer.looksSecret(route, emptySet()))
        }
    }
}
