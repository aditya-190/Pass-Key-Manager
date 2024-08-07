package com.bhardwaj.passkey.utils

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bhardwaj.passkey.utils.Constants.Companion.DETAILS_TABLE
import com.bhardwaj.passkey.utils.Constants.Companion.PREVIEW_TABLE
import com.bhardwaj.passkey.utils.Constants.Companion.TEMP_DETAILS_TABLE
import com.bhardwaj.passkey.utils.Constants.Companion.TEMP_PREVIEW_TABLE

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Rename the existing table to a temporary name
        db.execSQL("ALTER TABLE $PREVIEW_TABLE RENAME TO $TEMP_PREVIEW_TABLE")
        db.execSQL("ALTER TABLE $DETAILS_TABLE RENAME TO $TEMP_DETAILS_TABLE")

        db.execSQL(
            """
            CREATE TABLE $PREVIEW_TABLE (
                previewId INTEGER PRIMARY KEY AUTOINCREMENT,
                heading TEXT NOT NULL,
                categoryName TEXT NOT NULL,
                sequence INTEGER NOT NULL DEFAULT 0
            )
        """
        )
        db.execSQL(
            """
            CREATE TABLE $DETAILS_TABLE (
                detailsId INTEGER PRIMARY KEY AUTOINCREMENT,
                previewId INTEGER NOT NULL,
                question TEXT NOT NULL,
                answer TEXT NOT NULL,
                sequence INTEGER NOT NULL DEFAULT 0
            )
        """
        )

        db.execSQL(
            """
            INSERT INTO $PREVIEW_TABLE (previewId, heading, categoryName, sequence)
            SELECT previewId, heading, categoryName, priority FROM $TEMP_PREVIEW_TABLE
        """
        )
        db.execSQL(
            """
            INSERT INTO $DETAILS_TABLE (detailsId, previewId, question, answer, sequence)
            SELECT detailsId, previewId, question, answer, priority FROM $TEMP_DETAILS_TABLE
        """
        )

        db.execSQL("DROP TABLE $TEMP_PREVIEW_TABLE")
        db.execSQL("DROP TABLE $TEMP_DETAILS_TABLE")
    }
}
