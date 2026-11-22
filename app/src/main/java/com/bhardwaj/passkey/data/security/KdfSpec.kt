package com.bhardwaj.passkey.data.security

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Parameters used to turn a user password into a key-wrapping key.
 *
 * Stored alongside the wrapped key so parameters can be raised later without breaking existing
 * slots: an old slot keeps its own spec and is re-wrapped with the current one next time the
 * password is verified.
 */
@Serializable
sealed interface KdfSpec {

    val saltB64: String

    @Serializable
    @SerialName("pbkdf2-hmac-sha512")
    data class Pbkdf2HmacSha512(override val saltB64: String, val iterations: Int = DEFAULT_ITERATIONS) : KdfSpec {
        companion object {
            const val DEFAULT_ITERATIONS = 600_000
        }
    }
}
