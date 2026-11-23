package com.bhardwaj.passkey.presentation.screens.onboarding_screens

import androidx.annotation.StringRes
import com.bhardwaj.passkey.R

/** One pager page: a heading (with one [[highlighted]] phrase) and a description. */
enum class OnboardingPage(@StringRes val heading: Int, @StringRes val description: Int, val isTour: Boolean) {
    GENERATE(R.string.first_heading, R.string.first_description, isTour = false),
    ALL_HERE(R.string.second_heading, R.string.second_description, isTour = false),
    ONLY_YOU(R.string.third_heading, R.string.third_description, isTour = false),
    TOUR_ENTRIES(R.string.tour_entries_heading, R.string.tour_entries_description, isTour = true),
    TOUR_AUTOFILL(R.string.tour_autofill_heading, R.string.tour_autofill_description, isTour = true),
    TOUR_CODES(R.string.tour_codes_heading, R.string.tour_codes_description, isTour = true),
    TOUR_GENERATOR(R.string.tour_generator_heading, R.string.tour_generator_description, isTour = true),
    TOUR_BACKUP(R.string.tour_backup_heading, R.string.tour_backup_description, isTour = true),
    TOUR_PRIVACY(R.string.tour_privacy_heading, R.string.tour_privacy_description, isTour = true)
}

/**
 * First run shows the intro and then the feature tour, with Skip on every page. Settings replays the tour alone
 * (6.0.0), using the same pager and look, so nothing on screen is new to existing users.
 */
object OnboardingPages {
    val firstRun: List<OnboardingPage> = OnboardingPage.entries
    val tour: List<OnboardingPage> = OnboardingPage.entries.filter { it.isTour }
}
