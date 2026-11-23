package com.bhardwaj.passkey.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.presentation.screens.common.SecureModalBottomSheet

/** Full screen, not dismissible: Back is swallowed and the only action is updating. */
@Composable
fun HardUpdateScreen(title: String, message: String, onUpdate: () -> Unit, modifier: Modifier = Modifier) {
    BlockingScreen(
        icon = Icons.Filled.Refresh,
        title = title.ifBlank { stringResource(R.string.update_hard_title) },
        message = message.ifBlank { stringResource(R.string.update_hard_body) },
        actionLabel = stringResource(R.string.update_action),
        onAction = onUpdate,
        modifier = modifier
    )
}

@Composable
fun MaintenanceScreen(message: String, modifier: Modifier = Modifier) {
    BlockingScreen(
        icon = Icons.Filled.Build,
        title = stringResource(R.string.maintenance_title),
        message = message.ifBlank { stringResource(R.string.maintenance_body) },
        actionLabel = null,
        onAction = {},
        modifier = modifier
    )
}

@Composable
private fun BlockingScreen(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) { /* blocked by design */ }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(24.dp))
                Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (actionLabel != null) {
                Button(onClick = onAction, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}

/** Dismissible soft-update offer, used when Play's FLEXIBLE flow isn't available. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftUpdateSheet(title: String, message: String, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    SecureModalBottomSheet(onDismissRequest = onDismiss) {
        SoftUpdateContent(title, message, onUpdate, onDismiss)
    }
}

@Composable
private fun SoftUpdateContent(title: String, message: String, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Text(
            title.ifBlank {
                stringResource(R.string.update_soft_title)
            },
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message.ifBlank { stringResource(R.string.update_soft_body) },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onUpdate, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(R.string.update_action))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.update_later))
        }
    }
}

/** Shown after a FLEXIBLE update finished downloading. Restarting installs it. */
@Composable
fun FlexibleUpdateReadyBar(onRestart: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 6.dp
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.update_ready),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRestart) {
                Text(stringResource(R.string.update_restart), color = MaterialTheme.colorScheme.inversePrimary)
            }
        }
    }
}

/** Opens the store listing [url], falling back to the market:// intent for this package. */
fun openStoreListing(context: Context, url: String) {
    val fallback =
        Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val primary =
        if (url.isNotBlank()) {
            Intent(
                Intent.ACTION_VIEW,
                url.toUri()
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            fallback
        }
    try {
        context.startActivity(primary)
    } catch (e: ActivityNotFoundException) {
        runCatching { context.startActivity(fallback) }
    }
}

@Preview
@Composable
private fun HardUpdatePreview() {
    MaterialTheme { HardUpdateScreen(title = "", message = "", onUpdate = {}) }
}

@Preview
@Composable
private fun MaintenancePreview() {
    MaterialTheme { MaintenanceScreen(message = "") }
}

@Preview
@Composable
private fun SoftUpdatePreview() {
    MaterialTheme { Surface { SoftUpdateContent(title = "", message = "", onUpdate = {}, onDismiss = {}) } }
}

@Preview
@Composable
private fun FlexibleReadyPreview() {
    MaterialTheme { FlexibleUpdateReadyBar(onRestart = {}) }
}
