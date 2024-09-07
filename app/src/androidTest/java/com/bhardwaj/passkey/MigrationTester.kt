package com.bhardwaj.passkey

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bhardwaj.passkey.data.local.PassKeyDatabase
import com.bhardwaj.passkey.utils.Constants.Companion.DETAILS_TABLE
import com.bhardwaj.passkey.utils.Constants.Companion.PREVIEW_TABLE
import com.bhardwaj.passkey.utils.MIGRATION_1_2
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val DB_NAME = "test"

@RunWith(AndroidJUnit4::class)
class MigrationTester {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PassKeyDatabase::class.java,
        listOf(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration_checker_from_1_to_2() {
        var db = helper.createDatabase(DB_NAME, 1)

        db.execSQL("INSERT INTO $PREVIEW_TABLE (heading, categoryName, priority) VALUES ('Sample Heading', 'BANKS', 1)")
        db.execSQL("INSERT INTO $DETAILS_TABLE (question, answer, priority, headingName, categoryName) VALUES ('Sample Question', 'Sample Answer', 1, 'Sample Heading', 'BANKS')")
        db.close()

        db = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)

        db.query("SELECT * FROM $PREVIEW_TABLE").apply {
            assertThat(moveToFirst()).isTrue()
            assertThat(getString(getColumnIndex("heading"))).isEqualTo("Sample Heading")
            assertThat(getString(getColumnIndex("categoryName"))).isEqualTo("BANKS")
            assertThat(getInt(getColumnIndex("sequence"))).isEqualTo(1)
        }

        db.query("SELECT * FROM $DETAILS_TABLE").apply {
            assertThat(moveToFirst()).isTrue()
            assertThat(getString(getColumnIndex("question"))).isEqualTo("Sample Question")
            assertThat(getString(getColumnIndex("answer"))).isEqualTo("Sample Answer")
            assertThat(getInt(getColumnIndex("sequence"))).isEqualTo(1)
            assertThat(getLong(getColumnIndex("previewId"))).isNotNull()
        }
    }

    @Test
    fun migration_with_empty_tables() {
        var db = helper.createDatabase(DB_NAME, 1)
        db.close()

        db = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)

        db.query("SELECT * FROM $PREVIEW_TABLE").apply {
            assertThat(moveToFirst()).isFalse()
        }

        db.query("SELECT * FROM $DETAILS_TABLE").apply {
            assertThat(moveToFirst()).isFalse()
        }
    }

    @Test
    fun migration_with_multiple_rows() {
        var db = helper.createDatabase(DB_NAME, 1)

        for (i in 1..5) {
            db.execSQL("INSERT INTO $PREVIEW_TABLE (heading, categoryName, priority) VALUES ('Heading $i', 'Category $i', $i)")
            db.execSQL("INSERT INTO $DETAILS_TABLE (question, answer, priority, headingName, categoryName) VALUES ('Question $i', 'Answer $i', $i, 'Heading $i', 'Category $i')")
        }
        db.close()

        db = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)

        db.query("SELECT * FROM $PREVIEW_TABLE").apply {
            assertThat(count).isEqualTo(5)
        }

        db.query("SELECT * FROM $DETAILS_TABLE").apply {
            assertThat(count).isEqualTo(5)
            while (moveToNext()) {
                assertThat(getLong(getColumnIndex("previewId"))).isNotNull()
            }
        }
    }
}