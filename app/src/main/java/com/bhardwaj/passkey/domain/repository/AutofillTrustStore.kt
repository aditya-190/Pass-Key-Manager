package com.bhardwaj.passkey.domain.repository

/** Signers remembered for autofill trust on first use (see AppTrust). Lives in the encrypted vault. */
interface AutofillTrustStore {
    suspend fun trustedSigner(packageName: String): String?

    suspend fun rememberSigner(packageName: String, signer: String)
}
