package com.bhardwaj.passkey.about

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Play In-App Review (`com.google.android.play:review` + kotlinx-coroutines-play-services `await`). The `review-ktx`
 * artifact is avoided on purpose: its SAM wrapper references a class R8 reports as missing in release builds.
 */
object InAppReview {
    /**
     * Runs the review flow. False = it couldn't run (no Play Store, error): open the store listing instead.
     * True does NOT mean a dialog appeared (Play enforces a quota and never tells the app).
     */
    suspend fun launch(activity: Activity): Boolean = try {
        val manager = ReviewManagerFactory.create(activity)
        val info = manager.requestReviewFlow().await()
        manager.launchReviewFlow(activity, info).await()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (
        @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception
    ) {
        false
    }
}
