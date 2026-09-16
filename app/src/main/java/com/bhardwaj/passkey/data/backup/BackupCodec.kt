package com.bhardwaj.passkey.data.backup

import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.domain.repository.PasskeyRepository
import java.io.ByteArrayOutputStream
import java.util.Arrays
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/** A decoded backup, validated and ready to restore. */
internal data class ParsedBackup(val previews: List<BackupPreview>, val skipped: Int, val legacyFormat: Boolean)

/**
 * The backup format with no Android I/O: vault → encrypted bytes, and bytes → validated entries → vault.
 * [BackupRepository] only adds the file reading and writing.
 */
internal class BackupCodec(
    private val repository: PasskeyRepository,
    private val maxDecompressedBytes: Int = MAX_DECOMPRESSED_BYTES
) {
    private val json = Json { ignoreUnknownKeys = false }

    suspend fun encode(password: CharArray, exportedAt: Long, appVersion: String): ByteArray {
        val previews = repository.getPreviews().first()
        return encodePreviews(
            previews = previews.map { preview ->
                val details = repository.getDetailsByPreviewId(preview.id).first()
                BackupPreview(
                    heading = preview.heading,
                    categoryName = preview.category.name,
                    sequence = preview.sequence,
                    details = details.map { BackupDetail(it.question, it.answer, it.sequence, it.isSecret) }
                )
            },
            password = password,
            exportedAt = exportedAt,
            appVersion = appVersion
        )
    }

    /** Encrypts already-assembled entries; also used by the migration rescue export. */
    fun encodePreviews(
        previews: List<BackupPreview>,
        password: CharArray,
        exportedAt: Long,
        appVersion: String
    ): ByteArray {
        val payload = BackupPayload(exportedAt = exportedAt, appVersion = appVersion, previews = previews)
        val plaintext = gzip(json.encodeToString(payload).toByteArray(Charsets.UTF_8))
        return try {
            BackupCrypto.encrypt(plaintext, password)
        } finally {
            Arrays.fill(plaintext, 0)
        }
    }

    /**
     * Format is detected by magic bytes, not the file name: the old importer matched a name ending in
     * "passkey" and would have rejected every .pkbak. A wrong password and a corrupt file are deliberately
     * indistinguishable, since GCM cannot tell them apart.
     */
    fun decode(bytes: ByteArray, password: CharArray?): Result<ParsedBackup> = when {
        bytes.size > MAX_FILE_BYTES -> failure(BackupError.FileTooLarge)
        BackupFormat.hasMagic(bytes) -> decodeEncrypted(bytes, password)
        bytes.size > MAX_LEGACY_CHARS -> failure(BackupError.FileTooLarge)
        else -> LegacyCsvBackup.parse(bytes.toString(Charsets.UTF_8)).let {
            Result.success(ParsedBackup(it.previews, it.skippedRows, legacyFormat = true))
        }
    }

    private fun decodeEncrypted(bytes: ByteArray, password: CharArray?): Result<ParsedBackup> {
        if (password == null) return failure(BackupError.WrongPasswordOrCorrupt)
        return when (val result = BackupCrypto.decrypt(bytes, password)) {
            BackupCrypto.DecryptResult.WrongPasswordOrCorrupt -> failure(BackupError.WrongPasswordOrCorrupt)
            is BackupCrypto.DecryptResult.UnsupportedFormat -> failure(BackupError.UnsupportedVersion(result.reason))
            is BackupCrypto.DecryptResult.Success -> {
                val payload = try {
                    val json = gunzipBounded(result.plaintext) ?: return failure(BackupError.FileTooLarge)
                    runCatching { this.json.decodeFromString<BackupPayload>(json.toString(Charsets.UTF_8)) }.getOrNull()
                } finally {
                    // Wiped on every path, including a payload that fails to parse.
                    Arrays.fill(result.plaintext, 0)
                }
                val valid = payload != null &&
                    payload.schema <= BackupPayload.SCHEMA_VERSION &&
                    payload.previews.size <= BackupPayload.MAX_PREVIEWS
                if (valid) {
                    Result.success(ParsedBackup(payload.previews, skipped = 0, legacyFormat = false))
                } else {
                    failure(BackupError.SchemaInvalid)
                }
            }
        }
    }

    /** One transaction: a bad row part-way through must not leave a half-restored vault. */
    suspend fun restore(parsed: ParsedBackup, mode: ImportMode): ImportSummary {
        val counts = Counts(skipped = parsed.skipped)
        repository.runInTransaction {
            if (mode == ImportMode.REPLACE_ALL) repository.deleteAll()
            parsed.previews.forEach { restorePreview(it, counts) }
        }
        return ImportSummary(
            previewsAdded = counts.previews,
            detailsAdded = counts.details,
            skipped = counts.skipped,
            legacyFormat = parsed.legacyFormat
        )
    }

    private class Counts(var previews: Int = 0, var details: Int = 0, var skipped: Int = 0)

    private suspend fun restorePreview(backupPreview: BackupPreview, counts: Counts) {
        val heading = backupPreview.heading.trim()
        if (heading.isEmpty() || heading.length > BackupPayload.MAX_HEADING_CHARS) {
            counts.skipped++
            return
        }
        val category = Category.fromNameOrOther(backupPreview.categoryName)
        val previewId = repository.getPreviewByHeading(heading, category)?.id
            ?: repository.createPreview(heading = heading, category = category, sequence = backupPreview.sequence)
                .also { counts.previews++ }

        backupPreview.details.forEach { detail ->
            val question = detail.question.trim()
            val answer = detail.answer
            val tooLong = question.length > BackupPayload.MAX_QUESTION_CHARS ||
                answer.length > BackupPayload.MAX_ANSWER_CHARS
            when {
                question.isEmpty() || tooLong -> counts.skipped++
                repository.getDetailByContent(previewId, question, answer) == null -> {
                    repository.createDetail(
                        previewId = previewId,
                        question = question,
                        answer = answer,
                        sequence = detail.sequence,
                        isSecret = detail.isSecret
                    )
                    counts.details++
                }
            }
        }
    }

    private fun failure(error: BackupError): Result<ParsedBackup> = Result.failure(BackupException(error))

    private fun gzip(bytes: ByteArray): ByteArray = ByteArrayOutputStream().also { out ->
        GZIPOutputStream(out).use { it.write(bytes) }
    }.toByteArray()

    /** Null when the data expands beyond [maxDecompressedBytes] or is not valid gzip. */
    private fun gunzipBounded(bytes: ByteArray): ByteArray? = runCatching {
        GZIPInputStream(bytes.inputStream()).use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (out.size() + read > maxDecompressedBytes) return null
                out.write(buffer, 0, read)
            }
            out.toByteArray()
        }
    }.getOrElse { ByteArray(0) }

    companion object {
        /** A vault large enough to exceed this is not plausible; a hostile file is. */
        const val MAX_FILE_BYTES = 32 * 1024 * 1024
        const val MAX_LEGACY_CHARS = 8 * 1024 * 1024
        private const val BUFFER_BYTES = 64 * 1024

        /** A real vault is a few MB of JSON; this stops a small gzip bomb from exhausting memory. */
        const val MAX_DECOMPRESSED_BYTES = 64 * 1024 * 1024
    }
}
