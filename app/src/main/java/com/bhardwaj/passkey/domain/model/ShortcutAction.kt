package com.bhardwaj.passkey.domain.model

/** Home-screen icon shortcuts (6.0.0). They only ask Home to do something after the vault is unlocked. */
enum class ShortcutAction(val intentAction: String) {
    ADD_ENTRY("com.bhardwaj.passkey.action.ADD_ENTRY"),
    SEARCH("com.bhardwaj.passkey.action.SEARCH");

    companion object {
        /** Only our own actions: anything else (including the launcher's MAIN) is no shortcut. */
        fun fromIntentAction(action: String?): ShortcutAction? = entries.firstOrNull { it.intentAction == action }
    }
}
