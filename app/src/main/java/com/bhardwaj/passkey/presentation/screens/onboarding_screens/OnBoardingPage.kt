package com.bhardwaj.passkey.presentation.screens.onboarding_screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.domain.events.OnBoardingEvents
import com.bhardwaj.passkey.domain.viewModels.OnBoardingViewModel
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.components.OnBoardingItem
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.components.OnBoardingPageIndicator
import com.bhardwaj.passkey.utils.UiEvents
import kotlinx.coroutines.launch

data class OnBoardingScreen(
    val title: String,
    val description: String,
    val highlightedText: List<String>
)

@Composable
fun OnBoardingScreen(
    onNavigate: (UiEvents.Navigate) -> Unit,
    viewModel: OnBoardingViewModel = hiltViewModel()
) {
    val pages = listOf(
        OnBoardingScreen(
            title = stringResource(id = R.string.first_heading),
            description = stringResource(id = R.string.first_description),
            highlightedText = listOf("Secure")
        ),
        OnBoardingScreen(
            title = stringResource(id = R.string.second_heading),
            description = stringResource(id = R.string.second_description),
            highlightedText = listOf("Passwords")
        ),
        OnBoardingScreen(
            title = stringResource(id = R.string.third_heading),
            description = stringResource(id = R.string.third_description),
            highlightedText = listOf("Autofill")
        )
    )

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvents.Navigate -> onNavigate(event)
                else -> Unit
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val scope = rememberCoroutineScope()
            val pagerState = rememberPagerState(initialPage = 0) { pages.size }
            HorizontalPager(
                modifier = Modifier.fillMaxWidth(),
                state = pagerState
            ) { index ->
                OnBoardingItem(
                    screen = pages[index]
                )
            }
            OnBoardingPageIndicator(
                currentPage = pagerState.currentPage,
                totalPages = pages.size,
                onSkipClick = {
                    viewModel.onEvent(OnBoardingEvents.OnBoardingComplete)
                },
                onNextClick = { index ->
                    if (index == pagerState.pageCount) {
                        viewModel.onEvent(OnBoardingEvents.OnBoardingComplete)
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                }
            )
        }
    }
}