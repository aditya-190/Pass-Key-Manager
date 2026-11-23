package com.bhardwaj.passkey.utils

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bhardwaj.passkey.utils.Constants.Companion.AUTOFILL_TRUST_TABLE

/**
 * Adds the autofill trust-on-first-use table. Additive and idempotent: an interrupted run leaves nothing half-done,
 * and a retry is a no-op. The statement matches Room's exported 5.json exactly, or the identity check fails on open.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `$AUTOFILL_TRUST_TABLE` (" +
                "`packageName` TEXT NOT NULL, `signer` TEXT NOT NULL, `firstSeen` INTEGER NOT NULL, " +
                "PRIMARY KEY(`packageName`))"
        )
    }
}
