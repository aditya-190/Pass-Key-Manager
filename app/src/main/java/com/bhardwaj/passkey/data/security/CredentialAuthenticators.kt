package com.bhardwaj.passkey.data.security

import android.os.Build
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL

/**
 * Authenticators for the time-bound CRED slot prompt. androidx.biometric throws IllegalArgumentException for
 * DEVICE_CREDENTIAL alone, and for BIOMETRIC_STRONG | DEVICE_CREDENTIAL, below API 30; there the only accepted
 * credential combination is BIOMETRIC_WEAK | DEVICE_CREDENTIAL. A weak biometric cannot open the Keystore key,
 * in which case unlocking falls through to the recovery password rather than failing.
 */
object CredentialAuthenticators {
    fun forSdk(sdkInt: Int): Int = if (sdkInt >= Build.VERSION_CODES.R) {
        BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    } else {
        BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    }
}
