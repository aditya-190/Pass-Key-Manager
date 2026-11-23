package com.bhardwaj.passkey.core

/** Only https links built from remote data may leave the app (no intent:, javascript:, file: or plain http). */
object SafeUrl {
    fun isHttps(url: String?): Boolean = url != null &&
        url.startsWith("https://", ignoreCase = true) &&
        url.length > "https://".length &&
        url.none { it.isWhitespace() }
}
