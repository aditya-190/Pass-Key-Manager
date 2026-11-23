package com.bhardwaj.passkey.presentation.screens.detail_screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.presentation.screens.common.PassKeyButton
import com.bhardwaj.passkey.presentation.screens.common.PassKeyTextField
import com.bhardwaj.passkey.presentation.screens.common.SecureModalBottomSheet
import com.bhardwaj.passkey.presentation.theme.BebasNeue
import com.bhardwaj.passkey.presentation.theme.Poppins

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsBottomSheet(
    modifier: Modifier = Modifier,
    bottomSheetHeading: String,
    detailTitle: String,
    detailResponse: String,
    isSecret: Boolean,
    error: String?,
    onSecretToggle: (Boolean) -> Unit,
    onTitleChange: (heading: String) -> Unit,
    onDescriptionChange: (description: String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onGeneratePasswordClicked: () -> Unit,
    onPasswordSettingsClicked: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    SecureModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { onDismiss() },
        dragHandle = {},
        shape = RectangleShape
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = bottomSheetHeading,
                    fontFamily = BebasNeue,
                    fontWeight = FontWeight.Normal,
                    fontSize = 54.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                PassKeyTextField(
                    value = detailTitle,
                    onValueChanged = { onTitleChange(it) },
                    headingName = stringResource(id = R.string.title),
                    placeHolderText = stringResource(id = R.string.type_your_title_here),
                    textStyle = TextStyle(
                        fontFamily = Poppins,
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Normal,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.padding(top = 24.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PassKeyTextField(
                        value = detailResponse,
                        onValueChanged = { onDescriptionChange(it) },
                        headingName = stringResource(id = R.string.response),
                        placeHolderText = stringResource(id = R.string.type_your_response_here),
                        textStyle = TextStyle(
                            fontFamily = Poppins,
                            fontSize = 16.sp,
                            fontStyle = FontStyle.Normal,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        imeAction = ImeAction.Done,
                        // Secrets get a password keyboard, which never learns or suggests what is typed;
                        // usernames and notes keep a normal keyboard with Enter and suggestions.
                        isSecret = isSecret,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    FilledIconButton(
                        onClick = { onGeneratePasswordClicked() },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = stringResource(R.string.cd_generate_password),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    FilledIconButton(
                        onClick = { onPasswordSettingsClicked() },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_password_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // One switch for the whole row, read by TalkBack as "Secret, switch, on".
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(value = isSecret, role = Role.Switch, onValueChange = onSecretToggle)
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.secret_field_toggle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = isSecret, onCheckedChange = null)
                }
                // Validation stays in the sheet, so nothing the user typed is lost.
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                PassKeyButton(
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    text = stringResource(id = R.string.save_changes),
                    onClick = { onSave() }
                )
            }
        }
    }
}
