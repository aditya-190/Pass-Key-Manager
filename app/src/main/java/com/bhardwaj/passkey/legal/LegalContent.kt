package com.bhardwaj.passkey.legal

import androidx.annotation.StringRes
import com.bhardwaj.passkey.R
import java.time.LocalDate

enum class LegalDocument(val analyticsName: String) {
    TERMS("terms"),
    PRIVACY("privacy")
}

data class LegalSection(@StringRes val heading: Int, @StringRes val body: Int)

/**
 * In-app copy of a legal document. [lastUpdated] is the date of THIS text; bump it with every wording change
 * and keep the hosted page (RC `terms_url` / `privacy_url`) in sync.
 */
data class LegalContent(
    val document: LegalDocument,
    @StringRes val title: Int,
    val lastUpdated: LocalDate,
    val sections: List<LegalSection>
)

/**
 * TODO(port): write the app's own text (structure: ArrowPuzzle / MindGames privacy pages). Every data flow
 * listed must match the Play Data safety form. Strings live in res/values/strings_baseline.xml.
 *
 * Optional sections are composed in only when the app has the feature, so their strings can be deleted:
 * ads apps pass `privacyExtras = AdLegalSections.privacy` (ads/, strings_baseline_ads.xml), apps that sell
 * something pass `termsExtras = PurchaseLegalSections.terms` (strings_baseline_purchases.xml). Extras go after
 * the core data sections and before Children / Contact (privacy) or after "Using the app" (terms).
 * Example: `LegalTexts(privacyExtras = AdLegalSections.privacy, termsExtras = PurchaseLegalSections.terms)`.
 */
class LegalTexts(privacyExtras: List<LegalSection> = emptyList(), termsExtras: List<LegalSection> = emptyList()) {
    val privacy =
        LegalContent(
            document = LegalDocument.PRIVACY,
            title = R.string.legal_privacy_title,
            lastUpdated = LocalDate.of(2026, 11, 22),
            sections =
            listOf(
                LegalSection(R.string.legal_privacy_intro_heading, R.string.legal_privacy_intro_body),
                LegalSection(R.string.legal_privacy_device_heading, R.string.legal_privacy_device_body),
                LegalSection(R.string.legal_privacy_autofill_heading, R.string.legal_privacy_autofill_body),
                LegalSection(R.string.legal_privacy_analytics_heading, R.string.legal_privacy_analytics_body)
            ) + privacyExtras +
                listOf(
                    LegalSection(R.string.legal_privacy_children_heading, R.string.legal_privacy_children_body),
                    LegalSection(R.string.legal_privacy_deletion_heading, R.string.legal_privacy_deletion_body),
                    LegalSection(R.string.legal_contact_heading, R.string.legal_contact_body)
                )
        )

    val terms =
        LegalContent(
            document = LegalDocument.TERMS,
            title = R.string.legal_terms_title,
            lastUpdated = LocalDate.of(2026, 11, 22),
            sections =
            listOf(LegalSection(R.string.legal_terms_use_heading, R.string.legal_terms_use_body)) + termsExtras +
                listOf(
                    LegalSection(R.string.legal_terms_content_heading, R.string.legal_terms_content_body),
                    LegalSection(R.string.legal_terms_liability_heading, R.string.legal_terms_liability_body),
                    LegalSection(R.string.legal_terms_changes_heading, R.string.legal_terms_changes_body),
                    LegalSection(R.string.legal_contact_heading, R.string.legal_contact_body)
                )
        )

    fun of(document: LegalDocument): LegalContent = when (document) {
        LegalDocument.TERMS -> terms
        LegalDocument.PRIVACY -> privacy
    }
}
