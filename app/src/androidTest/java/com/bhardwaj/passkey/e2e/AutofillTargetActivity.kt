package com.bhardwaj.passkey.e2e

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout

/**
 * A username/password form in the test APK, so autofill can be exercised on an emulator that has no browser or
 * other login screen: `adb shell am start -n com.bhardwaj.passkey.test/com.bhardwaj.passkey.e2e.AutofillTargetActivity`.
 */
class AutofillTargetActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = EditText(this).apply {
            hint = "Username"
            setAutofillHints(View.AUTOFILL_HINT_USERNAME)
        }
        val password = EditText(this).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setAutofillHints(View.AUTOFILL_HINT_PASSWORD)
        }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(PADDING, PADDING, PADDING, PADDING)
                addView(user)
                addView(password)
            }
        )
    }

    private companion object {
        const val PADDING = 48
    }
}
