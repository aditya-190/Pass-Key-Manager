package com.bhardwaj.passkey.domain.model

/** Light/dark choice (6.0.0). The colours themselves never change; only which of the two schemes is used. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    /**
     * The value for UiModeManager.setApplicationNightMode (API 31+): AUTO 0, NO 1, YES 2. Telling the system lets
     * the launch screen and every window (the autofill prompt too) start in the chosen scheme.
     */
    fun nightMode(): Int = ordinal

    companion object {
        /** Never [valueOf]: a stored string may come from a newer version. */
        fun fromNameOrSystem(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
