package com.bhardwaj.passkey.about

import androidx.lifecycle.ViewModel
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.BaselineEvents
import com.bhardwaj.passkey.config.ConfigKeys
import com.bhardwaj.passkey.config.ConfigProvider
import com.bhardwaj.passkey.core.SafeUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** Build facts, passed from BuildConfig by the app (`BuildConfig.VERSION_NAME`, `VERSION_CODE`, `APPLICATION_ID`). */
data class AppInfo(val appName: String, val versionName: String, val versionCode: Long, val packageName: String)

data class AboutUiState(
    val app: AppInfo,
    val storeUrl: String,
    /** null hides "Contact us". */
    val contactEmail: String?,
    /** null hides "More apps". */
    val moreAppsUrl: String?
)

enum class AboutAction(val analyticsName: String) {
    RATE("rate"),
    SHARE("share"),
    CONTACT("contact"),
    MORE_APPS("more_apps"),
    LICENCES("licences"),
    TERMS("terms"),
    PRIVACY("privacy")
}

sealed interface AboutIntent {
    data class Clicked(val action: AboutAction) : AboutIntent

    /** The Play In-App Review flow could not run (no Play Store, error): fall back to the listing. */
    data object ReviewUnavailable : AboutIntent
}

sealed interface AboutEffect {
    data object LaunchInAppReview : AboutEffect

    data class OpenUrl(val url: String) : AboutEffect

    data class Share(val storeUrl: String) : AboutEffect

    data class Email(val address: String) : AboutEffect

    data object OpenLicences : AboutEffect

    data object OpenTerms : AboutEffect

    data object OpenPrivacy : AboutEffect
}

/**
 * About Us. Rate uses Play In-App Review first; Google shows it at most a few times per user and never says
 * whether it appeared, so a second tap in the same visit (or a failure) opens the store listing instead.
 * Hilt: `@HiltViewModel` + `@Inject constructor` with an `AppInfo` provided from BuildConfig.
 */
@HiltViewModel
class AboutViewModel @Inject constructor(
    app: AppInfo,
    config: ConfigProvider,
    private val analytics: AnalyticsTracker
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            AboutUiState(
                app = app,
                storeUrl = config.current.storeUrl(app.packageName),
                contactEmail = config.current[ConfigKeys.SUPPORT_EMAIL].trim().takeIf { EMAIL.matches(it) },
                moreAppsUrl = config.current[ConfigKeys.MORE_APPS_URL].trim().takeIf { SafeUrl.isHttps(it) }
            )
        )
    val state: StateFlow<AboutUiState> = _state.asStateFlow()

    private val _effects = Channel<AboutEffect>(Channel.BUFFERED)
    val effects: Flow<AboutEffect> = _effects.receiveAsFlow()

    private var reviewRequested = false

    fun onIntent(intent: AboutIntent) {
        when (intent) {
            is AboutIntent.Clicked -> onClicked(intent.action)
            AboutIntent.ReviewUnavailable -> _effects.trySend(AboutEffect.OpenUrl(_state.value.storeUrl))
        }
    }

    private fun onClicked(action: AboutAction) {
        val s = _state.value
        val effect =
            when (action) {
                AboutAction.RATE ->
                    if (reviewRequested) {
                        AboutEffect.OpenUrl(s.storeUrl)
                    } else {
                        reviewRequested = true
                        AboutEffect.LaunchInAppReview
                    }
                AboutAction.SHARE -> AboutEffect.Share(s.storeUrl)
                AboutAction.CONTACT -> s.contactEmail?.let { AboutEffect.Email(it) }
                AboutAction.MORE_APPS -> s.moreAppsUrl?.let { AboutEffect.OpenUrl(it) }
                AboutAction.LICENCES -> AboutEffect.OpenLicences
                AboutAction.TERMS -> AboutEffect.OpenTerms
                AboutAction.PRIVACY -> AboutEffect.OpenPrivacy
            } ?: return
        analytics.log(BaselineEvents.aboutAction(action.analyticsName))
        _effects.trySend(effect)
    }

    private companion object {
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
