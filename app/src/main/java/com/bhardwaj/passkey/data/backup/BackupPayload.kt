package com.bhardwaj.passkey.data.backup

import kotlinx.serialization.Serializable

/**
 * The decrypted contents of a backup.
 *
 * Details are nested under their preview rather than carrying `previewId`, so no install-local
 * row id crosses the file boundary and a restore into a different install cannot collide.
 */
@Serializable
internal data class BackupPayload(
    val schema: Int = SCHEMA_VERSION,
    val exportedAt: Long,
    val appVersion: String,
    val previews: List<BackupPreview>
) {
    companion object {
        /**
         * 2 adds `totp` (`isSecret` has been written since 5.6.0). A v1 file still restores: both are optional
         * with defaults, and a 5.6/5.7 reader is kept out by the *format* version (3) rather than failing on an
         * unknown key and reporting "wrong password".
         */
        const val SCHEMA_VERSION = 2

        /** Guards against a hostile file exhausting memory. */
        const val MAX_PREVIEWS = 10_000
        const val MAX_HEADING_CHARS = 256
        const val MAX_QUESTION_CHARS = 256
        const val MAX_ANSWER_CHARS = 4_096
        const val MAX_SECRET_CHARS = 512
    }
}

@Serializable
internal data class BackupPreview(
    val heading: String,
    val categoryName: String,
    val sequence: Long = 0,
    val details: List<BackupDetail> = emptyList(),
    val totp: List<BackupTotp> = emptyList()
)

@Serializable
internal data class BackupDetail(
    val question: String,
    val answer: String,
    val sequence: Long = 0,
    /**
     * Carried so a restore keeps the user's per-field choice (keyword matching classifies little outside
     * English); defaults keep older files readable.
     */
    val isSecret: Boolean = false
)

/** The Base32 secret, as printed, plus the parameters needed to reproduce the same codes. */
@Serializable
internal data class BackupTotp(
    val label: String,
    val secret: String,
    val issuer: String? = null,
    /** Optional so files written before it was carried still restore; the label stands in for it then. */
    val account: String? = null,
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val periodSeconds: Int = 30
)
