package com.bhardwaj.passkey.consent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.presentation.theme.PassKeyTheme
import com.bhardwaj.passkey.presentation.theme.Poppins

/**
 * First-run consent step, and Settings > Privacy options when [fromSettings] is true.
 * [onFinished] runs once the choice is saved.
 */
@Composable
fun ConsentRoute(
    fromSettings: Boolean,
    onFinished: () -> Unit,
    onBack: () -> Unit,
    viewModel: ConsentViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ConsentEffect.Finished -> onFinished()
            }
        }
    }
    ConsentScreen(state = state, fromSettings = fromSettings, onIntent = viewModel::onIntent, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentScreen(state: ConsentUiState, fromSettings: Boolean, onIntent: (ConsentIntent) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            if (fromSettings) {
                TopAppBar(
                    title = { Text(stringResource(R.string.privacy_options), fontFamily = Poppins) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Scrolls so nothing is clipped at 200% font size or in landscape.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 560.dp)) {
                if (!fromSettings) {
                    Text(
                        text = stringResource(R.string.consent_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = Poppins,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    text = stringResource(R.string.consent_body),
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = Poppins,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(16.dp))
                ConsentSwitchRow(
                    title = stringResource(R.string.consent_crash_title),
                    body = stringResource(R.string.consent_crash_body),
                    checked = state.crash,
                    enabled = !state.isLoading,
                    onCheckedChange = { onIntent(ConsentIntent.SetCrash(it)) }
                )
                ConsentSwitchRow(
                    title = stringResource(R.string.consent_analytics_title),
                    body = stringResource(R.string.consent_analytics_body),
                    checked = state.analytics,
                    enabled = !state.isLoading,
                    onCheckedChange = { onIntent(ConsentIntent.SetAnalytics(it)) }
                )
                Spacer(Modifier.height(24.dp))
                // Equal weight on purpose: declining must be exactly as easy as allowing.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onIntent(ConsentIntent.DeclineAll) },
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) { Text(stringResource(R.string.consent_decline_all), textAlign = TextAlign.Center) }
                    Button(
                        onClick = { onIntent(ConsentIntent.AllowAll) },
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) { Text(stringResource(R.string.consent_allow_all), textAlign = TextAlign.Center) }
                }
                TextButton(
                    onClick = { onIntent(ConsentIntent.Save) },
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) { Text(stringResource(R.string.consent_save)) }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.consent_change_later),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** The whole row is one switch for TalkBack: title, explanation and state are read together. */
@Composable
private fun ConsentSwitchRow(
    title: String,
    body: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = Poppins,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.padding(start = 12.dp))
        // Null handler: the row owns the toggle, so the switch isn't a second focus stop.
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsentScreenPreview() {
    PassKeyTheme {
        ConsentScreen(
            state = ConsentUiState(analytics = false, crash = true, isLoading = false),
            fromSettings = false,
            onIntent = {},
            onBack = {}
        )
    }
}
