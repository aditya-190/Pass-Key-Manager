package com.bhardwaj.passkey.data.autofill

import android.app.assist.AssistStructure
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId

/**
 * The fields of one form that this app can fill or save.
 *
 * Only the traversal lives here. Which entries match, and which of their values are the username
 * and the password, is decided by AutofillMatcher, which has no Android types and is tested.
 */
data class AutofillFields(
    val usernameIds: List<AutofillId> = emptyList(),
    val passwordIds: List<AutofillId> = emptyList(),
    val webDomain: String? = null
) {
    /** A password field alone is fillable; a username field alone is not worth offering. */
    val isFillable: Boolean get() = passwordIds.isNotEmpty()

    fun allIds(): Array<AutofillId> = (usernameIds + passwordIds).toTypedArray()
}

object AutofillStructureParser {

    // Matched against view ids and HTML attribute names, which stay English even in a fully
    // localized app - unlike the user-visible labels the password analyser once matched on.
    private val PASSWORD_HINTS = listOf("password", "passwd", "pwd", "pass")
    private val USERNAME_HINTS = listOf("username", "user", "email", "login", "account", "phone")

    fun parse(structure: AssistStructure): AutofillFields {
        val usernames = mutableListOf<AutofillId>()
        val passwords = mutableListOf<AutofillId>()
        var domain: String? = null

        for (i in 0 until structure.windowNodeCount) {
            fun visit(node: AssistStructure.ViewNode) {
                node.webDomain?.takeIf { it.isNotBlank() }?.let { if (domain == null) domain = it }
                val id = node.autofillId
                if (id != null && node.autofillType == View.AUTOFILL_TYPE_TEXT) {
                    when (classify(node)) {
                        FieldKind.PASSWORD -> passwords += id
                        FieldKind.USERNAME -> usernames += id
                        FieldKind.UNKNOWN -> Unit
                    }
                }
                for (child in 0 until node.childCount) visit(node.getChildAt(child))
            }
            visit(structure.getWindowNodeAt(i).rootViewNode)
        }

        return AutofillFields(
            usernameIds = usernames,
            passwordIds = passwords,
            webDomain = domain
        )
    }

    /** What the user typed into a form, read back when the framework offers to save it. */
    data class SavedCredential(val username: String?, val password: String, val webDomain: String?)

    /**
     * Reads the values out of a submitted form.
     *
     * A save request arrives after the user has already typed, so the fields carry values; the
     * fill path deliberately ignores them.
     */
    fun extractCredential(structure: AssistStructure): SavedCredential? {
        val collector = CredentialCollector()
        for (i in 0 until structure.windowNodeCount) collector.visit(structure.getWindowNodeAt(i).rootViewNode)
        val secret = collector.password ?: return null
        return SavedCredential(username = collector.username, password = secret, webDomain = collector.domain)
    }

    /** First value of each kind wins, as the user sees the form top to bottom. */
    private class CredentialCollector {
        var username: String? = null
        var password: String? = null
        var domain: String? = null

        fun visit(node: AssistStructure.ViewNode) {
            if (domain == null) domain = node.webDomain?.takeIf { it.isNotBlank() }
            node.typedText()?.let { text ->
                when (classify(node)) {
                    FieldKind.PASSWORD -> if (password == null) password = text
                    FieldKind.USERNAME -> if (username == null) username = text
                    FieldKind.UNKNOWN -> Unit
                }
            }
            for (child in 0 until node.childCount) visit(node.getChildAt(child))
        }

        private fun AssistStructure.ViewNode.typedText(): String? =
            autofillValue?.takeIf { autofillType == View.AUTOFILL_TYPE_TEXT && it.isText }
                ?.textValue?.toString()?.takeIf { it.isNotBlank() }
    }

    private enum class FieldKind { USERNAME, PASSWORD, UNKNOWN }

    /**
     * Declared hints first, then the keyboard type, then the view's own id. The order matters:
     * an app that declares its hints correctly should never be second-guessed by a substring.
     */
    private fun classify(node: AssistStructure.ViewNode): FieldKind =
        byHints(node) ?: byInputType(node) ?: byName(node) ?: FieldKind.UNKNOWN

    private fun byHints(node: AssistStructure.ViewNode): FieldKind? = node.autofillHints.orEmpty()
        .firstNotNullOfOrNull { hint ->
            when (hint) {
                View.AUTOFILL_HINT_PASSWORD -> FieldKind.PASSWORD
                View.AUTOFILL_HINT_USERNAME, View.AUTOFILL_HINT_EMAIL_ADDRESS -> FieldKind.USERNAME
                // Compose and several libraries emit lowercase or vendor-prefixed hints.
                else -> byKeyword(hint.lowercase())
            }
        }

    private fun byInputType(node: AssistStructure.ViewNode): FieldKind? {
        if (node.inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return null
        return when (node.inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> FieldKind.PASSWORD

            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> FieldKind.USERNAME

            else -> null
        }
    }

    private fun byName(node: AssistStructure.ViewNode): FieldKind? = listOfNotNull(
        node.idEntry,
        node.hint,
        node.htmlInfo?.attributes?.firstOrNull { it.first == "name" }?.second
    ).joinToString(" ").lowercase().takeIf { it.isNotBlank() }?.let(::byKeyword)

    /** Password is checked first: "user_password" is a password field, not a username one. */
    private fun byKeyword(text: String): FieldKind? = when {
        PASSWORD_HINTS.any { text.contains(it) } -> FieldKind.PASSWORD
        USERNAME_HINTS.any { text.contains(it) } -> FieldKind.USERNAME
        else -> null
    }
}
