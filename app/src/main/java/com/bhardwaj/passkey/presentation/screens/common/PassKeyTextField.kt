package com.bhardwaj.passkey.presentation.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhardwaj.passkey.presentation.theme.BebasNeue

@Composable
fun PassKeyTextField(
    value: String,
    onValueChanged: (String) -> Unit,
    headingName: String,
    modifier: Modifier = Modifier,
    placeHolderText: String = "",
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    maxLines: Int = 10
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            textStyle = textStyle,
            maxLines = maxLines,
            decorationBox = { innerTextField ->
                Box(modifier = Modifier) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeHolderText,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    innerTextField()
                }
            },
        )

        Text(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp)
                .background(MaterialTheme.colorScheme.background),
            text = headingName,
            fontFamily = BebasNeue,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}