package com.bhardwaj.passkey.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bhardwaj.passkey.R
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Stateless Terms / Privacy screen. Collect [LegalViewModel.effects] in the caller to open the hosted URL. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    state: LegalUiState,
    onIntent: (LegalIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(state.title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.legal_back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom =
                padding.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    stringResource(
                        R.string.legal_last_updated,
                        state.lastUpdated.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(state.sections, key = { it.heading }) { section ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(section.heading),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        stringResource(section.body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (state.hostedUrl != null) {
                item {
                    OutlinedButton(
                        onClick = { onIntent(LegalIntent.OpenOnline) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp)
                    ) {
                        Text(stringResource(R.string.legal_read_online))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PrivacyPreview() {
    val content = LegalTexts().privacy
    MaterialTheme {
        LegalScreen(
            state = LegalUiState(
                content.document,
                content.title,
                content.lastUpdated,
                content.sections,
                "https://example.com/privacy"
            ),
            onIntent = {},
            onBack = {}
        )
    }
}
