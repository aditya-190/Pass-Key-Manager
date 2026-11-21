package com.bhardwaj.passkey.analytics

/**
 * The only analytics entry point. ViewModels depend on this interface and never call Firebase directly.
 *
 * Vault rule: an event may say *that* something happened, never *what*. Param values are enum names,
 * booleans or coarse buckets built by [AppEvents]. Headings, field names, values, passwords and ids
 * must never be passed here.
 */
interface AnalyticsTracker {
    fun log(event: AnalyticsEvent)

    fun setUserProperty(name: String, value: String?)

    fun setCollectionEnabled(enabled: Boolean)
}

data class AnalyticsEvent(val name: String, val params: Map<String, Any> = emptyMap())

/** Used when Firebase isn't configured and in previews. */
object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun log(event: AnalyticsEvent) = Unit

    override fun setUserProperty(name: String, value: String?) = Unit

    override fun setCollectionEnabled(enabled: Boolean) = Unit
}
