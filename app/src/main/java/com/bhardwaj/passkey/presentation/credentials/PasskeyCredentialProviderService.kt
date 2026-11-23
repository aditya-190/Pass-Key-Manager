package com.bhardwaj.passkey.presentation.credentials

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.domain.credentials.BeginPlan
import com.bhardwaj.passkey.domain.credentials.CredentialProviderPolicy

/**
 * PassKey in Android 14's sign-in sheet (6.0.0), for apps that ask through Credential Manager.
 *
 * Like autofill: the sheet only ever shows one "Unlock to fill" action, never entry names, and everything else
 * (matching, the signer check, the fingerprint prompt) happens in [CredentialUnlockActivity]. Saving new logins is
 * not offered here yet; autofill's save covers it.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PasskeyCredentialProviderService : CredentialProviderService() {

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>
    ) {
        val wantsPassword = request.beginGetCredentialOptions.any { it is BeginGetPasswordOption }
        val response = when (CredentialProviderPolicy.begin(wantsPassword)) {
            BeginPlan.UNLOCK_ACTION -> BeginGetCredentialResponse(
                authenticationActions = listOf(
                    AuthenticationAction(title = getString(R.string.autofill_unlock), pendingIntent = unlockIntent())
                )
            )
            BeginPlan.NOTHING -> BeginGetCredentialResponse()
        }
        callback.onResult(response)
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>
    ) {
        // Nothing offered for saving through the sheet (yet).
        callback.onResult(BeginCreateCredentialResponse())
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>
    ) {
        // PassKey keeps no sign-in state for other apps.
        callback.onResult(null)
    }

    /** Mutable: the system adds the request to it before it starts the activity. */
    private fun unlockIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        UNLOCK_REQUEST_CODE,
        Intent(this, CredentialUnlockActivity::class.java).setAction(CredentialUnlockActivity.ACTION_LIST),
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private companion object {
        const val UNLOCK_REQUEST_CODE = 6_001
    }
}
