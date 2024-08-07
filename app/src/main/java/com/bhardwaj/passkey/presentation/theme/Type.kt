package com.bhardwaj.passkey.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bhardwaj.passkey.R

// Set of Material typography styles to start with
val Poppins = FontFamily(
    Font(R.font.poppins, weight = FontWeight.Normal),
    Font(R.font.poppins_medium, weight = FontWeight.Medium),
    Font(R.font.poppins_bold, weight = FontWeight.Bold),
)

val BebasNeue = FontFamily(
    Font(R.font.bebas_neue, weight = FontWeight.Normal),
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)