package com.bhardwaj.passkey.security

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.bhardwaj.passkey.data.security.CredentialAuthenticators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Found on the API 28 emulator: androidx.biometric rejects DEVICE_CREDENTIAL alone (and BIOMETRIC_STRONG |
 * DEVICE_CREDENTIAL) below API 30 with IllegalArgumentException, so every Android 9/10 user crashed on each
 * launch of the re-keyed vault. The only credential combination accepted there is BIOMETRIC_WEAK | DEVICE_CREDENTIAL.
 */
class CredentialAuthenticatorsTest {
    @Test
    fun `android 9 and 10 use the one combination androidx biometric accepts`() {
        listOf(28, 29).forEach { sdk ->
            assertEquals("sdk $sdk", BIOMETRIC_WEAK or DEVICE_CREDENTIAL, CredentialAuthenticators.forSdk(sdk))
        }
    }

    @Test
    fun `android 11 and later allow strong biometrics or the device credential`() {
        listOf(30, 33, 36, 37).forEach { sdk ->
            assertEquals("sdk $sdk", BIOMETRIC_STRONG or DEVICE_CREDENTIAL, CredentialAuthenticators.forSdk(sdk))
        }
    }

    @Test
    fun `no supported sdk ever gets a combination that throws`() {
        (28..40).forEach { sdk ->
            val allowed = CredentialAuthenticators.forSdk(sdk)
            if (sdk < 30) {
                assertNotEquals("sdk $sdk", DEVICE_CREDENTIAL, allowed)
                assertNotEquals("sdk $sdk", BIOMETRIC_STRONG or DEVICE_CREDENTIAL, allowed)
            }
        }
    }
}
