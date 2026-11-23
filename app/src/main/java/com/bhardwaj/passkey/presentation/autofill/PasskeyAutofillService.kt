package com.bhardwaj.passkey.presentation.autofill

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.analytics.AppEvents.AutofillSaveResult
import com.bhardwaj.passkey.data.autofill.AppSigner
import com.bhardwaj.passkey.data.autofill.AutofillResponseBuilder
import com.bhardwaj.passkey.data.autofill.AutofillStructureParser
import com.bhardwaj.passkey.data.local.VaultDatabaseProvider
import com.bhardwaj.passkey.domain.autofill.AppTrust
import com.bhardwaj.passkey.domain.autofill.AutofillDecision
import com.bhardwaj.passkey.domain.autofill.AutofillSaveMerger
import com.bhardwaj.passkey.domain.autofill.SaveOp
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.domain.repository.AutofillTrustStore
import com.bhardwaj.passkey.domain.repository.PasskeyRepository
import com.bhardwaj.passkey.utils.SecretKeywords
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fills credentials into other apps and web pages.
 *
 * The third onboarding screen has advertised "Don't Type, Autofill Your Credentials" since the
 * app shipped, and no AutofillService existed. This is that promise.
 *
 * The vault is encrypted and usually locked, so a fill request has two outcomes: if the vault is
 * open, real datasets; if not, one authentication entry that opens [AutofillUnlockActivity] and
 * comes back with the datasets. Nothing about the vault's contents - not even how many entries
 * match - is visible before the user authenticates.
 */
@AndroidEntryPoint
class PasskeyAutofillService : AutofillService() {

    @Inject
    lateinit var vault: VaultDatabaseProvider

    @Inject
    lateinit var responseBuilder: AutofillResponseBuilder

    @Inject
    lateinit var repository: PasskeyRepository

    @Inject
    lateinit var analytics: AnalyticsTracker

    @Inject
    lateinit var trustStore: AutofillTrustStore

    @Inject
    lateinit var secretKeywords: SecretKeywords

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Default)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val fields = request.fillContexts.lastOrNull()?.structure?.let { AutofillStructureParser.parse(it) }
        when (AutofillDecision.forRequest(isFillable = fields?.isFillable == true)) {
            AutofillDecision.None -> callback.onSuccess(null)
            // Always authenticate, even while the vault is open: an unlocked vault (the grace period after any
            // unlock, or auto-lock "Never") used to hand logins to a matching app without asking. The suggestion
            // carries no entry names.
            AutofillDecision.Authenticate -> {
                analytics.log(AppEvents.autofillUnlockShown())
                callback.onSuccess(responseBuilder.authenticationResponse(fields!!, authenticationSender()))
            }
        }
    }

    /**
     * Saves a credential the user has just typed elsewhere.
     *
     * Only possible while the vault is open: writing needs the key, and a save request cannot authenticate. An
     * existing entry's password is updated (its old value goes to history) rather than a second one being added.
     */
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure
        val credential = structure?.let { AutofillStructureParser.extractCredential(it) }
        when {
            credential == null || credential.password.isBlank() -> return fail(callback, R.string.autofill_save_failed)
            !vault.isOpen -> return fail(callback, R.string.autofill_save_locked, AutofillSaveResult.REFUSED_LOCKED)
        }
        val clientPackage = structure.activityComponent?.packageName
        scope.launch {
            val result = runCatching { save(credential, clientPackage) }.getOrDefault(AutofillSaveResult.FAILED)
            analytics.log(AppEvents.autofillSaved(result))
            when (result) {
                AutofillSaveResult.REFUSED_UNTRUSTED -> callback.onFailure(getString(R.string.autofill_untrusted_app))
                AutofillSaveResult.FAILED -> callback.onFailure(getString(R.string.autofill_save_failed))
                else -> callback.onSuccess()
            }
        }
    }

    private suspend fun save(
        credential: AutofillStructureParser.SavedCredential,
        clientPackage: String?
    ): AutofillSaveResult {
        // Apps (not web pages) must still be the app first seen under that name.
        val trustedPackage = clientPackage?.takeIf { credential.webDomain == null }
        val signer = trustedPackage?.let { AppSigner.fingerprintOf(this, it) }
        val trust = trustedPackage?.let { AppTrust.decide(trustStore.trustedSigner(it), signer) }
        if (trust == AppTrust.Decision.Mismatch) return AutofillSaveResult.REFUSED_UNTRUSTED

        val heading = credential.heading(clientPackage)
        val outcome = repository.runInTransaction {
            val existingId = repository.getPreviewByHeading(heading, Category.OTHERS)?.id
            val previewId = existingId ?: repository.createPreview(heading, Category.OTHERS)
            val existing = existingId?.let { repository.getDetailsByPreviewId(it).first() }.orEmpty()
            val ops = AutofillSaveMerger.plan(existing, credential.username, credential.password, secretKeywords.all())
            ops.forEach { op ->
                when (op) {
                    is SaveOp.Update -> repository.updateDetail(op.detail)
                    is SaveOp.Create -> repository.createDetail(
                        previewId,
                        op.question,
                        op.answer,
                        op.sequence,
                        op.isSecret
                    )
                }
            }
            when {
                existingId == null -> AutofillSaveResult.CREATED
                ops.isEmpty() -> AutofillSaveResult.UNCHANGED
                else -> AutofillSaveResult.UPDATED
            }
        }
        if (trust == AppTrust.Decision.FirstUse && signer != null) trustStore.rememberSigner(trustedPackage, signer)
        return outcome
    }

    private fun fail(callback: SaveCallback, message: Int, result: AutofillSaveResult = AutofillSaveResult.FAILED) {
        analytics.log(AppEvents.autofillSaved(result))
        callback.onFailure(getString(message))
    }

    private fun authenticationSender(): android.content.IntentSender {
        // Mutable because the framework adds the assist structure to this intent before
        // launching it. Below API 31 there is no flag to pass: PendingIntents were mutable by
        // default, and FLAG_IMMUTABLE is what had to be asked for.
        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        return PendingIntent.getActivity(
            this,
            AUTH_REQUEST_CODE,
            Intent(this, AutofillUnlockActivity::class.java),
            PendingIntent.FLAG_CANCEL_CURRENT or mutable
        ).intentSender
    }

    private companion object {
        const val AUTH_REQUEST_CODE = 9001
    }
}

private fun AutofillStructureParser.SavedCredential.heading(clientPackage: String?): String {
    // The web domain names the service; a package name is the fallback, tidied to its most
    // distinctive part so the entry reads "instagram" rather than "com.instagram.android".
    webDomain?.substringAfter("://")?.substringBefore('/')?.takeIf { it.isNotBlank() }
        ?.let { return it }
    val parts = clientPackage?.split('.').orEmpty()
    return parts.getOrNull(1) ?: clientPackage ?: "Autofill"
}
