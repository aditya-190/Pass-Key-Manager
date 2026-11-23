package com.bhardwaj.passkey.domain.autofill

import java.security.MessageDigest

/**
 * Trust on first use for apps identified only by package name.
 *
 * A package name is chosen by whoever builds the app: a sideloaded app can reuse a real app's name when the real one
 * is not installed. The first time an app is filled, its signing certificate is remembered; a later request under
 * the same name but a different signer gets nothing. Web pages skip this: the browser vouches for the domain.
 */
object AppTrust {
    enum class Decision { FirstUse, Trusted, Mismatch }

    /** [actual] null means the signer could not be read, which is never trusted. */
    fun decide(stored: String?, actual: String?): Decision = when {
        actual == null -> Decision.Mismatch
        stored == null -> Decision.FirstUse
        stored == actual -> Decision.Trusted
        else -> Decision.Mismatch
    }

    /** SHA-256 of the sorted signer digests, so the same set in any order gives the same value. */
    fun fingerprint(signers: List<String>): String? {
        if (signers.isEmpty()) return null
        val digest = MessageDigest.getInstance("SHA-256").digest(signers.sorted().joinToString("|").toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
