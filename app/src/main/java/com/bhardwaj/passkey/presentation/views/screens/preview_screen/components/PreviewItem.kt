package com.bhardwaj.passkey.presentation.views.screens.preview_screen.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhardwaj.passkey.presentation.views.events.PreviewEvents
import com.bhardwaj.passkey.presentation.views.theme.Poppins
import com.bhardwaj.passkey.data.local.entity.Preview as PreviewEntity

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewItem(
    modifier: Modifier = Modifier,
    preview: PreviewEntity,
    onEvent: (PreviewEvents) -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .background(Color.Transparent)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(10.dp)
                )
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = "Icon Drag",
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1F)
                    .combinedClickable(
                        onClick = {
                            onEvent(PreviewEvents.OnPreviewClick(preview.previewId!!))
                        },
                        onLongClick = {
                            onEvent(PreviewEvents.OnLongPress(preview.heading))
                        },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                text = preview.heading,
                fontFamily = Poppins,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Icon(
                modifier = Modifier.clickable(
                    onClick = {
                        onEvent(PreviewEvents.OnChangeClick(preview))
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
                imageVector = Icons.Default.Edit,
                contentDescription = "Icon Edit",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}