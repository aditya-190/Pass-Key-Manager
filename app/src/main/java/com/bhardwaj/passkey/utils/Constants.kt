package com.bhardwaj.passkey.utils

class Constants {
    companion object {
        const val DETAILS_TABLE = "details_table"
        const val PREVIEW_TABLE = "preview_table"
        const val PASS_KEY_DATABASE = "passkey_database"
        const val HEADING_NAME = "headingName"
        const val CATEGORY_NAME = "categoryName"
        const val FILE_NAME = "passkey_backup"
        const val FILE_TYPE = "passkey"
        const val FILE_PICKER_TYPE = "application/passkey"
        const val FILE_HEADER = "category,heading,question,answer\n"

        // SaveState Constants.
        const val PREVIEW_HEADING = "preview_heading"
        const val PREVIEW_CATEGORY_NAME = "preview_category_name"
        const val DETAIL_TITLE = "detail_title"
        const val DETAIL_RESPONSE = "detail_response"
        const val BOTTOM_SHEET_HEADING = "bottom_sheet_heading"

        // DataStore Prefs Constants.
        const val PASSKEY_PREFS = "passkey_pref"
        const val ONBOARDING_COMPLETE = "onboarding_complete"
        const val CURRENT_LANGUAGE = "current_language"
    }
}