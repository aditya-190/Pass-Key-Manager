package com.bhardwaj.passkey.backup

import com.bhardwaj.passkey.data.backup.BackupCodec
import com.bhardwaj.passkey.data.backup.BackupCrypto
import com.bhardwaj.passkey.data.backup.BackupError
import com.bhardwaj.passkey.data.backup.BackupException
import com.bhardwaj.passkey.data.backup.BackupFormat
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 5.6.0 attack-pass findings 1 and 2: hostile backups must fail fast and small. */
class BackupHardeningTest {
    private val password = "a shared password".toCharArray()

    private fun gzip(bytes: ByteArray): ByteArray = ByteArrayOutputStream().also { out ->
        GZIPOutputStream(out).use { it.write(bytes) }
    }.toByteArray()

    @Test
    fun `a decompression bomb is rejected instead of exhausting memory`() {
        // Validly encrypted with the password the victim was told, so it passes authentication.
        val bomb = gzip(ByteArray(2 * 1024 * 1024))
        assertTrue("bomb compresses well", bomb.size < 16 * 1024)
        val file = BackupCrypto.encrypt(bomb, password.copyOf())

        val result = BackupCodec(FakePasskeyRepository(), maxDecompressedBytes = 1024 * 1024)
            .decode(file, password.copyOf())

        assertEquals(BackupError.FileTooLarge, (result.exceptionOrNull() as BackupException).error)
    }

    @Test
    fun `the production decompression limit is generous but finite`() {
        assertTrue(BackupCodec.MAX_DECOMPRESSED_BYTES in (16 * 1024 * 1024)..(128 * 1024 * 1024))
    }

    @Test
    fun `a header demanding millions of key-derivation rounds is refused before any work is done`() {
        val header = BackupFormat.write(
            BackupFormat.Header(
                formatVersion = BackupFormat.FORMAT_VERSION,
                kdfId = BackupFormat.KDF_PBKDF2_HMAC_SHA512,
                iterations = 5_000_000,
                memoryKiB = 0,
                parallelism = 0,
                salt = ByteArray(BackupFormat.SALT_BYTES),
                nonce = ByteArray(BackupFormat.NONCE_BYTES),
                ciphertextLength = 32
            )
        )
        val started = System.nanoTime()
        val result = BackupCrypto.decrypt(header + ByteArray(32), password.copyOf())
        val elapsedMs = (System.nanoTime() - started) / 1_000_000

        assertTrue("$result", result is BackupCrypto.DecryptResult.UnsupportedFormat)
        assertTrue("took $elapsedMs ms", elapsedMs < 500)
    }

    @Test
    fun `the iteration cap still accepts every file this app writes`() {
        assertTrue(BackupFormat.DEFAULT_PBKDF2_ITERATIONS <= BackupCrypto.MAX_PBKDF2_ITERATIONS)
        assertTrue(BackupCrypto.MAX_PBKDF2_ITERATIONS <= 2_000_000)
    }
}
