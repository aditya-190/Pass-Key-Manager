package com.bhardwaj.passkey.data.autofill

import android.content.Context
import android.content.IntentSender
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.domain.autofill.AutofillCredential
import com.bhardwaj.passkey.domain.autofill.AutofillMatcher
import com.bhardwaj.passkey.domain.autofill.AutofillResponsePlan
import com.bhardwaj.passkey.domain.repository.PasskeyRepository
import com.bhardwaj.passkey.utils.SecretKeywords
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Turns matched vault entries into the response the autofill framework expects.
 *
 * Shared by the service and by the unlock activity, which has to build the same response after
 * authenticating - otherwise the two would drift and the post-unlock fill would behave
 * differently from the ordinary one.
 */
@Singleton
class AutofillResponseBuilder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: PasskeyRepository,
    private val secretKeywords: SecretKeywords
) {

    suspend fun credentialsFor(fields: AutofillFields, packageName: String?): List<AutofillCredential> =
        credentialsFor(packageName, fields.webDomain)

    /** The same matching for callers that are not a form (Android 14's sign-in sheet, 6.0.0). */
    suspend fun credentialsFor(packageName: String?, webDomain: String?): List<AutofillCredential> {
        val tokens = AutofillMatcher.tokensOf(packageName, webDomain)
        if (tokens.isEmpty()) return emptyList()
        val previews = repository.getPreviews().first()
        val details = repository.getDetails().first().groupBy { it.previewId }
        return AutofillMatcher.match(previews, details, tokens, secretKeywords.all())
    }

    fun fillResponse(fields: AutofillFields, credentials: List<AutofillCredential>): FillResponse? {
        val plan = AutofillResponsePlan.afterUnlock(credentials.size, canSave(fields)) ?: return null
        val builder = FillResponse.Builder()
        if (plan.datasets) credentials.forEach { builder.addDataset(dataset(fields, it)) }
        if (plan.offerSave) addSaveInfo(builder, fields)
        return builder.build()
    }

    /**
     * What a locked vault offers: a single entry that authenticates instead of filling.
     *
     * No entry names appear here. The suggestion list is drawn over another app's window, so a
     * locked vault must not leak which accounts it holds.
     */
    @Suppress("DEPRECATION") // setAuthentication(Presentations) is API 33+; minSdk here is 28.
    fun authenticationResponse(fields: AutofillFields, sender: IntentSender): FillResponse {
        val presentation = presentation(
            label = context.getString(R.string.autofill_unlock),
            sublabel = context.getString(R.string.app_name)
        )
        val builder = FillResponse.Builder().setAuthentication(fields.allIds(), sender, presentation)
        if (AutofillResponsePlan.forAuthentication(canSave(fields)).offerSave) addSaveInfo(builder, fields)
        return builder.build()
    }

    @Suppress("DEPRECATION") // Dataset.Builder(Presentations) is API 33+; minSdk here is 28.
    private fun dataset(fields: AutofillFields, credential: AutofillCredential): Dataset {
        val builder = Dataset.Builder(
            presentation(credential.label, credential.username.orEmpty())
        )
        val username = credential.username
        if (username != null) {
            fields.usernameIds.forEach { builder.setValue(it, AutofillValue.forText(username)) }
        }
        fields.passwordIds.forEach {
            builder.setValue(it, AutofillValue.forText(credential.password))
        }
        return builder.build()
    }

    /**
     * Offers to save only when both halves of a credential are present. A save prompt on a
     * password-only form would create an entry with nothing to identify it by.
     */
    private fun canSave(fields: AutofillFields) = fields.usernameIds.isNotEmpty() && fields.passwordIds.isNotEmpty()

    private fun addSaveInfo(builder: FillResponse.Builder, fields: AutofillFields) {
        if (!canSave(fields)) return
        builder.setSaveInfo(
            SaveInfo.Builder(
                SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                fields.allIds()
            ).build()
        )
    }

    private fun presentation(label: String, sublabel: String): RemoteViews =
        RemoteViews(context.packageName, R.layout.autofill_dataset).apply {
            setTextViewText(R.id.autofill_label, label)
            setTextViewText(R.id.autofill_sublabel, sublabel)
        }
}
