package com.bhardwaj.passkey

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bhardwaj.passkey.data.security.DefaultKeyDerivation
import com.bhardwaj.passkey.data.security.KdfSpec
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Base64

/**
 * PBKDF2-HMAC-SHA512 for the recovery-password slot (owner decision: no native Argon2 dependency).
 * Instrumented because the derivation uses android.util.Base64 for its stored salt.
 */
@RunWith(AndroidJUnit4::class)
class KeyDerivationTest {

    private val derivation = DefaultKeyDerivation()

    private fun pbkdf2(salt: ByteArray, iterations: Int = 1_000) = KdfSpec.Pbkdf2HmacSha512(
        saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP),
        iterations = iterations
    )

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    @Test
    fun matches_the_reference_vector() {
        // Independent value: python3 hashlib.pbkdf2_hmac('sha512', b'password', b'salt', 1, 32).
        val key = derivation.derive("password".toCharArray(), pbkdf2("salt".toByteArray(), iterations = 1))
        assertThat(hex(key)).isEqualTo("867f70cf1ade02cff3752599a3a53dc4af34c7a669815ae5d513554e1c8cf252")
    }

    @Test
    fun non_ascii_passwords_match_their_utf8_reference() {
        val key = derivation.derive("pÿssw0rd-éü-中文".toCharArray(), pbkdf2("saltsaltsaltsalt".toByteArray(), iterations = 2))
        assertThat(hex(key)).isEqualTo("e5e1930941e36ffd68fd71fef55ffc44a4916940e5dec88cb4ca863452c9af4f")
    }

    @Test
    fun differs_for_a_different_password_or_salt() {
        val salt = derivation.newSalt()
        val a = derivation.derive("correct horse".toCharArray(), pbkdf2(salt))
        assertThat(a).isNotEqualTo(derivation.derive("incorrect horse".toCharArray(), pbkdf2(salt)))
        assertThat(a).isNotEqualTo(derivation.derive("correct horse".toCharArray(), pbkdf2(derivation.newSalt())))
        assertThat(a).hasLength(32)
    }

    @Test
    fun production_cost_is_the_backup_cost() {
        assertThat(KdfSpec.Pbkdf2HmacSha512.DEFAULT_ITERATIONS).isAtLeast(600_000)
    }

    @Test
    fun salts_are_unique() {
        val salts = List(50) { derivation.newSalt().toList() }
        assertThat(salts.toSet()).hasSize(50)
    }
}
