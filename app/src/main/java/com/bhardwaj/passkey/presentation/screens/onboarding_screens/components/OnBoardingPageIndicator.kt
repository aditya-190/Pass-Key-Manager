package com.bhardwaj.passkey.presentation.screens.onboarding_screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.presentation.screens.common.PassKeyButton
import com.bhardwaj.passkey.presentation.theme.BebasNeue
import com.bhardwaj.passkey.utils.ButtonType

@Composable
fun OnBoardingPageIndicator(
    modifier: Modifier = Modifier,
    currentPage: Int,
    totalPages: Int,
    onSkipClick: () -> Unit,
    onNextClick: (index: Int) -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .navigationBarsPadding()
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalPages) { index ->
                Text(
                    modifier = modifier.padding(end = 10.dp),
                    text = (index + 1).toString(),
                    color = if (currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    fontFamily = BebasNeue,
                    fontSize = if (currentPage == index) 24.sp else 14.sp,
                    fontStyle = FontStyle.Normal
                )
            }
        }
        Spacer(modifier = Modifier.height(36.dp))
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PassKeyButton(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .weight(1F)
                    .defaultMinSize(minHeight = 48.dp),
                text = stringResource(id = R.string.skip),
                buttonType = ButtonType.OUTLINED
            ) {
                onSkipClick()
            }
            PassKeyButton(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1F)
                    .defaultMinSize(minHeight = 48.dp),
                text = stringResource(id = R.string.next),
                buttonType = ButtonType.DEFAULT
            ) {
                onNextClick(currentPage + 1)
            }
        }
    }
}