package com.bhardwaj.passkey.presentation.views.screens.settings_screen.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.utils.AlertBy

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FaqItem(openedBy: AlertBy) {
    val bottomSheetContent = mapOf(
        AlertBy.PRIVACY to Pair(R.string.privacy, R.string.privacy_message),
        AlertBy.TERMS_N_CONDITIONS to Pair(
            R.string.terms_n_condition,
            R.string.terms_n_condition_message
        ),
        AlertBy.ABOUT to Pair(R.string.about, R.string.about_message),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        stickyHeader {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                Text(
                    text = stringResource(id = bottomSheetContent[openedBy]!!.first),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1F)
                )
            }
        }

        item {
            Text(
                text = stringResource(id = bottomSheetContent[openedBy]!!.second),
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}