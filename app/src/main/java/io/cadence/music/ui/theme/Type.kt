package io.cadence.music.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import io.cadence.music.R

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs,
)

private fun gFont(name: String, weight: FontWeight) =
    Font(googleFont = GoogleFont(name), fontProvider = googleFontProvider, weight = weight)

val FontInter = FontFamily(
    gFont("Inter", FontWeight.Normal),
    gFont("Inter", FontWeight.Medium),
    gFont("Inter", FontWeight.SemiBold),
    gFont("Inter", FontWeight.Bold),
)

val FontSpaceGrotesk = FontFamily(
    gFont("Space Grotesk", FontWeight.Medium),
    gFont("Space Grotesk", FontWeight.SemiBold),
    gFont("Space Grotesk", FontWeight.Bold),
)

val FontJetBrainsMono = FontFamily(
    gFont("JetBrains Mono", FontWeight.Normal),
    gFont("JetBrains Mono", FontWeight.Medium),
    gFont("JetBrains Mono", FontWeight.SemiBold),
)

val CadenceTypography = Typography(
    // Hero — Space Grotesk display
    displayLarge = TextStyle(
        fontFamily = FontSpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.6).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontSpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.4).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontSpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
    ),
    // Section headings — Inter
    headlineLarge = TextStyle(
        fontFamily = FontSpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontInter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontInter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    // Titles — Inter UI
    titleLarge = TextStyle(
        fontFamily = FontInter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontInter,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontInter,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    // Body — Inter
    bodyLarge = TextStyle(
        fontFamily = FontInter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontInter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontInter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // Labels — JetBrains Mono for telemetry-style caps
    labelLarge = TextStyle(
        fontFamily = FontJetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 2.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontJetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.6.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontJetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
    ),
)
