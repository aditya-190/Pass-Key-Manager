package com.bhardwaj.passkey

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bhardwaj.passkey.data.local.PasskeyDatabase
import com.bhardwaj.passkey.data.local.entity.DetailsEntity
import com.bhardwaj.passkey.data.local.entity.PreviewEntity
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.utils.Constants
import com.bhardwaj.passkey.utils.MIGRATION_1_2
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Not a test: an opt-in helper for the version-upgrade check. It writes one known entry into the app's
 * real encrypted vault, opened exactly as AppModule opens it (same file, same key), so the UI of another
 * app version can then be checked for it.
 *
 * Skipped unless run with `-e seedVault true`, so a normal test run never adds data to a real vault.
 */
@RunWith(AndroidJUnit4::class)
class VaultSeedTool {
    @Test
    fun seedKnownEntry() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("seedVault") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, PasskeyDatabase::class.java, Constants.PASS_KEY_DATABASE)
            .addMigrations(MIGRATION_1_2)
            .openHelperFactory(SupportOpenHelperFactory(BuildConfig.LEGACY_PASS_PHRASE.toByteArray()))
            .build()
        runBlocking {
            val existing = db.previewDao.getPreviews().first().firstOrNull { it.heading == HEADING }
            val previewId = existing?.previewId
                ?: db.previewDao.upsertPreview(PreviewEntity(heading = HEADING, categoryName = Category.BANKS))
            if (db.detailsDao.getDetailsByPreviewId(previewId).first().none { it.question == QUESTION }) {
                db.detailsDao.upsertDetails(DetailsEntity(previewId = previewId, question = QUESTION, answer = ANSWER))
            }
        }
        db.close()
    }

    companion object {
        const val HEADING = "E2E Upgrade Bank"
        const val QUESTION = "E2E Password"
        const val ANSWER = "Upgrade-Check-42"
    }
}
