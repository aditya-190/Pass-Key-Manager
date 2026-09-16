package com.bhardwaj.passkey.data.security

import android.util.Base64
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/** Derives a 256-bit key-wrapping key from a user password. */
interface KeyDerivation {
    fun derive(password: CharArray, spec: KdfSpec): ByteArray
    fun newSalt(): ByteArray
}

@Singleton
class DefaultKeyDerivation @Inject constructor() : KeyDerivation {

    private val secureRandom = SecureRandom()

    override fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)

    override fun derive(password: CharArray, spec: KdfSpec): ByteArray {
        val salt = Base64.decode(spec.saltB64, Base64.NO_WRAP)
        return when (spec) {
            is KdfSpec.Pbkdf2HmacSha512 -> derivePbkdf2(password, salt, spec.iterations)
        }
    }

    private fun derivePbkdf2(
        password: CharArray,
        salt: ByteArray,
        iterations: Int
    ): ByteArray {
        val keySpec = PBEKeySpec(password, salt, iterations, KEY_BYTES * 8)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
                .generateSecret(keySpec).encoded
        } finally {
            keySpec.clearPassword()
        }
    }

    private companion object {
        const val SALT_BYTES = 16
        const val KEY_BYTES = 32
    }
}
