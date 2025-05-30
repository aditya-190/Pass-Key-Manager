package com.bhardwaj.passkey.presentation.screens.detail_screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.presentation.screens.common.PassKeyButton
import com.bhardwaj.passkey.presentation.screens.common.PassKeyTextField
import com.bhardwaj.passkey.presentation.theme.BebasNeue
import com.bhardwaj.passkey.presentation.theme.Poppins

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsBottomSheet(
    modifier: Modifier = Modifier,
    bottomSheetHeading: String,
    detailTitle: String,
    detailResponse: String,
    onTitleChange: (heading: String) -> Unit,
    onDescriptionChange: (description: String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { onDismiss() },
        dragHandle = {},
        shape = RectangleShape,

        ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
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
                    color = MaterialTheme.colorScheme.secondary,
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
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    modifier = Modifier.padding(top = 24.dp)
                )
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
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    imeAction = ImeAction.Done,
                    modifier = Modifier.padding(top = 24.dp)
                )
                PassKeyButton(
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    text = stringResource(id = R.string.save_changes),
                    onClick = { onSave() })
            }
        }
    }
}