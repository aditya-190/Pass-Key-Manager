package com.bhardwaj.passkey.backup

import com.bhardwaj.passkey.data.backup.BackupCodec
import com.bhardwaj.passkey.data.backup.BackupError
import com.bhardwaj.passkey.data.backup.BackupException
import com.bhardwaj.passkey.data.backup.BackupTotp
import com.bhardwaj.passkey.data.backup.ImportMode
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.domain.totp.Base32
import com.bhardwaj.passkey.domain.totp.TotpConfig
import com.bhardwaj.passkey.domain.totp.TotpGenerator
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The whole backup path minus file I/O: build → encrypt → decrypt → validate → restore. */
class BackupCodecTest {
    private val password = "correct horse battery".toCharArray()

    private suspend fun FakePasskeyRepository.seed(): FakePasskeyRepository = apply {
        val bank = createPreview(heading = "Bank, with comma", category = Category.BANKS, sequence = 2)
        createDetail(previewId = bank, question = "PIN", answer = "4321", sequence = 1)
        createDetail(previewId = bank, question = "Note", answer = "line1\nline2 🔐", sequence = 0)
        val mail = createPreview(heading = "Mail", category = Category.MAILS)
        createDetail(previewId = mail, question = "Password", answer = "hunter2")
    }

    private fun FakePasskeyRepository.snapshot() = allPreviews.sortedBy { it.heading }.map { preview ->
        Triple(
            preview.heading,
            preview.category,
            allDetails.filter {
                it.previewId == preview.id
            }.map { it.question to it.answer }.sortedBy { it.first }
        )
    }

    private fun failure(result: Result<*>): BackupError = (result.exceptionOrNull() as BackupException).error

    @Test
    fun `an encrypted backup restores every entry exactly into an empty vault`() = runTest {
        val source = FakePasskeyRepository().seed()
        val bytes = BackupCodec(source).encode(password.copyOf(), exportedAt = 1L, appVersion = "test")

        val target = FakePasskeyRepository()
        val codec = BackupCodec(target)
        val summary = codec.restore(codec.decode(bytes, password.copyOf()).getOrThrow(), ImportMode.MERGE)

        assertEquals(source.snapshot(), target.snapshot())
        assertEquals(2, summary.previewsAdded)
        assertEquals(3, summary.detailsAdded)
        assertEquals(false, summary.legacyFormat)
    }

    @Test
    fun `the secret flag survives a backup round trip`() = runTest {
        val source = FakePasskeyRepository()
        val id = source.createPreview(heading = "Wi-Fi", category = Category.OTHERS)
        source.createDetail(previewId = id, question = "Network", answer = "home", isSecret = false)
        source.createDetail(previewId = id, question = "Clave", answer = "x9!", isSecret = true)
        val bytes = BackupCodec(source).encode(password.copyOf(), 1L, "test")

        val target = FakePasskeyRepository()
        val codec = BackupCodec(target)
        codec.restore(codec.decode(bytes, password.copyOf()).getOrThrow(), ImportMode.MERGE)

        assertEquals(true, target.allDetails.single { it.question == "Clave" }.isSecret)
        assertEquals(false, target.allDetails.single { it.question == "Network" }.isSecret)
    }

    @Test
    fun `the file is not readable as plaintext`() = runTest {
        val bytes = BackupCodec(FakePasskeyRepository().seed()).encode(password.copyOf(), 1L, "test")
        val asText = bytes.toString(Charsets.ISO_8859_1)
        listOf("hunter2", "4321", "Bank", "PIN").forEach { assertTrue(it, it !in asText) }
    }

    @Test
    fun `wrong password, tampering and a missing password all fail the same way`() = runTest {
        val codec = BackupCodec(FakePasskeyRepository().seed())
        val bytes = codec.encode(password.copyOf(), 1L, "test")
        val tampered = bytes.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }

