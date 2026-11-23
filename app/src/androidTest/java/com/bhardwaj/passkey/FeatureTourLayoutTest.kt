package com.bhardwaj.passkey

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bhardwaj.passkey.analytics.NoOpAnalyticsTracker
import com.bhardwaj.passkey.config.AppConfig
import com.bhardwaj.passkey.config.ConfigProvider
import com.bhardwaj.passkey.core.Clock
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.FeatureTourScreen
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.FeatureTourViewModel
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.OnboardingPages
import com.bhardwaj.passkey.presentation.theme.PassKeyTheme
import com.bhardwaj.passkey.review.ReviewPromptState
import com.bhardwaj.passkey.review.ReviewPromptStore
import com.bhardwaj.passkey.review.ReviewPrompter
import com.google.common.truth.Truth.assertWithMessage
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 6.0.0 attack-pass #1: the tour descriptions are longer than the old onboarding text. The real tour screen (pager
 * plus Skip/Next) at 200% font, in every shipped language: no description may be cut off, and its last line must be
 * reachable above the buttons.
 */
@RunWith(AndroidJUnit4::class)
class FeatureTourLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val locales = listOf(
        "en", "hi", "de", "mr", "bn", "te", "ta", "gu", "zh", "es", "pt", "ru", "ja", "ko", "fr", "it", "ar"
    )

    private fun contextFor(tag: String): Context = app.createConfigurationContext(
        Configuration(app.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(tag))
            fontScale = 2f
        }
    )

    @Test
    fun everyTourPageCanBeReadToTheEndAtDoubleFontSizeInEveryLanguage() {
        var localized by mutableStateOf(contextFor("en"))
        val viewModel = FeatureTourViewModel(
            NoOpAnalyticsTracker,
            SavedStateHandle(),
            ReviewPrompter(
                NoReviewStore,
                Clock { 0L },
                object : ConfigProvider {
                    override val current = AppConfig()
                },
                NoOpAnalyticsTracker
            )
        )
        compose.setContent {
            val density = LocalDensity.current
            key(localized) {
                CompositionLocalProvider(
                    LocalContext provides localized,
                    LocalConfiguration provides localized.resources.configuration,
                    LocalDensity provides Density(density.density, fontScale = 2f)
                ) {
                    PassKeyTheme {
                        FeatureTourScreen(onClose = {}, viewModel = viewModel)
                    }
                }
            }
        }
        locales.forEach { tag ->
            localized = contextFor(tag)
            compose.waitForIdle()
            val next = localized.getString(R.string.next)
            OnboardingPages.tour.forEachIndexed { index, page ->
                val description = localized.getString(page.description)
                // Attack-pass #8: right after an install the device is slow; wait until the pager has settled on
                // this page (exactly one copy of its text) instead of assuming it already has.
                compose.waitUntil(PAGE_TIMEOUT_MS) {
                    compose.onAllNodesWithText(description).fetchSemanticsNodes().size == 1
                }
                val text = compose.onNodeWithText(description)
                repeat(SWIPES) { text.performTouchInput { swipeUp() } }
                compose.waitForIdle()

                val layouts = mutableListOf<TextLayoutResult>()
                text.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
                assertWithMessage("$tag ${page.name}: description is cut off")
                    .that(layouts.single().didOverflowHeight)
                    .isFalse()
                val buttonsTop = compose.onNodeWithText(next).getBoundsInRoot().top
                assertWithMessage("$tag ${page.name}: description runs under the buttons")
                    .that(text.getBoundsInRoot().bottom.value)
                    .isAtMost(buttonsTop.value)

                if (index < OnboardingPages.tour.lastIndex) {
                    compose.onNodeWithText(next).performClick()
                    compose.waitForIdle()
                }
            }
        }
    }

    private companion object {
        const val SWIPES = 4
        const val PAGE_TIMEOUT_MS = 10_000L
    }
}

/** Never asks: the layout test only reads pages. */
private object NoReviewStore : ReviewPromptStore {
    override val state = kotlinx.coroutines.flow.flowOf(ReviewPromptState())
    override suspend fun markFirstSeen(at: Long) = Unit
    override suspend fun recordAsked(at: Long) = Unit
    override suspend fun recordFill() = Unit
}
