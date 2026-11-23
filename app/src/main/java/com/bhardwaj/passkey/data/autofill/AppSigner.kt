package com.bhardwaj.passkey.data.autofill

import android.content.Context
import android.content.pm.PackageManager
import com.bhardwaj.passkey.domain.credentials.CredentialProviderPolicy

/**
 * Reads the signing certificate of the app asking to be filled, for [AppTrust]. Null when it cannot be read, which
 * AppTrust never trusts. On Android 11+ the app must be visible to PassKey; the manifest's `<queries>` for launcher
 * activities covers apps with a login screen (see docs/DECISIONS.md).
 */
object AppSigner {
    fun fingerprintOf(context: Context, packageName: String): String? = runCatching {
        val info = context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signers = info.signingInfo?.apkContentsSigners.orEmpty()
        // One definition for autofill and the sign-in sheet (6.0.0), so the two can never disagree.
        CredentialProviderPolicy.signerFingerprint(signers.map { it.toByteArray() })
    }.getOrNull()
}
