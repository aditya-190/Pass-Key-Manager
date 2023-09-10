package com.bhardwaj.passkey.presentation.views.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhardwaj.passkey.presentation.views.theme.BebasNeue
import com.bhardwaj.passkey.utils.ButtonType

@Composable
fun PassKeyButton(
    modifier: Modifier = Modifier,
    text: String,
    buttonType: ButtonType = ButtonType.DEFAULT,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .then(
                if (buttonType == ButtonType.DEFAULT) {
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary)
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            )
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clickable(
                onClick = { onClick() },
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = BebasNeue,
            color = if (buttonType == ButtonType.DEFAULT) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary,
            fontSize = 16.sp,
            fontStyle = FontStyle.Normal
        )
    }
}