package com.bhardwaj.passkey.data.autofill

import android.content.Context
import android.content.pm.PackageManager
import com.bhardwaj.passkey.domain.autofill.AppTrust
import java.security.MessageDigest

/**
 * Reads the signing certificate of the app asking to be filled, for [AppTrust]. Null when it cannot be read, which
 * AppTrust never trusts. On Android 11+ the app must be visible to PassKey; the manifest's `<queries>` for launcher
 * activities covers apps with a login screen (see docs/DECISIONS.md).
 */
object AppSigner {
    fun fingerprintOf(context: Context, packageName: String): String? = runCatching {
        val info = context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signers = info.signingInfo?.apkContentsSigners.orEmpty()
        AppTrust.fingerprint(
            signers.map { cert ->
                MessageDigest.getInstance("SHA-256").digest(cert.toByteArray()).joinToString("") { "%02x".format(it) }
            }
        )
    }.getOrNull()
}
