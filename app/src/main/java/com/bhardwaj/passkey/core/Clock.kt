package com.bhardwaj.passkey.core

/** Wall-clock source. Injected so time-dependent logic (windows, cooldowns, throttles) is unit-testable. */
fun interface Clock {
    fun nowMs(): Long

    companion object {
        val System: Clock = Clock { java.lang.System.currentTimeMillis() }
    }
}
