package com.bhardwaj.passkey.domain.model

/** Light/dark choice (6.0.0). The colours themselves never change; only which of the two schemes is used. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /** Never [valueOf]: a stored string may come from a newer version. */
        fun fromNameOrSystem(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
