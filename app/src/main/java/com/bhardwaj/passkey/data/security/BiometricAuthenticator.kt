package com.bhardwaj.passkey.data.security

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Suspend wrapper over [BiometricPrompt].
 *
 * Replaces a fire-and-forget callback style whose only effect was setting a flag that nothing
 * read, so authentication was a UI gate rather than a cryptographic one.
 *
 * Two distinct prompts exist because of a hard constraint in androidx.biometric 1.1.0: calling
 * `authenticate(promptInfo, cryptoObject)` with DEVICE_CREDENTIAL among the allowed
 * authenticators throws IllegalArgumentException below API 30. Since minSdk is 28, the crypto
 * path must be BIOMETRIC_STRONG only, and the credential path must run without a CryptoObject
 * and rely on a time-bound key instead.
 */
@Singleton
class BiometricAuthenticator @Inject constructor() {

    sealed interface Outcome {
        data class Success(val cipher: Cipher?) : Outcome
        data object UserCancelled : Outcome
        data object Lockout : Outcome
        data object NoneEnrolled : Outcome
        data class Failed(val code: Int, val message: CharSequence) : Outcome
    }

    fun canAuthenticate(activity: FragmentActivity): Int =
        BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)

    fun hasStrongBiometric(activity: FragmentActivity): Boolean = BiometricManager.from(activity)
        .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Strong-biometric prompt bound to [cipher]. BIOMETRIC_WEAK is deliberately excluded: a weak
     * biometric can never back a Keystore key, and the previous implementation allowed it.
     */
    suspend fun authenticateWithCrypto(
        activity: FragmentActivity,
        cipher: Cipher,
        title: String,
        subtitle: String,
        negativeButton: String
    ): Outcome = prompt(
        activity = activity,
        cryptoObject = BiometricPrompt.CryptoObject(cipher),
        allowed = BIOMETRIC_STRONG,
        title = title,
        subtitle = subtitle,
        negativeButton = negativeButton
    )

    /**
     * Device-credential (or biometric) prompt with no CryptoObject. Opens the validity window
     * for the time-bound CRED key, which the caller must use promptly.
     */
    suspend fun authenticateForTimeBoundKey(activity: FragmentActivity, title: String, subtitle: String): Outcome =
        prompt(
            activity = activity,
            cryptoObject = null,
            allowed = CredentialAuthenticators.forSdk(Build.VERSION.SDK_INT),
            title = title,
            subtitle = subtitle,
            negativeButton = null
        )

    private suspend fun prompt(
        activity: FragmentActivity,
        cryptoObject: BiometricPrompt.CryptoObject?,
        allowed: Int,
        title: String,
        subtitle: String,
        negativeButton: String?
    ): Outcome = suspendCancellableCoroutine { continuation ->
        // Building the prompt validates the authenticator combination per API level and throws on an unsupported
        // one. That must never crash the vault gate: report it as a failure so unlock falls through to the
        // recovery password (found on API 28, where DEVICE_CREDENTIAL alone is rejected).
        val info = runCatching { buildInfo(allowed, title, subtitle, negativeButton) }.getOrElse { error ->
            continuation.resume(Outcome.Failed(code = -1, message = error.javaClass.simpleName))
            return@suspendCancellableCoroutine
        }
        startPrompt(activity, cryptoObject, info, continuation)
    }

    private fun buildInfo(allowed: Int, title: String, subtitle: String, negativeButton: String?) =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(allowed)
            .apply {
                // Required whenever DEVICE_CREDENTIAL is not among the allowed authenticators.
                if (allowed and DEVICE_CREDENTIAL == 0 && negativeButton != null) {
                    setNegativeButtonText(negativeButton)
                }
            }
            .build()

    private fun startPrompt(
        activity: FragmentActivity,
        cryptoObject: BiometricPrompt.CryptoObject?,
        info: BiometricPrompt.PromptInfo,
        continuation: CancellableContinuation<Outcome>
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(code: Int, message: CharSequence) {
                    if (!continuation.isActive) return
                    continuation.resume(
                        when (code) {
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_CANCELED -> Outcome.UserCancelled

                            BiometricPrompt.ERROR_LOCKOUT,
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> Outcome.Lockout

                            BiometricPrompt.ERROR_NO_BIOMETRICS,
                            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> Outcome.NoneEnrolled

                            else -> Outcome.Failed(code, message)
                        }
                    )
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (!continuation.isActive) return
                    continuation.resume(Outcome.Success(result.cryptoObject?.cipher))
                }

                // onAuthenticationFailed is a single rejected attempt, not a terminal state;
                // the prompt stays up and the framework will call error() if it gives up.
            }
        )

        continuation.invokeOnCancellation { prompt.cancelAuthentication() }

        if (cryptoObject != null) {
            prompt.authenticate(info, cryptoObject)
        } else {
            prompt.authenticate(info)
        }
    }
}
