package com.bhardwaj.passkey.about

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.bhardwaj.passkey.R
import kotlinx.coroutines.flow.Flow

/**
 * Performs the [AboutEffect]s that leave the app (review, links, share, email) and hands navigation effects to
 * the callbacks. Put it next to [AboutScreen] in the app's route.
 */
@Composable
fun AboutEffectsHandler(
    effects: Flow<AboutEffect>,
    state: AboutUiState,
    onReviewUnavailable: () -> Unit,
    onOpenLicences: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val shareText = stringResource(R.string.about_share_text, state.app.appName, state.storeUrl)
    val shareChooser = stringResource(R.string.about_share_chooser)
    val emailSubject = stringResource(R.string.about_contact_subject, state.app.appName, state.app.versionName)
    val reviewUnavailable by rememberUpdatedState(onReviewUnavailable)
    val licences by rememberUpdatedState(onOpenLicences)
    val terms by rememberUpdatedState(onOpenTerms)
    val privacy by rememberUpdatedState(onOpenPrivacy)
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                AboutEffect.LaunchInAppReview -> {
                    val launched = activity?.let { InAppReview.launch(it) } ?: false
                    if (!launched) reviewUnavailable()
                }
                is AboutEffect.OpenUrl -> ExternalLinks.openUrl(context, effect.url)
                is AboutEffect.Share -> ExternalLinks.share(context, shareText, shareChooser)
                is AboutEffect.Email -> ExternalLinks.email(context, effect.address, emailSubject)
                AboutEffect.OpenLicences -> licences()
                AboutEffect.OpenTerms -> terms()
                AboutEffect.OpenPrivacy -> privacy()
            }
        }
    }
}
