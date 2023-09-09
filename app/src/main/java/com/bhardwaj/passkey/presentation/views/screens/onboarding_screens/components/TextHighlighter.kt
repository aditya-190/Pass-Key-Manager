package com.bhardwaj.passkey.presentation.views.screens.onboarding_screens.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bhardwaj.passkey.presentation.views.theme.BebasNeue

@Composable
fun TextHighlighter(
    modifier: Modifier = Modifier,
    fullText: String,
    highlightedText: List<String>,
) {
    val annotatedString = buildAnnotatedString {
        append(fullText)
        highlightedText.forEach { text ->
            val startIndex = fullText.indexOf(text)
            val endIndex = startIndex + text.length
            addStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary
                ),
                start = startIndex,
                end = endIndex
            )
        }
        addStyle(
            style = SpanStyle(
                fontFamily = BebasNeue,
                fontSize = 64.sp,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Normal,
            ),
            start = 0,
            end = fullText.length
        )
    }
    Text(
        modifier = modifier,
        text = annotatedString,
        lineHeight = 70.sp,
        color = MaterialTheme.colorScheme.secondary
    )
}