package com.bhardwaj.passkey.analytics

/** Enforces Firebase Analytics limits before an event leaves the app. */
object EventValidator {
    const val MAX_NAME_LENGTH = 40
    const val MAX_PARAM_NAME_LENGTH = 40
    const val MAX_PARAMS = 25
    const val MAX_VALUE_LENGTH = 100

    private val SNAKE_CASE = Regex("^[a-z][a-z0-9_]*$")
    private val RESERVED_PREFIXES = listOf("firebase_", "google_", "ga_")

    /** Returns a list of violations; empty means valid. */
    fun validate(event: AnalyticsEvent): List<String> {
        val problems = mutableListOf<String>()
        problems += checkName(event.name, MAX_NAME_LENGTH, "event")
        if (event.params.size > MAX_PARAMS) {
            problems += "too many params (${event.params.size} > $MAX_PARAMS)"
        }
        event.params.forEach { (key, value) ->
            problems += checkName(key, MAX_PARAM_NAME_LENGTH, "param")
            when (value) {
                is String ->
                    if (value.length > MAX_VALUE_LENGTH) {
                        problems += "param '$key' value longer than $MAX_VALUE_LENGTH"
                    }
                is Int, is Long, is Double, is Float, is Boolean -> Unit
                else -> problems += "param '$key' has unsupported type ${value::class.simpleName}"
            }
        }
        return problems
    }

    private fun checkName(name: String, max: Int, kind: String): List<String> = buildList {
        if (name.length > max) add("$kind name '$name' longer than $max")
        if (!SNAKE_CASE.matches(name)) add("$kind name '$name' is not snake_case")
        if (RESERVED_PREFIXES.any { name.startsWith(it) }) add("$kind name '$name' uses a reserved prefix")
    }
}
