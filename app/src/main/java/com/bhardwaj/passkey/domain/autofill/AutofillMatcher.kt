package com.bhardwaj.passkey.domain.autofill

import com.bhardwaj.passkey.domain.model.Detail
import com.bhardwaj.passkey.domain.model.Preview

/** One vault entry, reduced to what an autofill dataset can put in a form. */
data class AutofillCredential(val previewId: Long, val label: String, val username: String?, val password: String)

/**
 * Decides which vault entries belong to the app or web page being filled.
 *
 * Kept free of Android types on purpose: the AssistStructure walk is untestable on the JVM, so
 * everything that can be a decision rather than a traversal lives here, where it can be.
 */
object AutofillMatcher {

    private const val MIN_TOKEN_LENGTH = 3

    /**
     * Parts of a package name or domain that identify nobody. "com.google.android.gm" and
     * "accounts.google.com" both have to reduce to "google" for either to find a Gmail entry.
     */
    private val NOISE = setOf(
        "com", "org", "net", "io", "co", "uk", "in", "de", "app", "apps", "android",
        "www", "mobile", "web", "login", "signin", "sign", "auth", "account", "accounts",
        "secure", "my", "id", "www2", "m"
    )

    /**
     * Second-level public suffixes common among this app's users. Without them "google.co.uk" would reduce to "co".
     * Not the full Public Suffix List (no new dependency); an unknown suffix only makes matching stricter.
     */
    private val TWO_PART_SUFFIXES = setOf(
        "co.uk", "org.uk", "ac.uk", "gov.uk", "co.in", "net.in", "org.in", "gov.in", "ac.in", "co.jp", "ne.jp",
        "com.au", "net.au", "org.au", "com.br", "com.mx", "co.za", "co.nz", "com.sg", "com.tr", "co.kr", "com.cn",
        "com.tw", "com.hk", "com.ar", "co.id", "com.my", "com.pk", "com.bd", "com.eg", "com.sa", "co.il", "com.ng"
    )

    /**
     * What identifies the app or site asking to be filled. Both inputs are chosen by whoever built it, so only
     * the parts that are hard to fake count:
     * - a web page: its registrable name ("google" for accounts.google.co.uk, never "google" for
     *   google.evil.com) and its full host;
     * - an app: the publisher part of its package ("google" for com.google.android.gm, not "google" for
     *   com.evil.google), and only when no web page was reported, so a browser never matches by its own name.
     */
    fun tokensOf(packageName: String?, webDomain: String?): Set<String> {
        val host = webDomain?.substringAfter("://")?.substringBefore('/')?.substringBefore(':')?.lowercase()
            ?.trim('.')?.takeIf { it.isNotBlank() }
        val tokens = if (host != null) {
            setOfNotNull(host, registrableName(host))
        } else {
            setOfNotNull(
                packageName?.lowercase()?.split('.')?.firstOrNull {
                    it !in NOISE &&
                        it.length >= MIN_TOKEN_LENGTH
                }
            )
        }
        return tokens.map { it.normalize() }.filter { it.length >= MIN_TOKEN_LENGTH && it !in NOISE }.toSet()
    }

    private fun registrableName(host: String): String? {
        val labels = host.split('.').filter { it.isNotEmpty() }
        if (labels.size < 2) return labels.singleOrNull()
        val suffixParts = if (labels.takeLast(2).joinToString(".") in TWO_PART_SUFFIXES) 2 else 1
        return labels.getOrNull(labels.size - suffixParts - 1)
    }

    /**
     * Entries whose heading names the same service: "Google" and "My Google account" match the token
     * "google"; "Gmail Work Account" matches "gmail".
     */
    fun match(
        previews: List<Preview>,
        detailsByPreview: Map<Long, List<Detail>>,
        tokens: Set<String>,
        secretKeywords: Set<String> = emptySet()
    ): List<AutofillCredential> {
        if (tokens.isEmpty()) return emptyList()
        return previews
            .filter { preview -> matches(preview.heading, tokens) }
            .mapNotNull { preview ->
                credentialFor(preview, detailsByPreview[preview.id].orEmpty(), secretKeywords)
            }
    }

    /**
     * Whole words or the whole heading, never a substring: "Googleplex" is not Google, and a token like
     * "fakegoogle" must not reach a "Google" entry. "G-Mail" still matches "gmail" through the joined heading.
     */
    private fun matches(heading: String, tokens: Set<String>): Boolean {
        val joined = heading.normalize()
        if (joined.length < MIN_TOKEN_LENGTH) return false
        val words = heading.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= MIN_TOKEN_LENGTH }
        return tokens.any { token -> token == joined || token in words }
    }

    /**
     * The password is the entry's secret; the username is whatever non-secret value comes first.
     *
     * Picking the username by elimination rather than by matching words like "user" or "email"
     * is deliberate - that kind of keyword match is what made the password analyser return
     * "perfect vault" in all 16 non-English locales.
     */
    fun credentialFor(
        preview: Preview,
        details: List<Detail>,
        secretKeywords: Set<String> = emptySet()
    ): AutofillCredential? {
        val ordered = details.sortedBy { it.sequence }
        val password = ordered.firstOrNull { it.isSecret }
            ?: ordered.firstOrNull { detail ->
                secretKeywords.any { detail.question.normalize().contains(it.normalize()) }
            }
            ?: return null
        if (password.answer.isBlank()) return null
        val username = ordered.firstOrNull { it.id != password.id && it.answer.isNotBlank() }
        return AutofillCredential(
            previewId = preview.id,
            label = preview.heading,
            username = username?.answer,
            password = password.answer
        )
    }

    private fun String.normalize(): String = lowercase().filter { it.isLetterOrDigit() }
}
