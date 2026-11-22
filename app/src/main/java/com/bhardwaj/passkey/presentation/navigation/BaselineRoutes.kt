package com.bhardwaj.passkey.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhardwaj.passkey.about.AboutEffectsHandler
import com.bhardwaj.passkey.about.AboutScreen
import com.bhardwaj.passkey.about.AboutViewModel
import com.bhardwaj.passkey.about.ExternalLinks
import com.bhardwaj.passkey.about.LicencesScreen
import com.bhardwaj.passkey.about.OssLibrary
import com.bhardwaj.passkey.about.baselineLibraries
import com.bhardwaj.passkey.banner.AnnouncementBanner
import com.bhardwaj.passkey.banner.AnnouncementEffect
import com.bhardwaj.passkey.banner.AnnouncementIntent
import com.bhardwaj.passkey.banner.AnnouncementViewModel
import com.bhardwaj.passkey.legal.LegalDocument
import com.bhardwaj.passkey.legal.LegalEffect
import com.bhardwaj.passkey.legal.LegalScreen
import com.bhardwaj.passkey.legal.LegalViewModel

/** Everything PassKey ships besides the baseline set. Keep docs/ASSET_LICENSES.md in step. */
val passKeyLibraries = baselineLibraries + listOf(
    OssLibrary("Dagger Hilt", "Apache License 2.0"),
    OssLibrary("SQLCipher for Android (Community Edition)", "BSD-style licence by Zetetic LLC"),
    OssLibrary("OpenCSV", "Apache License 2.0"),
    OssLibrary("Reorderable (sh.calvin.reorderable)", "Apache License 2.0"),
    OssLibrary("Poppins and Bebas Neue fonts", "SIL Open Font License 1.1")
)

@Composable
fun AboutRoute(onBack: () -> Unit, onOpenLicences: () -> Unit, onOpenLegal: (LegalDocument) -> Unit) {
    val viewModel: AboutViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    AboutEffectsHandler(
        effects = viewModel.effects,
        state = state,
        onReviewUnavailable = {},
        onOpenLicences = onOpenLicences,
        onOpenTerms = { onOpenLegal(LegalDocument.TERMS) },
        onOpenPrivacy = { onOpenLegal(LegalDocument.PRIVACY) }
    )
    AboutScreen(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun LegalRoute(onBack: () -> Unit) {
    val viewModel: LegalViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LegalEffect.OpenUrl -> ExternalLinks.openUrl(context, effect.url)
            }
        }
    }
    LegalScreen(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun LicencesRoute(onBack: () -> Unit) = LicencesScreen(libraries = passKeyLibraries, onBack = onBack)

/** Remote Config announcement card for the vault home. Routes in announcements are app routes. */
@Composable
fun HomeAnnouncement(onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: AnnouncementViewModel = hiltViewModel()
    val announcement by viewModel.announcement.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AnnouncementEffect.OpenUrl -> ExternalLinks.openUrl(context, effect.url)
                is AnnouncementEffect.Navigate -> onNavigate(effect.route)
            }
        }
    }
    AnnouncementBanner(
        announcement = announcement,
        onCta = { viewModel.onIntent(AnnouncementIntent.CtaClicked(it)) },
        onDismiss = { viewModel.onIntent(AnnouncementIntent.Dismissed(it)) },
        modifier = modifier
    )
}
