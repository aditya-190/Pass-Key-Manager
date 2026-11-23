package com.bhardwaj.passkey.domain.autofill

import com.bhardwaj.passkey.domain.model.Detail
import java.text.Normalizer
import java.util.Locale

sealed interface SaveOp {
    data class Update(val detail: Detail) : SaveOp

    data class Create(val question: String, val answer: String, val isSecret: Boolean, val sequence: Long) : SaveOp
}

/**
 * Decides how a login captured by autofill lands in an existing entry.
 *
 * Saving used to append a new Password field every time, and the matcher fills the *first* secret, so after a
 * password change autofill kept offering the old one. Now the existing secret is updated (the repository moves the
 * old value into history) and a field is created only when the entry has none. The same rules pick fields as
 * [AutofillMatcher.credentialFor]: the secret first, then the first other non-blank field as the username.
 */
object AutofillSaveMerger {
    /** Field titles for entries autofill creates; the user can rename them. */
    const val USERNAME_LABEL = "Username"
    const val PASSWORD_LABEL = "Password"

    fun plan(existing: List<Detail>, username: String?, password: String, secretKeywords: Set<String>): List<SaveOp> {
        val ordered = existing.sortedBy { it.sequence }
        var nextSequence = (ordered.maxOfOrNull { it.sequence } ?: -1) + 1
        val ops = mutableListOf<SaveOp>()

        val secret = ordered.firstOrNull { it.isSecret }
            ?: ordered.firstOrNull { detail ->
                secretKeywords.any { detail.question.matchKey().contains(it.matchKey()) }
            }

        val user = username?.trim()?.takeIf { it.isNotEmpty() }
        if (user != null) {
            val field = ordered.firstOrNull { it.id != secret?.id && it.answer.isNotBlank() }
            when {
                field == null -> ops += SaveOp.Create(USERNAME_LABEL, user, isSecret = false, sequence = nextSequence++)
                field.answer != user -> ops += SaveOp.Update(field.copy(answer = user))
            }
        }

        when {
            secret == null -> ops += SaveOp.Create(PASSWORD_LABEL, password, isSecret = true, sequence = nextSequence)
            secret.answer != password || !secret.isSecret ->
                ops += SaveOp.Update(secret.copy(answer = password, isSecret = true))
        }
        return ops
    }

    private fun String.matchKey(): String = Normalizer.normalize(this, Normalizer.Form.NFKD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .trim()
}
