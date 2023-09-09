package com.bhardwaj.passkey.presentation.views.screens.splash_screen

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.presentation.viewModels.SplashViewModel
import com.bhardwaj.passkey.presentation.views.events.SplashEvents
import com.bhardwaj.passkey.utils.UiEvents
import kotlinx.coroutines.delay

@Composable
fun SplashPage(
    onNavigate: (UiEvents.Navigate) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val scale = remember {
        Animatable(0.5F)
    }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvents.Navigate -> onNavigate(event)
                else -> Unit
            }
        }
    }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1F,
            animationSpec = tween(
                durationMillis = 500,
                easing = {
                    OvershootInterpolator(3F).getInterpolation(it)
                }
            )
        )
        delay(300)
        viewModel.onEvent(SplashEvents.OnLoadingComplete)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .scale(scale.value)
                .clip(CircleShape)
        )
    }
}