package com.bhardwaj.passkey.data.repository

import com.bhardwaj.passkey.data.local.VaultDatabaseProvider
import com.bhardwaj.passkey.data.local.entity.AutofillTrustEntity
import com.bhardwaj.passkey.domain.repository.AutofillTrustStore
import javax.inject.Inject

class AutofillTrustStoreImpl @Inject constructor(private val vault: VaultDatabaseProvider) : AutofillTrustStore {
    override suspend fun trustedSigner(packageName: String): String? = vault.requireDb().trustDao.signerOf(packageName)

    override suspend fun rememberSigner(packageName: String, signer: String) {
        vault.requireDb().trustDao.insert(AutofillTrustEntity(packageName, signer, System.currentTimeMillis()))
    }
}
