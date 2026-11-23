package com.bhardwaj.passkey.presentation.screens.onboarding_screens

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.presentation.screens.common.ObserveAsEvents
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.OnBoardingEvents
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.OnBoardingViewModel
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.components.OnBoardingItem
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.components.OnBoardingPageIndicator
import com.bhardwaj.passkey.review.requestInAppReview
import kotlinx.coroutines.launch

data class OnBoardingScreen(val title: String, val description: String)

@Composable
fun OnBoardingScreen(onNavigate: (NavRoute) -> Unit, viewModel: OnBoardingViewModel = hiltViewModel()) {
    ObserveAsEvents(viewModel.effects) { event ->
        when (event) {
            is OnBoardingEffect.Navigate -> onNavigate(event.route)
        }
    }
    OnBoardingPager(
        pages = OnboardingPages.firstRun,
        onPageViewed = { page -> viewModel.onEvent(OnBoardingEvents.OnPageViewed(page)) },
        onSkip = { page -> viewModel.onEvent(OnBoardingEvents.OnSkip(page)) },
        onComplete = { viewModel.onEvent(OnBoardingEvents.OnBoardingComplete) }
    )
}

/** Settings › Feature tour: the tour pages alone, in the onboarding pager. Skip or finishing returns. */
@Composable
fun FeatureTourScreen(onClose: () -> Unit, viewModel: FeatureTourViewModel = hiltViewModel()) {
    val activity = LocalActivity.current
    ObserveAsEvents(viewModel.effects) { event ->
        when (event) {
            FeatureTourEffect.Close -> onClose()
            FeatureTourEffect.RequestReview -> requestInAppReview(activity)
        }
    }
    OnBoardingPager(
        pages = OnboardingPages.tour,
        onPageViewed = {},
        onSkip = { page -> viewModel.onEvent(FeatureTourEvent.Skip(page)) },
        onComplete = { viewModel.onEvent(FeatureTourEvent.Complete) }
    )
}

/**
 * The onboarding pager, unchanged in look. The emphasised word is marked with [[ ]] inside each heading string
 * resource, so each translation controls which of its own words is highlighted.
 */
@Composable
private fun OnBoardingPager(
    pages: List<OnboardingPage>,
    onPageViewed: (page: Int) -> Unit,
    onSkip: (page: Int) -> Unit,
    onComplete: () -> Unit
) {
    val screens = pages.map {
        OnBoardingScreen(title = stringResource(id = it.heading), description = stringResource(id = it.description))
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(initialPage = 0) { screens.size }
        val latestOnPageViewed by rememberUpdatedState(onPageViewed)
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page -> latestOnPageViewed(page) }
        }
        HorizontalPager(
            modifier = Modifier.fillMaxWidth(),
            state = pagerState
        ) { index ->
            OnBoardingItem(screen = screens[index])
        }
        OnBoardingPageIndicator(
            currentPage = pagerState.currentPage,
            totalPages = screens.size,
            onSkipClick = { onSkip(pagerState.currentPage) },
            onNextClick = { index ->
                if (index == pagerState.pageCount) {
                    onComplete()
                } else {
                    scope.launch { pagerState.animateScrollToPage(index) }
                }
            }
        )
    }
}
