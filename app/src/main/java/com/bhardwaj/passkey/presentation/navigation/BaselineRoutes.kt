package com.bhardwaj.passkey.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.about.AboutEffectsHandler
import com.bhardwaj.passkey.about.AboutScreen
import com.bhardwaj.passkey.about.AboutViewModel
import com.bhardwaj.passkey.about.ExternalLinks
import com.bhardwaj.passkey.about.LicencesScreen
import com.bhardwaj.passkey.about.OssLibrary
import com.bhardwaj.passkey.about.baselineLibraries
import com.bhardwaj.passkey.backupreminder.BackupReminderIntent
import com.bhardwaj.passkey.backupreminder.BackupReminderViewModel
import com.bhardwaj.passkey.banner.Announcement
import com.bhardwaj.passkey.banner.AnnouncementBanner
import com.bhardwaj.passkey.banner.AnnouncementEffect
import com.bhardwaj.passkey.banner.AnnouncementIntent
import com.bhardwaj.passkey.banner.AnnouncementRoutes
import com.bhardwaj.passkey.banner.AnnouncementStyle
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
fun HomeAnnouncement(onNavigate: (NavRoute) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: AnnouncementViewModel = hiltViewModel()
    val announcement by viewModel.announcement.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AnnouncementEffect.OpenUrl -> ExternalLinks.openUrl(context, effect.url)
                is AnnouncementEffect.Navigate -> AnnouncementRoutes.safeRoute(effect.route)?.let(onNavigate)
            }
        }
    }
    // The backup reminder (6.0.0) uses the same card. It shows under a live announcement rather than waiting
    // for it to end: a long announcement must not hold back a missing backup.
    val reminderViewModel: BackupReminderViewModel = hiltViewModel()
    val reminderDue by reminderViewModel.isDue.collectAsStateWithLifecycle()
    val reminder = if (reminderDue) {
        Announcement(
            id = BACKUP_REMINDER_ID,
            title = stringResource(R.string.backup_reminder_title),
            body = stringResource(R.string.backup_reminder_body),
            ctaLabel = stringResource(R.string.backup_reminder_cta),
            route = BACKUP_REMINDER_ROUTE,
            style = AnnouncementStyle.INFO
        )
    } else {
        null
    }
    Column(modifier) {
        AnnouncementBanner(
            announcement = announcement,
            onCta = { viewModel.onIntent(AnnouncementIntent.CtaClicked(it)) },
            onDismiss = { viewModel.onIntent(AnnouncementIntent.Dismissed(it)) }
        )
        AnnouncementBanner(
            announcement = reminder,
            onCta = {
                reminderViewModel.onIntent(BackupReminderIntent.BackUpNow)
                onNavigate(NavRoute.Settings(startExport = true))
            },
            onDismiss = { reminderViewModel.onIntent(BackupReminderIntent.Later) },
            // Space only when both cards show; a hidden card takes none.
            contentPadding = PaddingValues(top = if (announcement != null) 8.dp else 0.dp)
        )
    }
}

/** Local id: Remote Config ids are free text, so this one is prefixed to never collide with them. */
private const val BACKUP_REMINDER_ID = "local_backup_reminder"
private const val BACKUP_REMINDER_ROUTE = "settings_page"
