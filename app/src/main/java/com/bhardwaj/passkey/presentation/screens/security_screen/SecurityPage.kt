package com.bhardwaj.passkey.presentation.screens.security_screen

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.security.lockTrace
import com.bhardwaj.passkey.domain.events.SecurityEvents
import com.bhardwaj.passkey.domain.viewModels.SecurityViewModel
import com.bhardwaj.passkey.presentation.theme.Poppins
import com.bhardwaj.passkey.utils.UiEvents
import com.bhardwaj.passkey.utils.asString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SecurityScreen(navController: NavController, viewModel: SecurityViewModel = hiltViewModel()) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvents.Navigate -> {
                    navController.popBackStack()
                    navController.navigate(event.route)
                }

                is UiEvents.ShowSnackBar -> {
                    scope.launch {
                        snackBarHostState.showSnackbar(
                            message = event.message.asString(context),
                            actionLabel = event.action?.asString(context)
                        )
                    }
                }

                else -> Unit
            }
        }
    }

    val activity = LocalActivity.current as FragmentActivity
    val startForResult =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == FragmentActivity.RESULT_CANCELED) {
                viewModel.onEvent(SecurityEvents.AddPasswordToTheDeviceFirst)
            }
            viewModel.onEvent(SecurityEvents.OnAuthenticationError)
        }

    val title = stringResource(id = R.string.app_name)
    val subTitle = stringResource(id = R.string.welcome_to_pass_key)

    val showPrompt = {
        viewModel.onEvent(SecurityEvents.OnPromptStarted)
        biometricPrompt(
            title = title,
            subTitle = subTitle,
            context = context,
            activity = activity,
            viewModel = viewModel,
            scope = scope,
            startForResult = startForResult
        )
    }

    // Prompt only once the screen is really in front. Starting it from a screen composed in the background
    // (the vault locks while the app is away) left users on a screen with nothing to tap.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val prompt = viewModel.shouldPromptOnResume()
        lockTrace("lock screen ON_RESUME autoPrompt=$prompt")
        if (prompt) showPrompt()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        lockTrace("lock screen ON_STOP")
        viewModel.onEvent(SecurityEvents.OnStopped)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .semantics { heading() },
                text = stringResource(id = R.string.security_heading),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = Poppins,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            if (viewModel.needToOpenAlertDialog) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(id = R.string.security_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            // Always on screen, so the lock screen is never a dead end.
            Button(
                onClick = { if (viewModel.canPromptManually()) showPrompt() },
                modifier = Modifier
                    .padding(top = 24.dp)
                    .heightIn(min = 48.dp)
            ) {
                Text(text = stringResource(id = R.string.security_button), fontFamily = Poppins)
            }
        }
    }
}

fun biometricPrompt(
    title: String,
    subTitle: String,
    context: Context,
    activity: FragmentActivity,
    viewModel: SecurityViewModel,
    scope: CoroutineScope,
    startForResult: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricManager by lazy { BiometricManager.from(context) }
    val allowedAuthenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subTitle)
        .setAllowedAuthenticators(allowedAuthenticators)
        .build()

    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                lockTrace("prompt error code=$errorCode")
                super.onAuthenticationError(errorCode, errString)
                viewModel.onEvent(SecurityEvents.OnAuthenticationError)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                lockTrace("prompt success")
                super.onAuthenticationSucceeded(result)
                viewModel.onEvent(SecurityEvents.OnAuthenticationSucceeded)
            }
        }
    )
    when (biometricManager.canAuthenticate(allowedAuthenticators)) {
        BiometricManager.BIOMETRIC_SUCCESS -> {
            biometricPrompt.authenticate(promptInfo)
        }

        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(
                        Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        allowedAuthenticators
                    )
                }
                scope.launch {
                    startForResult.launch(intent)
                }
            } else {
                viewModel.onEvent(SecurityEvents.AddPasswordToTheDeviceFirst)
            }
        }

        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
            viewModel.onEvent(SecurityEvents.OnAuthenticationError)
        }
    }
}
