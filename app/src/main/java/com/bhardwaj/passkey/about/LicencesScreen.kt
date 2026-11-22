package com.bhardwaj.passkey.about

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bhardwaj.passkey.R

data class OssLibrary(val name: String, val licence: String)

/** Libraries every baseline app ships. Add the app's own (and fonts, sounds) and keep docs/ASSET_LICENSES.md in sync. */
val baselineLibraries =
    listOf(
        OssLibrary("AndroidX, Jetpack Compose and Material 3", "Apache License 2.0"),
        OssLibrary("Kotlin, kotlinx.coroutines and kotlinx.serialization", "Apache License 2.0"),
        OssLibrary("Firebase Android SDKs", "Apache License 2.0"),
        OssLibrary(
            "Google Play In-App Update and In-App Review",
            "Play Core Software Development Kit Terms of Service"
        ),
        OssLibrary("Material icons", "Apache License 2.0")
    )

/** Stateless open-source licences list (no extra dependency; the plugin-based screen is optional). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicencesScreen(libraries: List<OssLibrary>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_licences)) },
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
                top = padding.calculateTopPadding(),
                bottom =
                padding.calculateBottomPadding() + 24.dp
            )
        ) {
            items(libraries, key = { it.name }) { library ->
                ListItem(headlineContent = { Text(library.name) }, supportingContent = { Text(library.licence) })
                HorizontalDivider()
            }
        }
    }
}

@Preview
@Composable
private fun LicencesPreview() {
    MaterialTheme { LicencesScreen(baselineLibraries, onBack = {}) }
}
