package com.bhardwaj.passkey.banner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bhardwaj.passkey.R
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.launch

/**
 * Remote Config announcement card. Stateless: pass the current [announcement] (null hides it with an exit
 * animation). Swipe sideways or tap ✕ to dismiss (when dismissible); TalkBack gets a "Dismiss" action.
 * Hidden, it takes no space: pass spacing as [contentPadding], not as outer padding or a spacedBy gap.
 * Colours are fixed per style and pass WCAG AA for white text in light and dark themes.
 */
@Composable
fun AnnouncementBanner(
    announcement: Announcement?,
    onCta: (Announcement) -> Unit,
    onDismiss: (Announcement) -> Unit,
    modifier: Modifier = Modifier,
    /** Space around the card that appears and disappears with it (use instead of outer padding/spacing). */
    contentPadding: PaddingValues = PaddingValues()
) {
    // Keep the last item while the exit animation runs.
    var retained by remember { mutableStateOf(announcement) }
    if (announcement != null) retained = announcement
    AnimatedVisibility(
        visible = announcement != null,
        modifier = modifier,
        enter =
        fadeIn(tween(ENTER_MS)) + expandVertically(tween(ENTER_MS)) + slideInVertically(tween(ENTER_MS)) {
            -it / 3
        },
        exit = fadeOut(tween(EXIT_MS)) + shrinkVertically(tween(EXIT_MS))
    ) {
        retained?.let { item ->
            Box(Modifier.padding(contentPadding)) { AnnouncementCard(item, onCta, onDismiss) }
        }
    }
}

@Composable
private fun AnnouncementCard(item: Announcement, onCta: (Announcement) -> Unit, onDismiss: (Announcement) -> Unit) {
    val scope = rememberCoroutineScope()
    val offsetX = remember(item.id) { Animatable(0f) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    val dismissLabel = stringResource(R.string.announcement_dismiss)
    val colors = gradientFor(item.style)
    val shape = RoundedCornerShape(20.dp)

    val swipe =
        if (item.dismissible) {
            Modifier
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta -> scope.launch { offsetX.snapTo(offsetX.value + delta) } },
                    onDragStopped = { velocity ->
                        scope.launch {
                            val fling = abs(velocity) > FLING_VELOCITY
                            if (abs(offsetX.value) > widthPx * DISMISS_FRACTION || fling) {
                                val direction = if (offsetX.value + velocity >= 0f) 1f else -1f
                                offsetX.animateTo(direction * widthPx, tween(EXIT_MS))
                                onDismiss(item)
                            } else {
                                offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        }
                    }
                ).semantics {
                    customActions =
                        listOf(
                            CustomAccessibilityAction(dismissLabel) {
                                onDismiss(item)
                                true
                            }
                        )
                }
        } else {
            Modifier
        }

    Box(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .graphicsLayer {
                translationX = offsetX.value
                alpha = 1f - (abs(offsetX.value) / widthPx).coerceIn(0f, 1f) * FADE_ON_SWIPE
                rotationZ = sign(offsetX.value) * (abs(offsetX.value) / widthPx).coerceIn(0f, 1f) * TILT_DEGREES
            }.then(swipe)
            .shadow(8.dp, shape, clip = false)
            .clip(shape)
            .background(Brush.linearGradient(colors))
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                Modifier.size(40.dp).background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    iconFor(item.style),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                Modifier.weight(1f).padding(start = 12.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                if (item.body.isNotBlank()) {
                    Text(
                        item.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
                if (item.hasCta) {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { onCta(item) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = colors.first()
                        )
                    ) {
                        Text(item.ctaLabel.orEmpty(), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (item.dismissible) {
                IconButton(onClick = { onDismiss(item) }) {
                    Icon(Icons.Filled.Close, contentDescription = dismissLabel, tint = Color.White)
                }
            }
        }
    }
}

private fun gradientFor(style: AnnouncementStyle): List<Color> = when (style) {
    AnnouncementStyle.INFO -> listOf(Color(0xFF1D4ED8), Color(0xFF4338CA))
    AnnouncementStyle.PROMO -> listOf(Color(0xFF7E22CE), Color(0xFFBE185D))
    AnnouncementStyle.SUCCESS -> listOf(Color(0xFF047857), Color(0xFF0F766E))
    AnnouncementStyle.WARNING -> listOf(Color(0xFFB45309), Color(0xFFC2410C))
}

private fun iconFor(style: AnnouncementStyle): ImageVector = when (style) {
    AnnouncementStyle.INFO -> Icons.Filled.Info
    AnnouncementStyle.PROMO -> Icons.Filled.Star
    AnnouncementStyle.SUCCESS -> Icons.Filled.CheckCircle
    AnnouncementStyle.WARNING -> Icons.Filled.Warning
}

private const val ENTER_MS = 320
private const val EXIT_MS = 220
private const val DISMISS_FRACTION = 0.35f
private const val FLING_VELOCITY = 1_500f
private const val FADE_ON_SWIPE = 0.7f
private const val TILT_DEGREES = 4f

@Preview(widthDp = 380)
@Composable
private fun AnnouncementBannerPreview() {
    MaterialTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnnouncementStyle.entries.forEach { style ->
                AnnouncementBanner(
                    announcement =
                    Announcement(
                        id = style.analyticsName,
                        title = "Fresh puzzles are live",
                        body = "Fifty new hand-made levels, plus a weekend challenge.",
                        ctaLabel = "Play now",
                        route = "daily",
                        style = style,
                        dismissible = style != AnnouncementStyle.WARNING
                    ),
                    onCta = {},
                    onDismiss = {}
                )
            }
        }
    }
}
