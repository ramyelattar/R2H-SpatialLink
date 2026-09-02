package com.r2h.spatiallink.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SpatialAtmosphere(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SpatialLinkColors.CarbonLift.copy(alpha = 0.28f),
                        SpatialLinkColors.Carbon.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.84f, size.height * 0.28f),
                    radius = size.maxDimension * 0.86f,
                ),
            )

            drawCircle(
                color = SpatialLinkColors.BrassDim.copy(alpha = 0.07f),
                center = Offset(size.width * 0.82f, size.height * 0.38f),
                radius = size.maxDimension * 0.64f,
                style = Stroke(width = 1.dp.toPx()),
            )

            drawLine(
                color = SpatialLinkColors.TechnicalGrid.copy(alpha = 0.25f),
                start = Offset(size.width * 0.08f, 0f),
                end = Offset(size.width * 0.08f, size.height),
                strokeWidth = 1.dp.toPx(),
            )

            drawLine(
                color = SpatialLinkColors.TechnicalGrid.copy(alpha = 0.12f),
                start = Offset(0f, size.height * 0.36f),
                end = Offset(size.width, size.height * 0.36f),
                strokeWidth = 1.dp.toPx(),
            )

            repeat(34) { index ->
                val x = ((index * 83) % 101) / 100f * size.width
                val y = ((index * 47 + 19) % 101) / 100f * size.height

                drawCircle(
                    color = SpatialLinkColors.CarbonEdgeLight.copy(alpha = 0.08f),
                    radius = 0.45.dp.toPx(),
                    center = Offset(x, y),
                )
            }
        }

        content()
    }
}
