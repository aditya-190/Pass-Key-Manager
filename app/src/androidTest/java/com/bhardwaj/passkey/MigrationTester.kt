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
            assertThat(getLong(getColumnIndex("sequence"))).isEqualTo(0)
        }

        db.query("SELECT * FROM $DETAILS_TABLE").apply {
            assertThat(moveToFirst()).isTrue()
            assertThat(getLong(getColumnIndex("sequence"))).isEqualTo(0)
            assertThat(getLong(getColumnIndex("previewId"))).isEqualTo(null)
        }
    }
}