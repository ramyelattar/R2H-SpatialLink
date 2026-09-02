package com.r2h.spatiallink.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun SpatialField(
    status: StatusMarkModel,
    modifier: Modifier = Modifier,
    contentDescription: String = "Spatial field ${status.label.lowercase()}",
) {
    val transition = rememberInfiniteTransition(label = "spatial field")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32000, easing = LinearEasing),
        ),
        label = "spatial field sweep",
    )
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 280.dp, minHeight = 280.dp)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawFieldInstrument(status = status, rotation = rotation)
        }
        TrustSeal(
            status = status,
            modifier = Modifier.size(252.dp),
            contentDescription = "Spatial field trust seal",
        )
    }
}

@Composable
fun SpatialRing(
    modifier: Modifier = Modifier,
    color: Color = SpatialLinkColors.BrassEdge,
    sweep: Float = 300f,
) {
    Canvas(modifier = modifier) {
        val radius = min(size.width, size.height) * 0.42f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = SpatialLinkColors.BrassAntique.copy(alpha = 0.5f),
            radius = radius,
            center = center,
            style = Stroke(2.dp.toPx()),
        )
        drawArc(
            brush = Brush.sweepGradient(
                listOf(
                    SpatialLinkColors.BronzeBlack,
                    SpatialLinkColors.BrassAntique,
                    SpatialLinkColors.BronzeOxide,
                    color,
                    SpatialLinkColors.BrassAntique,
                ),
                center = center,
            ),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
fun TrustSeal(
    status: StatusMarkModel,
    shortId: String? = null,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription ?: "Trust seal ${status.label.lowercase()}"
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) * 0.46f
            val sealMaterial = Brush.sweepGradient(
                colors = listOf(
                    SpatialLinkColors.BronzeBlack,
                    SpatialLinkColors.BrassAntique,
                    SpatialLinkColors.BronzeOxide,
                    SpatialLinkColors.BrassEdge,
                    SpatialLinkColors.BrassAntique,
                    SpatialLinkColors.BronzeBlack,
                ),
                center = center,
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.84f),
                radius = radius + 8.dp.toPx(),
                center = center + Offset(0f, 5.dp.toPx()),
            )
            drawCircle(
                color = SpatialLinkColors.BronzeBlack,
                radius = radius + 4.dp.toPx(),
                center = center + Offset(0f, 1.5.dp.toPx()),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SpatialLinkColors.CarbonEdgeLight.copy(alpha = 0.48f),
                        SpatialLinkColors.CarbonLift,
                        SpatialLinkColors.BronzeBlack,
                        SpatialLinkColors.Obsidian,
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius * 0.99f,
                center = center,
            )
            drawCircle(
                color = SpatialLinkColors.BronzeDeep.copy(alpha = 0.92f),
                radius = radius * 0.97f,
                center = center + Offset(0f, 1.5.dp.toPx()),
                style = Stroke(width = 6.dp.toPx()),
            )
            drawCircle(
                color = SpatialLinkColors.BrassAntique,
                radius = radius * 0.955f,
                center = center,
                style = Stroke(width = 4.6.dp.toPx()),
            )
            drawCircle(
                brush = sealMaterial,
                radius = radius * 0.95f,
                center = center,
                style = Stroke(width = 2.5.dp.toPx()),
            )
            drawArc(
                color = SpatialLinkColors.BrassPeak.copy(alpha = 0.42f),
                startAngle = 206f,
                sweepAngle = 86f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.95f, center.y - radius * 0.95f),
                size = androidx.compose.ui.geometry.Size(radius * 1.9f, radius * 1.9f),
                style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = SpatialLinkColors.BrassShadow.copy(alpha = 0.76f),
                startAngle = 24f,
                sweepAngle = 108f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.95f, center.y - radius * 0.95f),
                size = androidx.compose.ui.geometry.Size(radius * 1.9f, radius * 1.9f),
                style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SpatialLinkColors.CarbonLift.copy(alpha = 0.9f),
                        SpatialLinkColors.CarbonDeep.copy(alpha = 0.98f),
                        SpatialLinkColors.Obsidian,
                    ),
                    center = center + Offset(-radius * 0.08f, -radius * 0.1f),
                    radius = radius * 0.84f,
                ),
                radius = radius * 0.84f,
                center = center,
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.7f),
                radius = radius * 0.84f,
                center = center + Offset(0f, 2.5.dp.toPx()),
                style = Stroke(width = 3.2.dp.toPx()),
            )
            drawCircle(
                color = SpatialLinkColors.BronzeOxide.copy(alpha = 0.82f),
                radius = radius * 0.78f,
                center = center,
                style = Stroke(width = 1.3.dp.toPx()),
            )
            drawCircle(
                color = SpatialLinkColors.CyanDim.copy(alpha = 0.16f),
                radius = radius * 0.72f,
                center = center,
                style = Stroke(width = 5.5.dp.toPx()),
            )
            drawArc(
                color = SpatialLinkColors.CyanGlow.copy(alpha = 0.76f),
                startAngle = 206f,
                sweepAngle = 218f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.72f, center.y - radius * 0.72f),
                size = androidx.compose.ui.geometry.Size(radius * 1.44f, radius * 1.44f),
                style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        SpatialLinkColors.CyanDim.copy(alpha = 0.18f),
                        SpatialLinkColors.CyanCore.copy(alpha = 0.94f),
                        SpatialLinkColors.CyanGlow.copy(alpha = 0.45f),
                        SpatialLinkColors.CyanDim.copy(alpha = 0.18f),
                    ),
                    center = center,
                ),
                startAngle = -48f,
                sweepAngle = 112f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.72f, center.y - radius * 0.72f),
                size = androidx.compose.ui.geometry.Size(radius * 1.44f, radius * 1.44f),
                style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round),
            )
            repeat(36) { index ->
                val angle = Math.toRadians((index * 10f - 90f).toDouble())
                val outer = radius * 0.7f
                val inner = outer - when {
                    index % 9 == 0 -> 8.dp.toPx()
                    index % 3 == 0 -> 5.dp.toPx()
                    else -> 3.dp.toPx()
                }
                val tick = when {
                    index % 9 == 0 -> SpatialLinkColors.BrassEdge.copy(alpha = 0.82f)
                    index % 3 == 0 -> SpatialLinkColors.BrassShadow.copy(alpha = 0.74f)
                    else -> SpatialLinkColors.TechnicalGrid.copy(alpha = 0.78f)
                }
                drawLine(
                    color = tick,
                    start = Offset(
                        center.x + cos(angle).toFloat() * inner,
                        center.y + sin(angle).toFloat() * inner,
                    ),
                    end = Offset(
                        center.x + cos(angle).toFloat() * outer,
                        center.y + sin(angle).toFloat() * outer,
                    ),
                    strokeWidth = when {
                        index % 9 == 0 -> 1.35.dp.toPx()
                        index % 3 == 0 -> 0.95.dp.toPx()
                        else -> 0.62.dp.toPx()
                    },
                )
            }
            val cardinalInner = radius * 0.6f
            val cardinalOuter = radius * 0.76f
            drawLine(
                color = SpatialLinkColors.BrassEdge.copy(alpha = 0.72f),
                start = Offset(center.x, center.y - cardinalInner),
                end = Offset(center.x, center.y - cardinalOuter),
                strokeWidth = 1.2.dp.toPx(),
            )
            drawLine(
                color = SpatialLinkColors.BrassShadow.copy(alpha = 0.82f),
                start = Offset(center.x, center.y + cardinalInner),
                end = Offset(center.x, center.y + cardinalOuter),
                strokeWidth = 1.2.dp.toPx(),
            )
            drawLine(
                color = SpatialLinkColors.BrassDim.copy(alpha = 0.42f),
                start = Offset(center.x - cardinalOuter, center.y),
                end = Offset(center.x - cardinalInner, center.y),
                strokeWidth = 1.1.dp.toPx(),
            )
            drawLine(
                color = SpatialLinkColors.BrassEdge.copy(alpha = 0.62f),
                start = Offset(center.x + cardinalInner, center.y),
                end = Offset(center.x + cardinalOuter, center.y),
                strokeWidth = 1.1.dp.toPx(),
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.72f),
                radius = radius * 0.49f,
                center = center + Offset(0f, 3.dp.toPx()),
                style = Stroke(width = 5.dp.toPx()),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SpatialLinkColors.CarbonLift, SpatialLinkColors.CarbonDeep),
                    center = center + Offset(-radius * 0.1f, -radius * 0.12f),
                    radius = radius * 0.47f,
                ),
                radius = radius * 0.47f,
                center = center,
            )
            drawCircle(
                color = SpatialLinkColors.BrassAntique,
                radius = radius * 0.47f,
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
            repeat(12) { index ->
                val angle = Math.toRadians((index * 30f - 90f).toDouble())
                val inner = radius * 0.385f
                val outer = radius * 0.435f
                val tickColor = when {
                    index % 3 == 0 -> SpatialLinkColors.BrassEdge.copy(alpha = 0.58f)
                    else -> SpatialLinkColors.BrassShadow.copy(alpha = 0.42f)
                }
                drawLine(
                    color = Color.Black.copy(alpha = 0.52f),
                    start = Offset(
                        center.x + cos(angle).toFloat() * (inner + 1.3.dp.toPx()),
                        center.y + sin(angle).toFloat() * (inner + 1.3.dp.toPx()),
                    ),
                    end = Offset(
                        center.x + cos(angle).toFloat() * (outer + 1.3.dp.toPx()),
                        center.y + sin(angle).toFloat() * (outer + 1.3.dp.toPx()),
                    ),
                    strokeWidth = 1.4.dp.toPx(),
                )
                drawLine(
                    color = tickColor,
                    start = Offset(
                        center.x + cos(angle).toFloat() * inner,
                        center.y + sin(angle).toFloat() * inner,
                    ),
                    end = Offset(
                        center.x + cos(angle).toFloat() * outer,
                        center.y + sin(angle).toFloat() * outer,
                    ),
                    strokeWidth = if (index % 3 == 0) 0.85.dp.toPx() else 0.55.dp.toPx(),
                )
            }
            drawCircle(
                color = SpatialLinkColors.CyanGlow.copy(alpha = 0.16f),
                radius = 15.dp.toPx(),
                center = center,
            )
            drawCircle(
                color = SpatialLinkColors.CyanCore.copy(alpha = 0.22f),
                radius = 4.2.dp.toPx(),
                center = center,
            )
            drawCircle(
                color = SpatialLinkColors.CyanCore.copy(alpha = 0.9f),
                radius = 1.6.dp.toPx(),
                center = center,
            )
            drawDiamond(
                center = center,
                radius = radius * 0.29f,
                color = SpatialLinkColors.BrassEdge.copy(alpha = 0.58f),
                strokeWidth = 1.15.dp.toPx(),
            )
            drawTrustMonogram(
                center = center,
                radius = radius * 0.44f,
                color = SpatialLinkColors.BrassPeak,
            )
            drawShieldOutline(
                center = center + Offset(0f, radius * 0.34f),
                radius = radius * 0.09f,
                color = SpatialLinkColors.BrassEdge.copy(alpha = 0.88f),
                strokeWidth = 1.1.dp.toPx(),
            )
        }
        shortId?.let {
            Text(
                text = it,
                color = SpatialLinkColors.MutedIvory,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = SpatialLinkFonts.Technical,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.9.sp,
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
internal fun SpatialEntryGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = SpatialLinkColors.CyanDim.copy(alpha = 0.12f),
            radius = size.minDimension * 0.45f,
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.55f),
            radius = size.minDimension * 0.28f,
            center = center,
        )
        drawLine(
            color = SpatialLinkColors.BrassShadow.copy(alpha = 0.72f),
            start = Offset(center.x, center.y - size.height * 0.46f),
            end = Offset(center.x, center.y + size.height * 0.46f),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = SpatialLinkColors.BrassShadow.copy(alpha = 0.72f),
            start = Offset(center.x - size.width * 0.46f, center.y),
            end = Offset(center.x + size.width * 0.46f, center.y),
            strokeWidth = 1.dp.toPx(),
        )
        drawArc(
            color = SpatialLinkColors.BrassEdge.copy(alpha = 0.78f),
            startAngle = 198f,
            sweepAngle = 144f,
            useCenter = false,
            topLeft = Offset(size.width * 0.15f, size.height * 0.15f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.7f, size.height * 0.7f),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            color = SpatialLinkColors.CyanCore.copy(alpha = 0.68f),
            startAngle = 270f,
            sweepAngle = 88f,
            useCenter = false,
            topLeft = Offset(size.width * 0.22f, size.height * 0.22f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.56f),
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(
            color = SpatialLinkColors.CyanCore.copy(alpha = 0.9f),
            radius = size.minDimension * 0.09f,
            center = center,
        )
    }
}

