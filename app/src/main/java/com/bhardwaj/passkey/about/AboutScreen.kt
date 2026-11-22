package com.bhardwaj.passkey.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.bhardwaj.passkey.R

/**
 * Stateless About Us screen. Collect [AboutViewModel.effects] in the caller and perform them with
 * [ExternalLinks] / [InAppReview] / navigation (see README).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    state: AboutUiState,
    onIntent: (AboutIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.legal_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom =
                padding.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Header(state.app) }
            item {
                SectionCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.about_studio_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            stringResource(R.string.about_studio_blurb),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                SectionCard {
                    LinkRow(R.string.about_rate, Icons.Filled.Star) { onIntent(AboutIntent.Clicked(AboutAction.RATE)) }
                    LinkRow(R.string.about_share, Icons.Filled.Share) {
                        onIntent(AboutIntent.Clicked(AboutAction.SHARE))
                    }
                    if (state.contactEmail != null) {
                        LinkRow(R.string.about_contact, Icons.Filled.Email) {
                            onIntent(AboutIntent.Clicked(AboutAction.CONTACT))
                        }
                    }
                    if (state.moreAppsUrl != null) {
                        LinkRow(R.string.about_more_apps, Icons.Filled.Favorite) {
                            onIntent(AboutIntent.Clicked(AboutAction.MORE_APPS))
                        }
                    }
                }
            }
            item {
                SectionCard {
                    LinkRow(R.string.legal_terms_title, Icons.Filled.Info) {
                        onIntent(AboutIntent.Clicked(AboutAction.TERMS))
                    }
                    LinkRow(R.string.legal_privacy_title, Icons.Filled.Lock) {
                        onIntent(AboutIntent.Clicked(AboutAction.PRIVACY))
                    }
                    LinkRow(R.string.about_licences, Icons.AutoMirrored.Filled.List, divider = false) {
                        onIntent(AboutIntent.Clicked(AboutAction.LICENCES))
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.about_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun Header(app: AppInfo) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val icon = rememberAppIcon()
        val shape = RoundedCornerShape(24.dp)
        if (icon != null) {
            Image(icon, contentDescription = null, modifier = Modifier.size(88.dp).clip(shape))
        } else {
            Box(Modifier.size(88.dp).clip(shape).background(MaterialTheme.colorScheme.primaryContainer))
        }
        Text(
            app.appName,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp).semantics { heading() }
        )
        Text(
            stringResource(R.string.about_version, app.versionName, app.versionCode),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The launcher icon as a bitmap (adaptive icons can't go through painterResource). Null in previews. */
@Composable
private fun rememberAppIcon(): ImageBitmap? {
    if (LocalInspectionMode.current) return null
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap()
        }.getOrNull()
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column { content() }
    }
}

@Composable
private fun LinkRow(label: Int, icon: ImageVector, divider: Boolean = true, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(label)) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.clickable(onClick = onClick)
    )
    if (divider) HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Preview
@Composable
private fun AboutPreview() {
    MaterialTheme {
        AboutScreen(
            state =
            AboutUiState(
                app = AppInfo("Sample App", "1.4.0", 14, "com.example.app"),
                storeUrl = "https://play.google.com/store/apps/details?id=com.example.app",
                contactEmail = "hello@example.com",
                moreAppsUrl = "https://play.google.com/store/apps/dev?id=0"
            ),
            onIntent = {},
            onBack = {}
        )
    }
}
