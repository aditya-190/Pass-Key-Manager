package com.bhardwaj.passkey.presentation.views.screens.settings_screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.domain.models.Language
import com.bhardwaj.passkey.presentation.views.theme.Poppins

@Composable
fun LanguageItem(
    modifier: Modifier = Modifier,
    language: Language,
    onLanguageChange: (Language) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onLanguageChange(language)
            }
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            text = language.languageName,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = language.languageNameInEnglish,
            fontFamily = Poppins,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
        )
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1F)
        )
    }
}

@Composable
fun LanguageBottomSheet(
    modifier: Modifier = Modifier,
    onBackIconClick: () -> Unit,
    onLanguageChange: (Language) -> Unit,
) {
    val languageList = arrayListOf(
        Language(
            languageId = "en",
            languageName = "English",
            languageNameInEnglish = "English",
            comingSoon = false
        ),
        Language(
            languageId = "hi",
            languageName = "हिन्दी",
            languageNameInEnglish = "Hindi",
            comingSoon = false
        ),
        Language(
            languageId = "de",
            languageName = "Deutsch",
            languageNameInEnglish = "German",
            comingSoon = false
        ),
        Language(
            languageId = "mr",
            languageName = "मराठी",
            languageNameInEnglish = "Marathi",
            comingSoon = true
        ),
        Language(
            languageId = "bn",
            languageName = "বাংলা",
            languageNameInEnglish = "Bengali",
            comingSoon = true
        ),
        Language(
            languageId = "te",
            languageName = "తెలుగు",
            languageNameInEnglish = "Telugu",
            comingSoon = true
        ),
        Language(
            languageId = "ta",
            languageName = "தமிழ்",
            languageNameInEnglish = "Tamil",
            comingSoon = true
        ),
        Language(
            languageId = "gu",
            languageName = "ગુજરાતી",
            languageNameInEnglish = "Gujarati",
            comingSoon = true
        ),
        Language(
            languageId = "zh",
            languageName = "中文",
            languageNameInEnglish = "Chinese",
            comingSoon = true
        ),
        Language(
            languageId = "es",
            languageName = "Español",
            languageNameInEnglish = "Spanish",
            comingSoon = true
        ),
        Language(
            languageId = "pt",
            languageName = "Português",
            languageNameInEnglish = "Portuguese",
            comingSoon = true
        ),
        Language(
            languageId = "ru",
            languageName = "Русский",
            languageNameInEnglish = "Russian",
            comingSoon = true
        ),
        Language(
            languageId = "ja",
            languageName = "日本語",
            languageNameInEnglish = "Japanese",
            comingSoon = true
        ),
        Language(
            languageId = "ko",
            languageName = "한국어",
            languageNameInEnglish = "Korean",
            comingSoon = true
        ),
        Language(
            languageId = "fr",
            languageName = "Français",
            languageNameInEnglish = "French",
            comingSoon = true
        ),
        Language(
            languageId = "it",
            languageName = "Italiano",
            languageNameInEnglish = "Italian",
            comingSoon = true
        ),
    )

    Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        Icon(
            modifier = Modifier
                .padding(16.dp)
                .clickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = null,
                    onClick = { onBackIconClick() }
                ),
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back Icon"
        )
        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = stringResource(id = R.string.change),
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
        )
        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = stringResource(id = R.string.language),
            fontFamily = Poppins,
            fontWeight = FontWeight.Normal,
            fontSize = 26.sp,
        )
        LazyColumn {
            items(languageList) { single ->
                LanguageItem(language = single, onLanguageChange = onLanguageChange)
            }
        }
    }
}