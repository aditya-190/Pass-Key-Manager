package com.bhardwaj.passkey.review

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.bhardwaj.passkey.about.InAppReview
import kotlinx.coroutines.launch

/**
 * Starts Play's in-app review on the activity's own scope, so it survives the screen that asked (the tour closes
 * straight after). Unsolicited, so there is no store fallback when Play can't show it.
 */
fun requestInAppReview(activity: Activity?) {
    val host = activity as? ComponentActivity ?: return
    host.lifecycleScope.launch { InAppReview.launch(host) }
}
