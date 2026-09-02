package com.r2h.spatiallink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.r2h.spatiallink.designsystem.SpatialLinkColors

private val SpatialLinkDarkColors = darkColorScheme(
    primary = SpatialLinkColors.IceCyan,
    onPrimary = SpatialLinkColors.Obsidian,
    primaryContainer = SpatialLinkColors.CarbonEdge,
    onPrimaryContainer = SpatialLinkColors.WarmIvory,
    secondary = SpatialLinkColors.ChampagneBrass,
    onSecondary = SpatialLinkColors.Obsidian,
    secondaryContainer = SpatialLinkColors.Carbon,
    onSecondaryContainer = SpatialLinkColors.WarmIvory,
    tertiary = SpatialLinkColors.MutedMint,
    onTertiary = SpatialLinkColors.Obsidian,
    background = SpatialLinkColors.Obsidian,
    onBackground = SpatialLinkColors.WarmIvory,
    surface = SpatialLinkColors.Obsidian,
    onSurface = SpatialLinkColors.WarmIvory,
    surfaceVariant = SpatialLinkColors.Carbon,
    onSurfaceVariant = SpatialLinkColors.MutedIvory,
    outline = SpatialLinkColors.BrassDim,
    outlineVariant = SpatialLinkColors.CarbonEdge,
    error = SpatialLinkColors.SoftRose,
    onError = SpatialLinkColors.Obsidian,
)

private val SpatialLinkLightColors = lightColorScheme(
    primary = SpatialLinkColors.CyanDim,
    onPrimary = SpatialLinkColors.WarmIvory,
    primaryContainer = ColorTokens.LightCarbon,
    onPrimaryContainer = SpatialLinkColors.WarmIvory,
    secondary = ColorTokens.LightBrass,
    onSecondary = SpatialLinkColors.Obsidian,
    secondaryContainer = ColorTokens.LightBrassContainer,
    onSecondaryContainer = SpatialLinkColors.Obsidian,
    tertiary = ColorTokens.LightMint,
    onTertiary = SpatialLinkColors.Obsidian,
    background = ColorTokens.LightIvory,
    onBackground = SpatialLinkColors.Obsidian,
    surface = ColorTokens.LightIvory,
    onSurface = SpatialLinkColors.Obsidian,
    surfaceVariant = ColorTokens.LightSurface,
    onSurfaceVariant = ColorTokens.LightMuted,
    outline = ColorTokens.LightOutline,
    outlineVariant = ColorTokens.LightOutlineVariant,
    error = ColorTokens.LightError,
    onError = SpatialLinkColors.Obsidian,
)

private object ColorTokens {
    val LightIvory = androidx.compose.ui.graphics.Color(0xFFF4F0E9)
    val LightSurface = androidx.compose.ui.graphics.Color(0xFFEAE5DC)
    val LightCarbon = androidx.compose.ui.graphics.Color(0xFF34413F)
    val LightBrass = androidx.compose.ui.graphics.Color(0xFF725E42)
    val LightBrassContainer = androidx.compose.ui.graphics.Color(0xFFE1D5C0)
    val LightMint = androidx.compose.ui.graphics.Color(0xFF4A6B5E)
    val LightMuted = androidx.compose.ui.graphics.Color(0xFF5E625F)
    val LightOutline = androidx.compose.ui.graphics.Color(0xFF857560)
    val LightOutlineVariant = androidx.compose.ui.graphics.Color(0xFFB7ADA0)
    val LightError = androidx.compose.ui.graphics.Color(0xFF8C4F4C)
}

val SpatialLinkTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.5).sp,
        ),
        displayMedium = base.displayMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
        ),
        headlineLarge = base.headlineLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
        ),
        headlineMedium = base.headlineMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
        ),
        headlineSmall = base.headlineSmall.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
        ),
        titleLarge = base.titleLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
        ),
        titleMedium = base.titleMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.8.sp,
        ),
        titleSmall = base.titleSmall.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            letterSpacing = 1.1.sp,
        ),
        bodyLarge = base.bodyLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        bodyMedium = base.bodyMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        bodySmall = base.bodySmall.copy(
            fontFamily = FontFamily.SansSerif,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        ),
        labelLarge = base.labelLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            letterSpacing = 1.5.sp,
        ),
        labelMedium = base.labelMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            letterSpacing = 1.8.sp,
        ),
        labelSmall = base.labelSmall.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            letterSpacing = 2.sp,
        ),
    )
}

@Composable
fun SpatialLinkTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isSystemInDarkTheme()) {
        SpatialLinkDarkColors
    } else {
        SpatialLinkLightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpatialLinkTypography,
        content = content,
    )
}
