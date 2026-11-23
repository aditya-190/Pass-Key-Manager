package com.bhardwaj.passkey.presentation.credentials

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.PendingIntentHandler
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.data.autofill.AutofillResponseBuilder
import com.bhardwaj.passkey.data.security.VaultSession
import com.bhardwaj.passkey.domain.autofill.AppTrust
import com.bhardwaj.passkey.domain.autofill.AutofillCredential
import com.bhardwaj.passkey.domain.credentials.CredentialProviderPolicy
import com.bhardwaj.passkey.domain.repository.AutofillTrustStore
import com.bhardwaj.passkey.domain.repository.PreferencesRepository
import com.bhardwaj.passkey.presentation.autofill.UnlockContent
import com.bhardwaj.passkey.presentation.screens.security_screen.VaultGateViewModel
import com.bhardwaj.passkey.presentation.theme.PassKeyAppTheme
import com.bhardwaj.passkey.review.ReviewPrompter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Two steps of PassKey in Android 14's sign-in sheet (6.0.0):
 *  - [ACTION_LIST], from the sheet's "Unlock to fill": the user unlocks (every time, like autofill), the calling app's
 *    signer is checked against the one first seen under its name, and the matching logins go back to the sheet.
 *  - [ACTION_PICK], from one of those logins: it is found again from fresh matches (nothing secret travels in the
 *    intent) and returned. The vault must still be open; if it locked in between, the user unlocks again.
 *
 * Web pages in browsers are not matched yet (that needs a trusted-browser list); apps are, by package and signer.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@AndroidEntryPoint
class CredentialUnlockActivity : FragmentActivity() {
    @Inject
    lateinit var responseBuilder: AutofillResponseBuilder

    @Inject
    lateinit var trustStore: AutofillTrustStore

    @Inject
    lateinit var vaultSession: VaultSession

    @Inject
    lateinit var analytics: AnalyticsTracker

    @Inject
    lateinit var preferences: PreferencesRepository

    @Inject
    lateinit var reviewPrompter: ReviewPrompter

    private val viewModel: VaultGateViewModel by viewModels()
    private val untrusted = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        if (callerOf(intent) == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        // A login picked moments after unlocking returns straight away; anything else asks first.
        if (intent.action == ACTION_PICK && vaultSession.isUnlocked.value) {
            respond()
            return
        }
        setContent {
            PassKeyAppTheme(themeMode = preferences.themeMode) {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val isUntrusted by untrusted.collectAsStateWithLifecycle()
                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(state) {
                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) promptIfAllowed()
                }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { promptIfAllowed() }
                LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onGateStopped() }
                UnlockContent(
                    state = state,
                    isUntrusted = isUntrusted,
                    onAuthenticate = ::requestUnlock,
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onUnlocked = ::respond
                )
            }
        }
        viewModel.start()
    }

    private fun promptIfAllowed() = viewModel.onGateVisible(
        activity = this,
        title = getString(R.string.app_name),
        subtitle = getString(R.string.autofill_unlock),
        negativeButton = getString(R.string.cancel)
    )

    private fun requestUnlock() = viewModel.onUnlockRequested(
        activity = this,
        title = getString(R.string.app_name),
        subtitle = getString(R.string.autofill_unlock),
        negativeButton = getString(R.string.cancel)
    )

    private fun respond() {
        val caller = callerOf(intent) ?: return cancel()
        lifecycleScope.launch {
            val credentials = trustedCredentials(caller) ?: return@launch
            when (intent.action) {
                ACTION_PICK -> returnPicked(credentials)
                else -> returnList(credentials)
            }
        }
    }

    /** Null (and the "different app" message) when the caller's signer is not the one first seen under its name. */
    private suspend fun trustedCredentials(caller: CallingAppInfo): List<AutofillCredential>? {
        val signer = CredentialProviderPolicy.signerFingerprint(
            caller.signingInfo.apkContentsSigners.orEmpty().map { it.toByteArray() }
        )
        val trust = AppTrust.decide(trustStore.trustedSigner(caller.packageName), signer)
        if (trust == AppTrust.Decision.Mismatch) {
            untrusted.value = true
            return null
        }
        val credentials = CredentialProviderPolicy.offerable(
            responseBuilder.credentialsFor(caller.packageName, webDomain = null),
            forWebsite = caller.isOriginPopulated()
        )
        if (trust == AppTrust.Decision.FirstUse && signer != null && credentials.isNotEmpty()) {
            trustStore.rememberSigner(caller.packageName, signer)
        }
        return credentials
    }

    private fun returnList(credentials: List<AutofillCredential>) {
        val request = PendingIntentHandler.retrieveBeginGetCredentialRequest(intent) ?: return cancel()
        val option = request.beginGetCredentialOptions.filterIsInstance<BeginGetPasswordOption>().firstOrNull()
            ?: return cancel()
        val entries = credentials.mapIndexed { index, credential ->
            PasswordCredentialEntry.Builder(
                this,
                credential.username.orEmpty(),
                pickIntent(index, credential),
                option
            ).setDisplayName(credential.label).build()
        }
        val result = Intent()
        PendingIntentHandler.setBeginGetCredentialResponse(
            result,
            BeginGetCredentialResponse(credentialEntries = entries)
        )
        setResult(RESULT_OK, result)
        finish()
    }

    private suspend fun returnPicked(credentials: List<AutofillCredential>) {
        val previewId = intent.getLongExtra(EXTRA_PREVIEW_ID, -1)
        val username = intent.getStringExtra(EXTRA_USERNAME)
        val chosen = CredentialProviderPolicy.pick(credentials, previewId, username) ?: return cancel()
        analytics.log(AppEvents.autofillFilled(matches = 1))
        reviewPrompter.recordFill()
        val result = Intent()
        PendingIntentHandler.setGetCredentialResponse(
            result,
            GetCredentialResponse(PasswordCredential(chosen.username.orEmpty(), chosen.password))
        )
        setResult(RESULT_OK, result)
        finish()
    }

    /** Carries only which login, never its password. */
    private fun pickIntent(index: Int, credential: AutofillCredential): PendingIntent = PendingIntent.getActivity(
        this,
        PICK_REQUEST_BASE + index,
        Intent(this, CredentialUnlockActivity::class.java)
            .setAction(ACTION_PICK)
            .putExtra(EXTRA_PREVIEW_ID, credential.previewId)
            .putExtra(EXTRA_USERNAME, credential.username),
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun callerOf(intent: Intent): CallingAppInfo? = when (intent.action) {
        ACTION_PICK -> PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)?.callingAppInfo
        else -> PendingIntentHandler.retrieveBeginGetCredentialRequest(intent)?.callingAppInfo
    }

    private fun cancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    companion object {
        const val ACTION_LIST = "com.bhardwaj.passkey.credentials.LIST"
        const val ACTION_PICK = "com.bhardwaj.passkey.credentials.PICK"
        private const val EXTRA_PREVIEW_ID = "previewId"
        private const val EXTRA_USERNAME = "username"
        private const val PICK_REQUEST_BASE = 6_100
    }
}
