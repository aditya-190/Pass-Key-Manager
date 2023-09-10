package com.bhardwaj.passkey.presentation.views.screens.preview_screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bhardwaj.passkey.utils.Categories

data class BottomNavigationItem(
    val title: String,
    val icon: ImageVector,
    val isVisible: Boolean = true
)

val bottomNavigationList = listOf(
    BottomNavigationItem(
        title = Categories.BANKS.name,
        icon = Icons.Filled.AccountBalance
    ),
    BottomNavigationItem(
        title = Categories.APPS.name,
        icon = Icons.Filled.Gamepad
    ),
    BottomNavigationItem(
        title = Categories.BANKS.name,
        icon = Icons.Filled.AccountBalance,
        isVisible = false,
    ),

    BottomNavigationItem(
        title = Categories.MAILS.name,
        icon = Icons.Filled.Email
    ),
    BottomNavigationItem(
        title = Categories.OTHERS.name,
        icon = Icons.Filled.Article
    )
)

@Composable
fun MainBottomNavigation(
    selectedIndex: Int,
    onItemClick: (index: Int, title: String) -> Unit,
    onFabClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavigationList.forEachIndexed { index, item ->
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(42.dp)
                        .padding(8.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                if (item.isVisible) {
                                    onItemClick(index, item.title)
                                }
                            })
                        .alpha(if (item.isVisible) 1F else 0F),
                    tint = if (selectedIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
        }
        FloatingActionButton(
            modifier = Modifier.padding(bottom = 42.dp),
            onClick = { onFabClick() },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            content = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Preview",
                )
            },
        )
    }
}