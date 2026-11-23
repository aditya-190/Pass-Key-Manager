package com.bhardwaj.passkey.presentation.screens.detail_screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.domain.model.PasswordCharacterClass
import com.bhardwaj.passkey.domain.model.PasswordPolicy
import com.bhardwaj.passkey.presentation.screens.common.SecureModalBottomSheet
import com.bhardwaj.passkey.presentation.theme.BebasNeue
import com.bhardwaj.passkey.presentation.theme.Poppins
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordSettingsSheet(
    policy: PasswordPolicy,
    onDismissPasswordSettings: () -> Unit,
    onPasswordLengthChange: (length: Int) -> Unit,
    onToggle: (PasswordCharacterClass, checked: Boolean) -> Unit,
    onPassphraseToggle: (enabled: Boolean) -> Unit = {},
    onWordsChange: (words: Int) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState()

    SecureModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { onDismissPasswordSettings() },
        dragHandle = { },
        shape = RectangleShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.password_settings),
                fontFamily = BebasNeue,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 6.0.0: words instead of characters. A-Z capitalises the words and 0-9 adds a digit; a-z and !@#
            // don't apply to words, so they step aside in this mode.
            PasswordToggleRow(stringResource(R.string.passphrase), policy.passphrase, onPassphraseToggle)
            if (policy.passphrase) {
                // Some sites cap length well below a passphrase's ~30 characters.
                Text(
                    text = stringResource(R.string.passphrase_hint),
                    fontFamily = Poppins,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            val (label, value, range) = if (policy.passphrase) {
                Triple(
                    stringResource(R.string.passphrase_words, policy.words),
                    policy.words,
                    PasswordPolicy.MIN_WORDS..PasswordPolicy.MAX_WORDS
                )
            } else {
                Triple(
                    stringResource(R.string.password_length, policy.length),
                    policy.length,
                    PasswordPolicy.MIN_LENGTH..PasswordPolicy.MAX_LENGTH
                )
            }
            Text(
                text = label,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Slider(
                value = value.toFloat(),
                onValueChange = {
                    if (policy.passphrase) onWordsChange(it.roundToInt()) else onPasswordLengthChange(it.roundToInt())
                },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = range.last - range.first - 1,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            PasswordToggleRow(
                "A-Z",
                policy.includeUppercase
            ) { onToggle(PasswordCharacterClass.UPPERCASE, it) }
            if (!policy.passphrase) {
                PasswordToggleRow(
                    "a-z",
                    policy.includeLowercase
                ) { onToggle(PasswordCharacterClass.LOWERCASE, it) }
            }
            PasswordToggleRow(
                "0-9",
                policy.includeDigits
            ) { onToggle(PasswordCharacterClass.DIGITS, it) }
            if (!policy.passphrase) {
                PasswordToggleRow(
                    "!@#",
                    policy.includeSymbols
                ) { onToggle(PasswordCharacterClass.SYMBOLS, it) }
            }
        }
    }
}

@Composable
fun PasswordToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = Poppins,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.background,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
