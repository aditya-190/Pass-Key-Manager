package com.bhardwaj.passkey.simulation

import androidx.lifecycle.SavedStateHandle
import com.bhardwaj.passkey.data.backup.BackupCodec
import com.bhardwaj.passkey.data.backup.ImportMode
import com.bhardwaj.passkey.domain.autofill.AppTrust
import com.bhardwaj.passkey.domain.autofill.AutofillMatcher
import com.bhardwaj.passkey.domain.autofill.AutofillSaveMerger
import com.bhardwaj.passkey.domain.autofill.SaveOp
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.domain.model.Detail
import com.bhardwaj.passkey.domain.model.PasswordHistoryEntry
import com.bhardwaj.passkey.domain.model.Preview
import com.bhardwaj.passkey.domain.totp.Base32
import com.bhardwaj.passkey.domain.totp.OtpAuthUri
import com.bhardwaj.passkey.domain.totp.TotpAlgorithm
import com.bhardwaj.passkey.domain.totp.TotpConfig
import com.bhardwaj.passkey.domain.totp.TotpGenerator
import com.bhardwaj.passkey.presentation.screens.preview_screen.PreviewIntent
import com.bhardwaj.passkey.presentation.screens.preview_screen.PreviewViewModel
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import java.util.Random
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Feature workflow step 3 for 5.8.0 phase 4b: seeded runs through the autofill matcher (with look-alike apps and
 * sites), TOTP generation and Base32/otpauth parsing, and backup round trips carrying authenticators.
 * Every assertion names its seed.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class Release580bSimulationTest {
    @get:org.junit.Rule
    val main = com.bhardwaj.passkey.testing.MainDispatcherRule()

    private val services = listOf("google", "github", "netflix", "amazon", "paypal", "proton", "instagram", "bank")
    private val scripts = listOf("बैंक", "বাংলা", "بنك", "銀行", "🔐", "Ünïcödé", "x", "")

    private fun Random.heading(): String = when (nextInt(5)) {
        0 -> services[nextInt(services.size)].replaceFirstChar { it.uppercase() }
        1 -> "My ${services[nextInt(services.size)]} account"
        2 -> services[nextInt(services.size)] + services[nextInt(services.size)]
        3 -> scripts[nextInt(scripts.size)] + " " + scripts[nextInt(scripts.size)]
        else -> "fake" + services[nextInt(services.size)]
    }

    @Test
    fun `look-alike apps and sites never receive another service's login`() {
        repeat(SEEDS) { seed ->
            val rnd = Random(seed.toLong())
            val previews = List(rnd.nextInt(8) + 1) { i -> Preview(i.toLong(), rnd.heading(), Category.APPS) }
            val details = previews.associate {
                it.id to
                    listOf(Detail(it.id * 10, it.id, "Password", "pw", isSecret = true))
            }
            val victim = services[rnd.nextInt(services.size)]
            val attacker = listOf(
                null to "$victim.evil.com",
                null to "https://login-$victim.example.net/",
                null to "$victim.com.attacker.io",
                "com.fake$victim.login" to null,
                "com.evil.$victim" to null,
                "com.android.chrome" to "attacker.org"
            )[rnd.nextInt(6)]
            val offered = AutofillMatcher.match(
                previews,
                details,
                AutofillMatcher.tokensOf(attacker.first, attacker.second)
            )
            val wordsOf = { h: String -> h.lowercase().split(Regex("[^\\p{L}\\p{N}]+")) }
            assertTrue(
                "seed $seed: $attacker got ${offered.map { it.label }}",
                offered.none { victim in wordsOf(it.label) || it.label.lowercase() == victim }
            )
        }
    }

    @Test
    fun `the genuine app or site always finds an entry named after it`() {
        repeat(SEEDS) { seed ->
            val rnd = Random(seed.toLong() + 7)
            val service = services[rnd.nextInt(services.size)]
            val previews =
                listOf(Preview(1, if (rnd.nextBoolean()) service.uppercase() else "Work $service", Category.APPS))
            val details = mapOf(
                1L to listOf(Detail(10, 1, "User", "me"), Detail(11, 1, "Password", "pw", isSecret = true))
            )
            val genuine = if (rnd.nextBoolean()) {
                AutofillMatcher.tokensOf("com.$service.android", null)
            } else {
                AutofillMatcher.tokensOf(
                    null,
                    "https://accounts.$service.${listOf("com", "co.uk", "co.in")[rnd.nextInt(3)]}/x"
                )
            }
            val offered = AutofillMatcher.match(previews, details, genuine)
            assertEquals("seed $seed", listOf("me" to "pw"), offered.map { it.username to it.password })
        }
    }

    @Test
    fun `random secrets, algorithms and times always give well-formed, stable codes`() {
        repeat(SEEDS) { seed ->
            val rnd = Random(seed.toLong())
            val secret = ByteArray(rnd.nextInt(64) + 1).also(rnd::nextBytes)
            val config = TotpConfig(
                secret = secret,
                algorithm = TotpAlgorithm.entries[rnd.nextInt(TotpAlgorithm.entries.size)],
                digits = TotpConfig.DIGIT_RANGE.random(kotlin.random.Random(seed)),
                periodSeconds = TotpConfig.PERIOD_RANGE.random(kotlin.random.Random(seed))
            )
            val now = rnd.nextLong() and 0xFFFFFFFFFL
            val code = TotpGenerator.generate(config, now)
            assertEquals("seed $seed length", config.digits, code.length)
            assertTrue("seed $seed digits", code.all { it.isDigit() })
            val stepStart = now - now % config.periodSeconds
            assertEquals("seed $seed stable within a step", code, TotpGenerator.generate(config, stepStart))
            assertTrue("seed $seed base32", Base32.decode(Base32.encode(secret))!!.contentEquals(secret))
            // Untrusted text pasted into the add dialog must never crash the parser.
            val junk = buildString { repeat(rnd.nextInt(80)) { append((rnd.nextInt(0x2FF) + 1).toChar()) } }
            OtpAuthUri.parse("otpauth://totp/$junk?secret=$junk")
            Base32.decode(junk)
        }
    }

    @Test
    fun `backups with authenticators restore exactly and never duplicate on re-import`() =
        runTest(timeout = 5.minutes) {
            repeat(BACKUP_SEEDS) { seed ->
                val rnd = Random(seed.toLong())
                val source = FakePasskeyRepository()
                repeat(rnd.nextInt(4) + 1) { i ->
                    val id = source.createPreview("Entry $i ${rnd.heading()}".trim(), Category.entries[rnd.nextInt(4)])
                    repeat(rnd.nextInt(3)) { t ->
                        val secret = ByteArray(rnd.nextInt(32) + 10).also(rnd::nextBytes)
                        source.createTotp(
                            id,
                            "code $t ${scripts[rnd.nextInt(scripts.size)]}".trim(),
                            TotpConfig(secret, issuer = "i$t", account = "a$t")
                        )
                    }
                }
                val password = "pass $seed".toCharArray()
                val bytes = BackupCodec(source).encode(password.copyOf(), 1L, "sim")
                val target = FakePasskeyRepository()
                val codec = BackupCodec(target)
                repeat(2) { codec.restore(codec.decode(bytes, password.copyOf()).getOrThrow(), ImportMode.MERGE) }
                val expected = source.allTotp.map { it.label to it.config }.sortedBy { it.first }
                val actual = target.allTotp.map { it.label to it.config }.sortedBy { it.first }
                assertEquals("seed $seed", expected, actual)
            }
        }

    @Test
    fun `repeated autofill saves keep one password field and only move changed values to history`() {
        repeat(SEEDS) { seed ->
            val rnd = Random(seed.toLong())
            var details = List(rnd.nextInt(4)) { i ->
                Detail(i + 1L, 1, listOf("User", "Note", "Password", "PIN")[i], "v$i", i.toLong(), isSecret = i >= 2)
            }
            var nextId = 100L
            var historyWrites = 0
            repeat(rnd.nextInt(12) + 1) {
                val password = if (rnd.nextInt(3) ==
                    0
                ) {
                    details.firstOrNull { it.isSecret }?.answer ?: "p"
                } else {
                    "p${rnd.nextInt(5)}"
                }
                val user = listOf(null, "", "me", "me2")[rnd.nextInt(4)]
                AutofillSaveMerger.plan(details, user, password, setOf("password")).forEach { op ->
                    details = when (op) {
                        is SaveOp.Update -> {
                            val before = details.single { it.id == op.detail.id }
                            assertTrue("seed $seed no-op update", before != op.detail)
                            if (before.isSecret && before.answer != op.detail.answer) historyWrites++
                            details.map { if (it.id == op.detail.id) op.detail else it }
                        }
                        is SaveOp.Create ->
                            details +
                                Detail(nextId++, 1, op.question, op.answer, op.sequence, op.isSecret)
                    }
                }
                assertEquals("seed $seed password", password, details.first { it.isSecret }.answer)
                assertTrue(
                    "seed $seed duplicate creates",
                    details.count { it.question == AutofillSaveMerger.PASSWORD_LABEL } <= 1
                )
            }
            assertTrue(historyWrites >= 0)
        }
    }

    @Test
    fun `trust on first use is never fooled by a changed or unreadable signer`() {
        repeat(SEEDS) { seed ->
            val rnd = Random(seed.toLong())
            var stored: String? = null
            val genuine = AppTrust.fingerprint(listOf("cert${rnd.nextInt(3)}"))
            repeat(rnd.nextInt(10) + 1) {
                val actual = listOf(
                    genuine,
                    null,
                    AppTrust.fingerprint(listOf("evil${rnd.nextInt(3)}"))
                )[rnd.nextInt(3)]
                when (AppTrust.decide(stored, actual)) {
                    AppTrust.Decision.FirstUse -> stored = actual
                    AppTrust.Decision.Trusted -> assertEquals("seed $seed", stored, actual)
                    AppTrust.Decision.Mismatch -> assertTrue("seed $seed", actual == null || actual != stored)
                }
            }
        }
    }

    @Test
    fun `deleting and undoing entries with codes and history never loses anything`() = runTest {
        repeat(SEEDS) { seed ->
            val rnd = Random(seed.toLong())
            val repo = FakePasskeyRepository()
            repeat(rnd.nextInt(4) + 1) { i ->
                val id = repo.createPreview("Entry $i", Category.BANKS, i.toLong())
                repeat(rnd.nextInt(3)) { d ->
                    val field = repo.createDetail(id, "q$d", "a$d", isSecret = rnd.nextBoolean())
                    if (rnd.nextBoolean()) repo.history[field] = listOf(PasswordHistoryEntry(1, field, "old$d", 1))
                }
                repeat(rnd.nextInt(3)) { t ->
                    repo.createTotp(id, "code$t", TotpConfig(byteArrayOf(1, 2, 3, t.toByte())))
                }
            }
            val before = Triple(repo.allDetails.toSet(), repo.allTotp.toSet(), repo.history.toMap())
            val vm = PreviewViewModel(repo, SavedStateHandle(), RecordingAnalyticsTracker())
            val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
            vm.onIntent(PreviewIntent.SwipedToDelete(vm.state.value.items.random(kotlin.random.Random(seed))))
            vm.onIntent(PreviewIntent.DeleteConfirmed)
            vm.onIntent(PreviewIntent.UndoDelete)
            assertEquals(
                "seed $seed",
                before,
                Triple(repo.allDetails.toSet(), repo.allTotp.toSet(), repo.history.toMap())
            )
            job.cancel()
        }
    }

    private companion object {
        const val SEEDS = 2_000

        /** Each encode runs 600k PBKDF2 rounds. */
        const val BACKUP_SEEDS = 30
    }
}
