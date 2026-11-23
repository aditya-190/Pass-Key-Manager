package com.bhardwaj.passkey.data.backup

import android.content.Context
import android.net.Uri
import com.bhardwaj.passkey.BuildConfig
import com.bhardwaj.passkey.domain.repository.PasskeyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** How an import should treat entries already in the vault. */
enum class ImportMode { MERGE, REPLACE_ALL }

data class ImportSummary(val previewsAdded: Int, val detailsAdded: Int, val skipped: Int, val legacyFormat: Boolean)

sealed interface BackupError {
    data object WrongPasswordOrCorrupt : BackupError
    data class UnsupportedVersion(val reason: String) : BackupError
    data object FileTooLarge : BackupError
    data object SchemaInvalid : BackupError
    data object ReadFailed : BackupError
    data object WriteFailed : BackupError
}

@Singleton
class BackupRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    repository: PasskeyRepository
) {
    private val codec = BackupCodec(repository)

    suspend fun export(uri: Uri, password: CharArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val encrypted = codec.encode(password, System.currentTimeMillis(), BuildConfig.VERSION_NAME)
            // "wt" truncates: CreateDocument can hand back an existing file.
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(encrypted) }
                ?: return@withContext Result.failure(BackupException(BackupError.WriteFailed))
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(BackupException(BackupError.WriteFailed))
        }
    }

    /**
     * Writes a backup assembled outside the normal repository path: the migration failure screen reads the
     * un-migrated vault through the legacy key, so a user whose re-key could not complete still gets their
     * data out.
     */
    internal suspend fun exportPreviews(uri: Uri, password: CharArray, previews: List<BackupPreview>): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val encrypted =
                    codec.encodePreviews(previews, password, System.currentTimeMillis(), BuildConfig.VERSION_NAME)
                context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(encrypted) }
                    ?: return@withContext Result.failure(BackupException(BackupError.WriteFailed))
                Result.success(Unit)
            } catch (_: Exception) {
                Result.failure(BackupException(BackupError.WriteFailed))
            }
        }

    suspend fun import(uri: Uri, password: CharArray?, mode: ImportMode): Result<ImportSummary> =
        withContext(Dispatchers.IO) {
            val bytes = readLimited(uri)
                ?: return@withContext Result.failure(BackupException(BackupError.ReadFailed))
            codec.decode(bytes, password).mapCatching { parsed ->
                runCatching { codec.restore(parsed, mode) }
                    .getOrElse { throw BackupException(BackupError.SchemaInvalid) }
            }
        }

    private fun readLimited(uri: Uri): ByteArray? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
                if (total > BackupCodec.MAX_FILE_BYTES) return@use ByteArray(BackupCodec.MAX_FILE_BYTES + 1)
                out.write(buffer, 0, read)
            }
            out.toByteArray()
        }
    }.getOrNull()
}

class BackupException(val error: BackupError) : Exception(error.toString())

/**
 * Never [Category.valueOf]: it throws on unknown input, and the previous importer let that
 * escape into a broad catch that silently abandoned the rest of the file.
 */
