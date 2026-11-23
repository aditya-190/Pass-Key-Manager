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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
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
import androidx.compose.ui.window.SecureFlagPolicy
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.domain.model.PasswordCharacterClass
import com.bhardwaj.passkey.domain.model.PasswordPolicy
import com.bhardwaj.passkey.presentation.theme.BebasNeue
import com.bhardwaj.passkey.presentation.theme.Poppins
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordSettingsSheet(
    policy: PasswordPolicy,
    onDismissPasswordSettings: () -> Unit,
    onPasswordLengthChange: (length: Int) -> Unit,
    onToggle: (PasswordCharacterClass, checked: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        properties = ModalBottomSheetProperties(
            securePolicy = SecureFlagPolicy.SecureOn
        ),
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

            Text(
                text = stringResource(R.string.password_length, policy.length),
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Slider(
                value = policy.length.toFloat(),
                onValueChange = { onPasswordLengthChange(it.roundToInt()) },
                valueRange = PasswordPolicy.MIN_LENGTH.toFloat()..PasswordPolicy.MAX_LENGTH.toFloat(),
                steps = PasswordPolicy.MAX_LENGTH - PasswordPolicy.MIN_LENGTH - 1,
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
            PasswordToggleRow(
                "a-z",
                policy.includeLowercase
            ) { onToggle(PasswordCharacterClass.LOWERCASE, it) }
            PasswordToggleRow(
                "0-9",
                policy.includeDigits
            ) { onToggle(PasswordCharacterClass.DIGITS, it) }
            PasswordToggleRow(
                "!@#",
                policy.includeSymbols
            ) { onToggle(PasswordCharacterClass.SYMBOLS, it) }
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