private fun DrawScope.drawFieldInstrument(
    status: StatusMarkModel,
    rotation: Float,
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = min(size.width, size.height) * 0.49f
    val brassMaterial = Brush.sweepGradient(
        listOf(
            SpatialLinkColors.BronzeBlack,
            SpatialLinkColors.BrassAntique,
            SpatialLinkColors.BronzeOxide,
            SpatialLinkColors.BrassEdge,
            SpatialLinkColors.BrassAntique,
            SpatialLinkColors.BronzeBlack,
        ),
        center = center,
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                SpatialLinkColors.CarbonLift.copy(alpha = 0.88f),
                SpatialLinkColors.BronzeBlack.copy(alpha = 0.92f),
                SpatialLinkColors.Obsidian.copy(alpha = 0.98f),
            ),
            center = center,
            radius = radius,
        ),
        radius = radius * 0.98f,
        center = center,
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.68f),
        radius = radius * 0.985f,
        center = center + Offset(0f, 3.dp.toPx()),
        style = Stroke(width = 8.dp.toPx()),
    )
    drawCircle(
        color = SpatialLinkColors.CarbonEdgeLight.copy(alpha = 0.24f),
        radius = radius * 0.975f,
        center = center,
        style = Stroke(width = 1.dp.toPx()),
    )

    drawLine(
        color = SpatialLinkColors.BrassDim.copy(alpha = 0.24f),
        start = Offset(0f, center.y),
        end = Offset(size.width, center.y),
        strokeWidth = 1.dp.toPx(),
    )
    drawLine(
        color = SpatialLinkColors.BrassDim.copy(alpha = 0.2f),
        start = Offset(center.x, 0f),
        end = Offset(center.x, size.height),
        strokeWidth = 1.dp.toPx(),
    )
    drawLine(
        color = SpatialLinkColors.TechnicalGrid.copy(alpha = 0.2f),
        start = Offset(center.x - radius * 0.72f, center.y - radius * 0.72f),
        end = Offset(center.x + radius * 0.72f, center.y + radius * 0.72f),
        strokeWidth = 1.dp.toPx(),
    )
    drawLine(
        color = SpatialLinkColors.TechnicalGrid.copy(alpha = 0.14f),
        start = Offset(center.x + radius * 0.72f, center.y - radius * 0.72f),
        end = Offset(center.x - radius * 0.72f, center.y + radius * 0.72f),
        strokeWidth = 1.dp.toPx(),
    )

    val recessedRings = listOf(
        0.95f to 0.55f,
        0.88f to 0.8f,
        0.72f to 0.65f,
        0.65f to 0.5f,
        0.52f to 0.55f,
    )
    recessedRings.forEach { (ratio, strokeWidth) ->
        val ringRadius = radius * ratio
        drawCircle(
            color = Color.Black.copy(alpha = 0.42f),
            radius = ringRadius + 2.dp.toPx(),
            center = center + Offset(0f, 2.dp.toPx()),
            style = Stroke(width = (strokeWidth + 1.2f).dp.toPx()),
        )
        drawCircle(
            color = SpatialLinkColors.BrassAntique.copy(alpha = 0.28f),
            radius = ringRadius,
            center = center,
            style = Stroke(width = strokeWidth.dp.toPx()),
        )
    }

    val structuralRings = listOf(
        0.9f to 6.8f,
        0.79f to 4.2f,
        0.6f to 5.4f,
        0.42f to 3.4f,
    )
    structuralRings.forEachIndexed { index, (ratio, strokeWidth) ->
        val ringRadius = radius * ratio
        drawCircle(
            color = Color.Black.copy(alpha = 0.7f),
            radius = ringRadius + 2.dp.toPx(),
            center = center + Offset(0f, 3.dp.toPx()),
            style = Stroke(width = (strokeWidth + 2.2f).dp.toPx()),
        )
        drawCircle(
            color = SpatialLinkColors.BrassAntique.copy(alpha = 0.96f),
            radius = ringRadius,
            center = center,
            style = Stroke(width = (strokeWidth + 1.1f).dp.toPx()),
        )
        drawCircle(
            brush = brassMaterial,
            radius = ringRadius,
            center = center,
            style = Stroke(width = strokeWidth.dp.toPx()),
        )
        drawArc(
            color = SpatialLinkColors.BrassPeak.copy(alpha = if (index == 1) 0.46f else 0.28f),
            startAngle = 208f,
            sweepAngle = if (index == 1) 116f else 88f,
            useCenter = false,
            topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
            size = androidx.compose.ui.geometry.Size(ringRadius * 2f, ringRadius * 2f),
            style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round),
        )
    }

    val energyRadius = radius * 0.835f
    val energySize = energyRadius * 2f
    val energyTopLeft = Offset(center.x - energyRadius, center.y - energyRadius)
    rotate(degrees = rotation * 0.11f, pivot = center) {
        drawArc(
            color = SpatialLinkColors.CyanDim.copy(alpha = 0.28f),
            startAngle = 112f,
            sweepAngle = 152f,
            useCenter = false,
            topLeft = energyTopLeft,
            size = androidx.compose.ui.geometry.Size(energySize, energySize),
            style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            color = SpatialLinkColors.CyanGlow.copy(alpha = 0.19f),
            startAngle = 112f,
            sweepAngle = 152f,
            useCenter = false,
            topLeft = energyTopLeft,
            size = androidx.compose.ui.geometry.Size(energySize, energySize),
            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            brush = Brush.sweepGradient(
                listOf(
                    SpatialLinkColors.CyanDim.copy(alpha = 0.24f),
                    SpatialLinkColors.CyanGlow.copy(alpha = 0.82f),
                    SpatialLinkColors.CyanCore.copy(alpha = 0.96f),
                    SpatialLinkColors.IceCyan.copy(alpha = 0.56f),
                    SpatialLinkColors.CyanDim.copy(alpha = 0.18f),
                ),
                center = center,
            ),
            startAngle = 112f,
            sweepAngle = 152f,
            useCenter = false,
            topLeft = energyTopLeft,
            size = androidx.compose.ui.geometry.Size(energySize, energySize),
            style = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round),
        )
        listOf(0.34f to 0.42f, 0.52f to 0.66f, 0.76f to 0.4f).forEach { (start, alpha) ->
            drawArc(
                color = SpatialLinkColors.CyanGlow.copy(alpha = alpha),
                startAngle = 112f + start * 152f,
                sweepAngle = 18f + alpha * 20f,
                useCenter = false,
                topLeft = energyTopLeft,
                size = androidx.compose.ui.geometry.Size(energySize, energySize),
                style = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        drawArc(
            color = SpatialLinkColors.CyanCore.copy(alpha = 0.78f),
            startAngle = 127f,
            sweepAngle = 126f,
            useCenter = false,
            topLeft = energyTopLeft,
            size = androidx.compose.ui.geometry.Size(energySize, energySize),
            style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round),
        )
    }

    repeat(72) { index ->
        val angleDegrees = index * 5f - 90f + rotation * 0.025f
        val angle = Math.toRadians(angleDegrees.toDouble())
        val outer = radius * 0.86f
        val inner = outer - when {
            index % 18 == 0 -> 14.dp.toPx()
            index % 6 == 0 -> 9.dp.toPx()
            else -> 4.dp.toPx()
        }
        val tickColor = when {
            index % 18 == 0 -> SpatialLinkColors.BrassPeak.copy(alpha = 0.72f)
            index % 6 == 0 -> SpatialLinkColors.ChampagneBrass.copy(alpha = 0.68f)
            else -> SpatialLinkColors.BrassDim.copy(alpha = 0.48f)
        }
        drawLine(
            color = tickColor,
            start = Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
            end = Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
            strokeWidth = when {
                index % 18 == 0 -> 1.6.dp.toPx()
                index % 6 == 0 -> 1.1.dp.toPx()
                else -> 0.65.dp.toPx()
            },
        )
    }

    listOf(-90f, 0f, 90f, 180f).forEach { angle ->
        drawStructuralMarker(center = center, radius = radius, angle = angle, brush = brassMaterial)
    }

    listOf(-42f, 42f, 138f, 222f).forEach { angle ->
        val radians = Math.toRadians(angle.toDouble())
        val point = Offset(
            center.x + cos(radians).toFloat() * radius * 0.74f,
            center.y + sin(radians).toFloat() * radius * 0.74f,
        )
        drawSpatialNode(
            point = point,
            color = SpatialLinkColors.BrassEdge,
            scale = 0.72f,
        )
    }

    listOf(126f, 186f, 244f).forEachIndexed { index, angle ->
        val radians = Math.toRadians(angle.toDouble())
        val point = Offset(
            center.x + cos(radians).toFloat() * energyRadius,
            center.y + sin(radians).toFloat() * energyRadius,
        )
        drawSpatialNode(
            point = point,
            color = SpatialLinkColors.CyanCore,
            scale = if (index == 1) 1.18f else 0.92f,
            glowAlpha = listOf(0.12f, 0.18f, 0.1f)[index],
        )
    }

    drawCircle(
        color = Color.Black.copy(alpha = 0.72f),
        radius = radius * 0.36f,
        center = center + Offset(0f, 5.dp.toPx()),
        style = Stroke(width = 9.dp.toPx()),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SpatialLinkColors.CarbonLift, SpatialLinkColors.Obsidian),
            center = center,
            radius = radius * 0.34f,
        ),
        radius = radius * 0.34f,
        center = center,
    )
    drawCircle(
        color = SpatialLinkColors.BrassAntique.copy(alpha = 0.94f),
        radius = radius * 0.335f,
        center = center,
        style = Stroke(width = 3.dp.toPx()),
    )
    drawCircle(
        color = SpatialLinkColors.BrassEdge.copy(alpha = 0.3f),
        radius = radius * 0.305f,
        center = center,
        style = Stroke(width = 0.9.dp.toPx()),
    )
    drawArc(
        color = SpatialLinkColors.CyanCore.copy(alpha = 0.36f),
        startAngle = 208f,
        sweepAngle = 196f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.27f, center.y - radius * 0.27f),
        size = androidx.compose.ui.geometry.Size(radius * 0.54f, radius * 0.54f),
        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawSpatialNode(
    point: Offset,
    color: Color,
    scale: Float,
    glowAlpha: Float = 0.08f,
) {
    drawCircle(
        color = color.copy(alpha = glowAlpha * 0.34f),
        radius = 20.dp.toPx() * scale,
        center = point,
    )
    drawCircle(
        color = color.copy(alpha = glowAlpha),
        radius = 14.dp.toPx() * scale,
        center = point,
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.68f),
        radius = 7.dp.toPx() * scale,
        center = point + Offset(0f, 1.5.dp.toPx()),
    )
    drawCircle(
        color = color.copy(alpha = 0.62f),
        radius = 6.dp.toPx() * scale,
        center = point,
        style = Stroke(width = 1.dp.toPx()),
    )
    drawCircle(
        color = color.copy(alpha = 0.94f),
        radius = 3.dp.toPx() * scale,
        center = point,
    )
    drawCircle(
        color = SpatialLinkColors.WarmIvory.copy(alpha = 0.88f),
        radius = 0.9.dp.toPx() * scale,
        center = point + Offset(-0.7.dp.toPx(), -0.7.dp.toPx()),
    )
}

