package com.example.contadorhoras.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.contadorhoras.R

// Titulares
private val Anton = FontFamily(
    Font(R.font.anton_regular, weight = FontWeight.Normal)
)

// Cuerpo y etiquetas (todas las variantes de Open Sans que tengas)
private val OpenSans = FontFamily(
    Font(R.font.opensans_light,     weight = FontWeight.Light),
    Font(R.font.opensans_regular,   weight = FontWeight.Normal),
    Font(R.font.opensans_medium,    weight = FontWeight.Medium),
    Font(R.font.opensans_semibold,  weight = FontWeight.SemiBold),
    Font(R.font.opensans_bold,      weight = FontWeight.Bold),
)

val AppTypography = Typography(
    // Titulares (Anton)
    displayLarge   = TextStyle(fontFamily = Anton,    fontWeight = FontWeight.Normal, fontSize = 48.sp, lineHeight = 52.sp),
    displayMedium  = TextStyle(fontFamily = Anton,    fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 44.sp),
    headlineLarge  = TextStyle(fontFamily = Anton,    fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Anton,    fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 32.sp),
    titleLarge     = TextStyle(fontFamily = Anton,    fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 26.sp),
    titleMedium    = TextStyle(fontFamily = Anton,    fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 22.sp),

    // Cuerpos (Open Sans)
    bodyLarge  = TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.Normal,  fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.Normal,  fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall  = TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.Normal,  fontSize = 12.sp, lineHeight = 16.sp),

    // Labels/Chips/Botones (Open Sans, algo más marcadas)
    labelLarge  = TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall  = TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 14.sp)
)
