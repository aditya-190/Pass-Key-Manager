package com.bhardwaj.passkey.presentation.autofill

import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.view.autofill.AutofillManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.autofill.AutofillResponseBuilder
import com.bhardwaj.passkey.data.autofill.AutofillStructureParser
import com.bhardwaj.passkey.presentation.screens.security_screen.VaultGateViewModel
import com.bhardwaj.passkey.presentation.theme.PassKeyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Unlocks the vault on behalf of an autofill request, then answers it.
 *
 * The framework launches this from the single "Unlock" suggestion the service offers while the
 * vault is closed, and hands it the same assist structure the fill request carried. Once the
 * user authenticates, the datasets are built here and returned as the authentication result, so
 * the form is filled without a trip through the app.
 *
 * Paths the vault gate handles but this screen cannot - first-run provisioning, the one-time
 * re-key, recovery - send the user to the app instead. None of them should happen for the first
 * time inside another app's login form.
 */
@AndroidEntryPoint
class AutofillUnlockActivity : FragmentActivity() {

    @Inject
    lateinit var responseBuilder: AutofillResponseBuilder

    private val viewModel: VaultGateViewModel by viewModels()

    private var structure: AssistStructure? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        structure = IntentCompat.getParcelableExtra(
            intent,
            AutofillManager.EXTRA_ASSIST_STRUCTURE,
            AssistStructure::class.java
        )
        if (structure == null) {
            // Nothing to fill; cancelling leaves the user's own keyboard input untouched.
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        setContent {
            PassKeyTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                // Same prompt rules as the vault gate: prompt when the screen is really in front, never again
                // after a cancel until the user asks (on API 28 the PIN screen is a separate activity, and
                // re-prompting on every return looped forever).
                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(state) {
                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) promptIfAllowed()
                }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { promptIfAllowed() }
                LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onGateStopped() }
                UnlockContent(
                    state = state,
                    onAuthenticate = ::requestUnlock,
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onUnlocked = ::respondWithDatasets
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

    private fun requestUnlock() {
        viewModel.onUnlockRequested(
            activity = this,
            title = getString(R.string.app_name),
            subtitle = getString(R.string.autofill_unlock),
            negativeButton = getString(R.string.cancel)
        )
    }

    private fun respondWithDatasets() {
        val assist = structure ?: return
        lifecycleScope.launch {
            val fields = AutofillStructureParser.parse(assist)
            val credentials = responseBuilder.credentialsFor(
                fields,
                assist.activityComponent?.packageName
            )
            val response = responseBuilder.fillResponse(fields, credentials)
            setResult(
                RESULT_OK,
                Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response)
            )
            finish()
        }
    }
}

@Composable
private fun UnlockContent(
    state: VaultGateViewModel.State,
    onAuthenticate: () -> Unit,
    onCancel: () -> Unit,
    onUnlocked: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                // The first prompt starts by itself; after a cancel the user decides whether to try again.
                VaultGateViewModel.State.Locked -> {
                    Button(onClick = onAuthenticate) { Text(text = stringResource(R.string.autofill_unlock)) }
                    TextButton(modifier = Modifier.padding(top = 8.dp), onClick = onCancel) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }

                VaultGateViewModel.State.Checking,
                VaultGateViewModel.State.Authenticating,
                VaultGateViewModel.State.Migrating -> CircularProgressIndicator()

                VaultGateViewModel.State.Unlocked ->
                    LaunchedEffect(Unit) { onUnlocked() }

                else -> {
                    Text(
                        text = stringResource(R.string.autofill_open_app),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Button(
                        modifier = Modifier.padding(top = 16.dp),
                        onClick = onCancel
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}
