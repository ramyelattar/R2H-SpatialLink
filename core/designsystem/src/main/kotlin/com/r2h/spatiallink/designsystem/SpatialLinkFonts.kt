package com.r2h.spatiallink.designsystem

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * The technical layer uses a bundled, stable sans face so a device-wide font
 * theme cannot turn engineered labels into display typography.
 */
object SpatialLinkFonts {
    val Technical = FontFamily(
        Font(
            resId = R.font.spatiallink_technical_regular,
            weight = FontWeight.Normal,
        ),
    )
}