        assertEquals(BackupError.WrongPasswordOrCorrupt, failure(codec.decode(bytes, "wrong".toCharArray())))
        assertEquals(BackupError.WrongPasswordOrCorrupt, failure(codec.decode(tampered, password.copyOf())))
        assertEquals(BackupError.WrongPasswordOrCorrupt, failure(codec.decode(bytes, null)))
    }

    @Test
    fun `legacy plaintext CSV still restores and counts bad rows`() = runTest {
        val csv = "category,heading,question,answer\nBANKS,Old Bank,PIN,1111\nnot,enough\nWAT,Odd,Q,A\n"
        val target = FakePasskeyRepository()
        val codec = BackupCodec(target)
        val summary = codec.restore(codec.decode(csv.toByteArray(), password = null).getOrThrow(), ImportMode.MERGE)

        assertEquals(true, summary.legacyFormat)
        assertEquals(1, summary.skipped)
        // Unknown categories land in Others instead of aborting the whole import.
        assertEquals(Category.OTHERS, target.allPreviews.single { it.heading == "Odd" }.category)
        assertEquals("1111", target.allDetails.single { it.question == "PIN" }.answer)
    }

    @Test
    fun `merge does not duplicate identical details, replace clears the vault first`() = runTest {
        val vault = FakePasskeyRepository().seed()
        val codec = BackupCodec(vault)
        val bytes = codec.encode(password.copyOf(), 1L, "test")
        vault.createPreview(heading = "Only here", category = Category.APPS)

        val merged = codec.restore(codec.decode(bytes, password.copyOf()).getOrThrow(), ImportMode.MERGE)
        assertEquals(0, merged.detailsAdded)
        assertEquals(3, vault.allPreviews.size)

        codec.restore(codec.decode(bytes, password.copyOf()).getOrThrow(), ImportMode.REPLACE_ALL)
        assertEquals(listOf("Bank, with comma", "Mail"), vault.allPreviews.map { it.heading }.sorted())
        assertEquals(3, vault.allDetails.size)
    }

    @Test
    fun `oversized fields are skipped, not truncated or crashed on`() = runTest {
        val csv = "category,heading,question,answer\nBANKS,${"h".repeat(300)},Q,A\nBANKS,Fine,${"q".repeat(300)},A\n"
        val target = FakePasskeyRepository()
        val codec = BackupCodec(target)
        val summary = codec.restore(codec.decode(csv.toByteArray(), null).getOrThrow(), ImportMode.MERGE)
        assertEquals(2, summary.skipped)
        assertEquals(0, summary.detailsAdded)
    }

    @Test
    fun `non-backup bytes are rejected without touching the vault`() = runTest {
        val vault = FakePasskeyRepository().seed()
        val before = vault.snapshot()
        val result = BackupCodec(vault).decode(ByteArray(64) { 7 }, password.copyOf())
        val restoredNothing = result.isFailure || result.getOrThrow().previews.isEmpty()
        assertTrue(restoredNothing)
        assertEquals(before, vault.snapshot())
    }

    @Test
    fun `authenticator codes survive a backup and a second import does not list them twice`() = runTest {
        val source = FakePasskeyRepository()
        val id = source.createPreview("GitHub", Category.APPS)
        val config = TotpConfig(secret = Base32.decode("JBSWY3DPEHPK3PXP")!!, issuer = "GitHub", account = "me")
        source.createTotp(id, "me@github", config)
        val bytes = BackupCodec(source).encode(password.copyOf(), 1L, "test")

        val target = FakePasskeyRepository()
        val codec = BackupCodec(target)
        repeat(2) { codec.restore(codec.decode(bytes, password.copyOf()).getOrThrow(), ImportMode.MERGE) }

        val restored = target.allTotp.single()
        assertEquals("me@github", restored.label)
        assertEquals(config, restored.config)
        assertEquals(TotpGenerator.generate(config, 59), TotpGenerator.generate(restored.config, 59))
    }

    @Test
    fun `an undecodable authenticator secret is skipped and counted, not stored`() = runTest {
        val source = FakePasskeyRepository()
        val id = source.createPreview("Bank", Category.BANKS)
        source.createDetail(id, "PIN", "1234")
        val bytes = BackupCodec(source).encode(password.copyOf(), 1L, "test")
        val payload = BackupCodec(source).decode(bytes, password.copyOf()).getOrThrow()
        val tampered = payload.copy(
            previews = payload.previews.map {
                it.copy(totp = listOf(BackupTotp(label = "x", secret = "!!not base32!!")))
            }
        )
        val target = FakePasskeyRepository()
        val summary = BackupCodec(target).restore(tampered, ImportMode.MERGE)
        assertTrue(target.allTotp.isEmpty())
        assertEquals(1, summary.skipped)
    }
}
