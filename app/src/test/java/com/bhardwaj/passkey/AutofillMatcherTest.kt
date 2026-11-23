package com.bhardwaj.passkey

import com.bhardwaj.passkey.domain.autofill.AutofillMatcher
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.domain.model.Detail
import com.bhardwaj.passkey.domain.model.Preview
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AutofillMatcherTest {

    private fun preview(id: Long, heading: String) = Preview(id = id, heading = heading, category = Category.APPS)

    private fun detail(
        id: Long,
        previewId: Long,
        question: String,
        answer: String,
        sequence: Long = 0,
        isSecret: Boolean = false
    ) = Detail(id, previewId, question, answer, sequence, isSecret)

    @Test
    fun `a package name and a domain reduce to the same identifying token`() {
        assertThat(AutofillMatcher.tokensOf("com.google.android.gm", null)).contains("google")
        assertThat(AutofillMatcher.tokensOf(null, "accounts.google.com")).contains("google")
    }

    @Test
    fun `boilerplate parts of a name identify nobody and are dropped`() {
        val tokens = AutofillMatcher.tokensOf("com.example.android.app", "www.login.example.com")
        assertThat(tokens).containsExactly("example", "wwwloginexamplecom")
    }

    @Test
    fun `the full host survives, so an entry headed with one still matches`() {
        val tokens = AutofillMatcher.tokensOf(null, "https://mail.proton.me/login")
        val previews = listOf(preview(1, "mail.proton.me"))
        val details = mapOf(1L to listOf(detail(10, 1, "Password", "pw", isSecret = true)))

        assertThat(AutofillMatcher.match(previews, details, tokens).map { it.label })
            .containsExactly("mail.proton.me")
    }

    @Test
    fun `no identifying token means no suggestions rather than every entry`() {
        val previews = listOf(preview(1, "Gmail"))
        val details = mapOf(1L to listOf(detail(10, 1, "Password", "pw", isSecret = true)))

        assertThat(AutofillMatcher.match(previews, details, emptySet())).isEmpty()
    }

    @Test
    fun `an entry with no secret is not offered`() {
        // Filling a form with a note would be worse than offering nothing.
        val previews = listOf(preview(1, "Google"))
        val details = mapOf(1L to listOf(detail(10, 1, "Note", "some text")))

        assertThat(AutofillMatcher.match(previews, details, setOf("google"))).isEmpty()
    }

    @Test
    fun `a secret with an empty value is not offered`() {
        val previews = listOf(preview(1, "Google"))
        val details = mapOf(1L to listOf(detail(10, 1, "Password", "   ", isSecret = true)))

        assertThat(AutofillMatcher.match(previews, details, setOf("google"))).isEmpty()
    }

    @Test
    fun `the username is the first non-secret value, not a keyword match`() {
        val previews = listOf(preview(1, "Google"))
        val details = mapOf(
            1L to listOf(
                detail(10, 1, "correo", "alice@example.com", sequence = 0),
                detail(11, 1, "contraseña", "s3cret", sequence = 1, isSecret = true)
            )
        )

        val credential = AutofillMatcher.match(previews, details, setOf("google")).single()
        assertThat(credential.username).isEqualTo("alice@example.com")
        assertThat(credential.password).isEqualTo("s3cret")
    }

    @Test
    fun `an older entry with no isSecret flag falls back to keyword matching`() {
        // Rows written before the isSecret column existed default to false.
        val previews = listOf(preview(1, "Google"))
        val details = mapOf(
            1L to listOf(
                detail(10, 1, "Username", "alice", sequence = 0),
                detail(11, 1, "Password", "s3cret", sequence = 1)
            )
        )

        val credential = AutofillMatcher
            .match(previews, details, setOf("google"), secretKeywords = setOf("password"))
            .single()
        assertThat(credential.password).isEqualTo("s3cret")
        assertThat(credential.username).isEqualTo("alice")
    }

    @Test
    fun `matching is case and punctuation insensitive in both directions`() {
        val previews = listOf(preview(1, "G-Mail"), preview(2, "Gmail Work Account"))
        val details = (1L..2L).associateWith {
            listOf(detail(it * 10, it, "Password", "pw", isSecret = true))
        }

        assertThat(AutofillMatcher.match(previews, details, setOf("gmail")).map { it.previewId })
            .containsExactly(1L, 2L)
    }

    @Test
    fun `an unrelated entry is not offered`() {
        val previews = listOf(preview(1, "Netflix"))
        val details = mapOf(1L to listOf(detail(10, 1, "Password", "pw", isSecret = true)))

        assertThat(AutofillMatcher.match(previews, details, setOf("google"))).isEmpty()
    }

    @Test
    fun `a one or two letter heading cannot match everything`() {
        val previews = listOf(preview(1, "X"))
        val details = mapOf(1L to listOf(detail(10, 1, "Password", "pw", isSecret = true)))

        assertThat(AutofillMatcher.match(previews, details, setOf("example"))).isEmpty()
    }

    // Look-alikes (5.8.0 review): suggesting a login to the wrong app or site is how autofill gets phished.
    private val google = listOf(preview(1, "Google"))
    private val googlePassword = mapOf(1L to listOf(detail(10, 1, "Password", "pw", isSecret = true)))

    private fun offeredTo(packageName: String?, webDomain: String?) =
        AutofillMatcher.match(google, googlePassword, AutofillMatcher.tokensOf(packageName, webDomain))

    @Test
    fun `a look-alike app name does not get another service's login`() {
        assertThat(offeredTo("com.fakegoogle.login", null)).isEmpty()
        assertThat(offeredTo("com.evil.google", null)).isEmpty()
        assertThat(offeredTo("com.google.android.gm", null)).hasSize(1)
    }

    @Test
    fun `only the registrable part of a web address identifies the site`() {
        assertThat(offeredTo(null, "google.evil.com")).isEmpty()
        assertThat(offeredTo(null, "https://login.google-account.example.net/")).isEmpty()
        assertThat(offeredTo(null, "accounts.google.co.uk")).hasSize(1)
        assertThat(offeredTo(null, "https://accounts.google.com/signin")).hasSize(1)
    }

    @Test
    fun `a browser's own name is ignored when it reports the page's address`() {
        val chrome = listOf(preview(2, "Chrome"))
        val details = mapOf(2L to listOf(detail(20, 2, "Password", "pw", isSecret = true)))
        val tokens = AutofillMatcher.tokensOf("com.android.chrome", "example.com")
        assertThat(AutofillMatcher.match(chrome, details, tokens)).isEmpty()
    }

    @Test
    fun `a heading matches by whole word, not by any substring`() {
        val previews = listOf(preview(1, "Googleplex notes"), preview(2, "My Google account"))
        val details = (1L..2L).associateWith { listOf(detail(it * 10, it, "Password", "pw", isSecret = true)) }
        assertThat(AutofillMatcher.match(previews, details, setOf("google")).map { it.previewId }).containsExactly(2L)
    }

    // Device finding B: shared hosting platforms are not one site.
    @Test
    fun `a login saved on a hosted site is not offered to other sites on the same platform`() {
        val saved = listOf(preview(1, "the-internet.herokuapp.com"))
        val details = mapOf(1L to listOf(detail(10, 1, "Password", "pw", isSecret = true)))
        listOf("evil.herokuapp.com", "https://me.github.io/login", "phish.web.app", "x.netlify.app").forEach { host ->
            assertThat(AutofillMatcher.match(saved, details, AutofillMatcher.tokensOf(null, host))).isEmpty()
        }
        val genuine = AutofillMatcher.tokensOf(null, "https://the-internet.herokuapp.com/login")
        assertThat(AutofillMatcher.match(saved, details, genuine)).hasSize(1)
    }

    @Test
    fun `the site name on a hosting platform is the label before the platform`() {
        assertThat(AutofillMatcher.tokensOf(null, "shop.example.github.io")).contains("example")
        assertThat(AutofillMatcher.tokensOf(null, "files.mybucket.s3.amazonaws.com")).contains("mybucket")
        assertThat(AutofillMatcher.tokensOf(null, "my.herokuapp.com")).doesNotContain("herokuapp")
    }
}
