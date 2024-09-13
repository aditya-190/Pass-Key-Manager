package com.bhardwaj.passkey.utils

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bhardwaj.passkey.utils.Constants.Companion.DETAILS_TABLE
import com.bhardwaj.passkey.utils.Constants.Companion.PREVIEW_TABLE
import com.bhardwaj.passkey.utils.Constants.Companion.TEMP_DETAILS_TABLE
import com.bhardwaj.passkey.utils.Constants.Companion.TEMP_PREVIEW_TABLE

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Rename the existing table to a temporary name.
        db.execSQL("ALTER TABLE $PREVIEW_TABLE RENAME TO $TEMP_PREVIEW_TABLE")
        db.execSQL("ALTER TABLE $DETAILS_TABLE RENAME TO $TEMP_DETAILS_TABLE")

        // 2. Create the new tables with the updated schema.
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

        // 3. Insert data from the temporary tables into the new tables.
        db.execSQL(
            """
            INSERT INTO $PREVIEW_TABLE (previewId, heading, categoryName, sequence)
            SELECT previewId, heading, categoryName, priority FROM $TEMP_PREVIEW_TABLE
        """
        )
        db.execSQL(
            """
            INSERT INTO $DETAILS_TABLE (detailsId, previewId, question, answer, sequence)
            SELECT temp.detailsId, new.previewId, temp.question, temp.answer, temp.priority
            FROM $TEMP_DETAILS_TABLE temp
            JOIN $PREVIEW_TABLE new ON temp.headingName = new.heading AND temp.categoryName = new.categoryName
        """
        )

        // 4. Drop the temporary tables.
        db.execSQL("DROP TABLE $TEMP_PREVIEW_TABLE")
        db.execSQL("DROP TABLE $TEMP_DETAILS_TABLE")
    }
}