private fun DrawScope.drawDiamond(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float,
) {
    val diamond = androidx.compose.ui.graphics.Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.7f, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * 0.7f, center.y)
        close()
    }
    drawPath(diamond, color = color, style = Stroke(width = strokeWidth))
}

private fun DrawScope.drawTrustMonogram(
    center: Offset,
    radius: Float,
    color: Color,
) {
    val s = radius * 0.76f
    val monogram = androidx.compose.ui.graphics.Path().apply {
        moveTo(center.x + s * 0.38f, center.y - s * 0.56f)
        cubicTo(
            center.x - s * 0.12f,
            center.y - s * 0.72f,
            center.x - s * 0.56f,
            center.y - s * 0.48f,
            center.x - s * 0.2f,
            center.y - s * 0.18f,
        )
        cubicTo(
            center.x + s * 0.12f,
            center.y + s * 0.08f,
            center.x + s * 0.12f,
            center.y + s * 0.2f,
            center.x - s * 0.28f,
            center.y + s * 0.27f,
        )
        cubicTo(
            center.x - s * 0.7f,
            center.y + s * 0.35f,
            center.x - s * 0.38f,
            center.y + s * 0.72f,
            center.x + s * 0.35f,
            center.y + s * 0.5f,
        )
    }
    drawPath(
        path = monogram,
        color = Color.Black.copy(alpha = 0.82f),
        style = Stroke(width = 5.2.dp.toPx(), cap = StrokeCap.Round),
    )
    drawPath(
        path = monogram,
        color = SpatialLinkColors.BronzeOxide,
        style = Stroke(width = 3.8.dp.toPx(), cap = StrokeCap.Round),
    )
    drawPath(
        path = monogram,
        color = color.copy(alpha = 0.98f),
        style = Stroke(width = 2.25.dp.toPx(), cap = StrokeCap.Round),
    )
    drawPath(
        path = monogram,
        color = SpatialLinkColors.WarmIvory.copy(alpha = 0.42f),
        style = Stroke(width = 0.7.dp.toPx(), cap = StrokeCap.Round),
    )
    drawLine(
        color = Color.Black.copy(alpha = 0.82f),
        start = Offset(center.x + s * 0.12f + 1.2.dp.toPx(), center.y - s * 0.66f + 1.8.dp.toPx()),
        end = Offset(center.x + s * 0.12f + 1.2.dp.toPx(), center.y + s * 0.58f + 1.8.dp.toPx()),
        strokeWidth = 4.8.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SpatialLinkColors.BronzeOxide,
        start = Offset(center.x + s * 0.12f, center.y - s * 0.66f),
        end = Offset(center.x + s * 0.12f, center.y + s * 0.58f),
        strokeWidth = 3.6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(center.x + s * 0.12f, center.y - s * 0.66f),
        end = Offset(center.x + s * 0.12f, center.y + s * 0.58f),
        strokeWidth = 2.15.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SpatialLinkColors.BrassShadow,
        start = Offset(center.x + s * 0.12f, center.y + s * 0.58f),
        end = Offset(center.x + s * 0.48f, center.y + s * 0.58f),
        strokeWidth = 3.6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color.copy(alpha = 0.92f),
        start = Offset(center.x + s * 0.12f, center.y + s * 0.58f),
        end = Offset(center.x + s * 0.48f, center.y + s * 0.58f),
        strokeWidth = 2.15.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

internal fun DrawScope.drawShieldOutline(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float,
) {
    val shield = androidx.compose.ui.graphics.Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.74f, center.y - radius * 0.56f)
        lineTo(center.x + radius * 0.62f, center.y + radius * 0.34f)
        cubicTo(
            center.x + radius * 0.44f,
            center.y + radius * 0.72f,
            center.x + radius * 0.16f,
            center.y + radius * 0.9f,
            center.x,
            center.y + radius,
        )
        cubicTo(
            center.x - radius * 0.16f,
            center.y + radius * 0.9f,
            center.x - radius * 0.44f,
            center.y + radius * 0.72f,
            center.x - radius * 0.62f,
            center.y + radius * 0.34f,
        )
        lineTo(center.x - radius * 0.74f, center.y - radius * 0.56f)
        close()
    }
    drawPath(shield, color = color, style = Stroke(width = strokeWidth))
}
private fun DrawScope.drawStructuralMarker(
    center: Offset,
    radius: Float,
    angle: Float,
    brush: Brush,
) {
    val radians = Math.toRadians(angle.toDouble())
    val direction = Offset(cos(radians).toFloat(), sin(radians).toFloat())
    val start = center + direction * radius * 0.74f
    val end = center + direction * radius * 0.88f
    drawLine(
        color = Color.Black.copy(alpha = 0.72f),
        start = start + Offset(0f, 2.dp.toPx()),
        end = end + Offset(0f, 2.dp.toPx()),
        strokeWidth = 9.dp.toPx(),
        cap = StrokeCap.Square,
    )
    drawLine(
        color = SpatialLinkColors.BrassAntique,
        start = start,
        end = end,
        strokeWidth = 7.dp.toPx(),
        cap = StrokeCap.Square,
    )
    drawLine(
        brush = brush,
        start = start,
        end = end,
        strokeWidth = 4.2.dp.toPx(),
        cap = StrokeCap.Square,
    )
    drawLine(
        color = SpatialLinkColors.BrassPeak.copy(alpha = 0.52f),
        start = start - Offset(0f, 0.7.dp.toPx()),
        end = end - Offset(0f, 0.7.dp.toPx()),
        strokeWidth = 0.8.dp.toPx(),
        cap = StrokeCap.Square,
    )
}

