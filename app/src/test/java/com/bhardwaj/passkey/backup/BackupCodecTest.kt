package com.bhardwaj.passkey.backup

import com.bhardwaj.passkey.data.backup.BackupCodec
import com.bhardwaj.passkey.data.backup.BackupError
import com.bhardwaj.passkey.data.backup.BackupException
import com.bhardwaj.passkey.data.backup.ImportMode
import com.bhardwaj.passkey.data.local.entity.Details
import com.bhardwaj.passkey.data.local.entity.Preview
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import com.bhardwaj.passkey.utils.Categories
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The whole backup path minus file I/O: build → encrypt → decrypt → validate → restore. */
class BackupCodecTest {
    private val password = "correct horse battery".toCharArray()

    private suspend fun FakePasskeyRepository.seed(): FakePasskeyRepository = apply {
        val bank = upsertPreview(Preview(heading = "Bank, with comma", categoryName = Categories.BANKS, sequence = 2))
        upsertDetails(Details(previewId = bank, question = "PIN", answer = "4321", sequence = 1))
        upsertDetails(Details(previewId = bank, question = "Note", answer = "line1\nline2 🔐", sequence = 0))
        val mail = upsertPreview(Preview(heading = "Mail", categoryName = Categories.MAILS))
        upsertDetails(Details(previewId = mail, question = "Password", answer = "hunter2"))
    }

    private fun FakePasskeyRepository.snapshot() = allPreviews.sortedBy { it.heading }.map { preview ->
        Triple(
            preview.heading,
            preview.categoryName,
            allDetails.filter {
                it.previewId == preview.previewId
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
        val id = source.upsertPreview(Preview(heading = "Wi-Fi", categoryName = Categories.OTHERS))
        source.upsertDetails(Details(previewId = id, question = "Network", answer = "home", isSecret = false))
        source.upsertDetails(Details(previewId = id, question = "Clave", answer = "x9!", isSecret = true))
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
        assertEquals(Categories.OTHERS, target.allPreviews.single { it.heading == "Odd" }.categoryName)
        assertEquals("1111", target.allDetails.single { it.question == "PIN" }.answer)
    }

    @Test
    fun `merge does not duplicate identical details, replace clears the vault first`() = runTest {
        val vault = FakePasskeyRepository().seed()
        val codec = BackupCodec(vault)
        val bytes = codec.encode(password.copyOf(), 1L, "test")
        vault.upsertPreview(Preview(heading = "Only here", categoryName = Categories.APPS))

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
}
