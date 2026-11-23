package com.bhardwaj.passkey.utils

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * A piece of user-facing text that is resolved as late as possible.
 *
 * ViewModels emit these unresolved. Resolving in a ViewModel means resolving against the
 * Application context, whose Resources do not track the per-app locale override applied by
 * [androidx.appcompat.app.AppCompatDelegate.setApplicationLocales] below API 33 - so on this
 * app's minSdk 28..32 range every such string came back in the wrong language after the user
 * switched language in Settings.
 *
 * These are data classes on purpose: tests assert on emitted effects by value, which an
 * identity-based class (or a `vararg` array field) would silently break.
 */
/** A date argument, formatted only when the text is shown, in the app's own language (6.0.0). */
data class FormattedDate(val epochMs: Long)

fun formatDate(epochMs: Long, locale: Locale): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(epochMs))

private fun List<Any>.resolved(locale: Locale): List<Any> =
    map { if (it is FormattedDate) formatDate(it.epochMs, locale) else it }

sealed interface UiText {

    data class DynamicString(val value: String) : UiText

    data class StringResource(@param:StringRes val resId: Int, val args: List<Any> = emptyList()) : UiText

    data class PluralResource(@param:PluralsRes val resId: Int, val count: Int, val args: List<Any> = emptyList()) :
        UiText
}

/** Resolves against [context]. Prefer the `@Composable` overload inside composition. */
// Android's string APIs take varargs; argument lists here hold at most a few items.
@Suppress("SpreadOperator")
fun UiText.asString(context: Context): String = when (this) {
    is UiText.DynamicString -> value
    is UiText.StringResource -> context.getString(resId, *args.resolved(context.locale()).toTypedArray())
    is UiText.PluralResource ->
        context.resources.getQuantityString(resId, count, *args.resolved(context.locale()).toTypedArray())
}

private fun Context.locale(): Locale = resources.configuration.locales[0] ?: Locale.getDefault()

@Suppress("SpreadOperator")
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.DynamicString -> value
    is UiText.StringResource -> stringResource(
        resId,
        *args.resolved(LocalConfiguration.current.locales[0]).toTypedArray()
    )
    is UiText.PluralResource -> pluralStringResource(
        resId,
        count,
        *args.resolved(LocalConfiguration.current.locales[0]).toTypedArray()
    )
}
