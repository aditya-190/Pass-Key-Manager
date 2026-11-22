package com.bhardwaj.passkey.utils

import android.content.Context
import android.content.res.Configuration
import com.bhardwaj.passkey.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

/** Words that mark a field title as holding a secret ("Password", "PIN", "Contraseña", …). */
fun interface SecretKeywords {
    fun all(): Set<String>
}

/**
 * Union of the default-locale and current-locale `R.array.secret_field_keywords`. A vault may hold entries
 * labelled before the user switched language, so matching only the current locale would silently stop
 * classifying them.
 */
class ResourceSecretKeywords @Inject constructor(@param:ApplicationContext private val context: Context) :
    SecretKeywords {
    override fun all(): Set<String> {
        val current = context.resources.getStringArray(R.array.secret_field_keywords).toSet()
        val english = Configuration(context.resources.configuration).apply { setLocale(Locale.ENGLISH) }
        val fallback = context.createConfigurationContext(english)
            .resources.getStringArray(R.array.secret_field_keywords).toSet()
        return current + fallback
    }
}
