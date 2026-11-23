package com.bhardwaj.passkey.presentation.screens.settings_screen.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.utils.PasswordStrength
import com.bhardwaj.passkey.utils.Strength

private const val MIN_BACKUP_PASSWORD_LENGTH = 8

/**
 * Collects the password protecting a backup file.
 *
 * In [confirmMode] (export) the password is entered twice, because there is no recovery path:
 * the file is useless without it. On restore a single field is shown.
 */
@Composable
fun BackupPasswordDialog(
    confirmMode: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit,
    @StringRes titleRes: Int? = null,
    /** Field label; the vault gate passes the recovery-password label instead of "Backup password". */
    @StringRes hintRes: Int = R.string.backup_password_hint,
    @StringRes messageRes: Int? = null,
    /** False for gate dialogs that the user must not be able to escape. */
    dismissible: Boolean = true,
    /**
     * Recovery passwords: anyone holding the phone's files can guess them offline, so they need
     * [PasswordStrength.MIN_RECOVERY_LENGTH] characters, must not be obviously weak, and show a strength meter.
     */
    recoveryRules: Boolean = false
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var showValidation by remember { mutableStateOf(false) }

    val minLength = if (recoveryRules) PasswordStrength.MIN_RECOVERY_LENGTH else MIN_BACKUP_PASSWORD_LENGTH
    val tooShort = password.codePointCount(0, password.length) < minLength
    val strength = PasswordStrength.of(password)
    val tooWeak = recoveryRules && strength == Strength.WEAK
    val mismatch = confirmMode && password != confirmation
    val error = when {
        !showValidation -> null
        confirmMode && tooShort && recoveryRules ->
            pluralStringResource(R.plurals.password_min_length, minLength, minLength)
        confirmMode && tooShort -> stringResource(R.string.backup_password_too_short)
        confirmMode && tooWeak -> stringResource(R.string.recovery_password_too_weak)
        mismatch -> stringResource(R.string.backup_password_mismatch)
        else -> null
    }

    AlertDialog(
        // The dialog is its own window, so it needs FLAG_SECURE in its own right.
        properties = DialogProperties(
            securePolicy = SecureFlagPolicy.SecureOn,
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible
        ),
        onDismissRequest = { if (dismissible) onDismiss() },
        title = {
            Text(
                text = stringResource(
                    titleRes ?: if (confirmMode) {
                        R.string.backup_password_title
                    } else {
                        R.string.restore_password_title
                    }
                )
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(
                        messageRes ?: if (confirmMode) {
                            R.string.backup_password_message
                        } else {
                            R.string.restore_password_message
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    modifier = Modifier.padding(top = 16.dp),
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    isError = error != null,
                    label = { Text(stringResource(hintRes)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (confirmMode) ImeAction.Next else ImeAction.Done
                    )
                )
                if (recoveryRules && confirmMode && password.isNotEmpty()) {
                    StrengthMeter(strength)
                }
                if (confirmMode) {
                    OutlinedTextField(
                        modifier = Modifier.padding(top = 8.dp),
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        singleLine = true,
                        isError = error != null,
                        label = { Text(stringResource(R.string.backup_password_confirm_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )
                }
                error?.let {
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                showValidation = true
                val valid = if (confirmMode) !tooShort && !tooWeak && !mismatch else password.isNotEmpty()
                if (valid) onConfirm(password.toCharArray())
            }) { Text(stringResource(R.string.backup_confirm)) }
        },
        dismissButton = {
            if (dismissible) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}

/** Text and bar together, so colour is never the only signal; announced politely as it changes. */
@Composable
private fun StrengthMeter(strength: Strength) {
    val (label, fraction, color) = when (strength) {
        Strength.WEAK -> Triple(R.string.password_strength_weak, 0.33f, MaterialTheme.colorScheme.error)
        Strength.FAIR -> Triple(R.string.password_strength_fair, 0.66f, MaterialTheme.colorScheme.tertiary)
        Strength.STRONG -> Triple(R.string.password_strength_strong, 1f, MaterialTheme.colorScheme.primary)
    }
    Column(modifier = Modifier.padding(top = 8.dp)) {
        LinearProgressIndicator(progress = { fraction }, color = color, modifier = Modifier.fillMaxWidth())
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(top = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}
