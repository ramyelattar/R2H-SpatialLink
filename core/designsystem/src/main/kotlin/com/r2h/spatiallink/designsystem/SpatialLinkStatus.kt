package com.r2h.spatiallink.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SpatialLinkTone {
    ACTIVE,
    HEALTHY,
    QUIET,
    CAUTION,
    ERROR,
}

data class StatusMarkModel(
    val label: String,
    val tone: SpatialLinkTone,
)

@Composable
fun StatusMark(
    status: StatusMarkModel,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val color = statusColor(status.tone)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(if (compact) 12.dp else 17.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = color.copy(alpha = 0.1f),
                radius = size.minDimension * 0.5f,
                center = center,
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.58f),
                radius = size.minDimension * 0.38f,
                center = center,
            )
            drawCircle(
                color = color.copy(alpha = 0.75f),
                radius = size.minDimension * 0.4f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = color,
                radius = size.minDimension * 0.16f,
                center = center,
            )
            drawCircle(
                color = SpatialLinkColors.WarmIvory.copy(alpha = 0.72f),
                radius = size.minDimension * 0.06f,
                center = center,
            )
        }
        Text(
            text = status.label,
            color = color,
            style = if (compact) {
                MaterialTheme.typography.labelSmall.copy(
                    fontFamily = SpatialLinkFonts.Technical,
                    fontWeight = FontWeight.Light,
                )
            } else {
                MaterialTheme.typography.labelMedium.copy(
                    fontFamily = SpatialLinkFonts.Technical,
                    fontWeight = FontWeight.Light,
                )
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TechnicalValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.widthIn(min = 0.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            color = SpatialLinkColors.MutedIvory,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = SpatialLinkFonts.Technical,
                fontWeight = FontWeight.Light,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = SpatialLinkColors.WarmIvory,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = SpatialLinkFonts.Technical,
                fontWeight = FontWeight.Light,
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun InstrumentLabel(
    title: String,
    status: StatusMarkModel,
    modifier: Modifier = Modifier,
) {
    FieldAnchor(title = title, status = status, modifier = modifier)
}

@Composable
fun FieldAnchor(
    title: String,
    status: StatusMarkModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        SpatialLinkColors.Obsidian.copy(alpha = 0.78f),
                        SpatialLinkColors.CarbonDeep.copy(alpha = 0.54f),
                        SpatialLinkColors.CarbonDeep.copy(alpha = 0.2f),
                        Color.Transparent,
                    ),
                ),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(start = 7.dp, end = 11.dp, top = 6.dp, bottom = 6.dp)
            .semantics {
                contentDescription = "$title ${status.label}"
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        SubsystemInstrument(title = title, status = status)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title.uppercase(),
                color = SpatialLinkColors.WarmIvory,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = SpatialLinkFonts.Technical,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.1.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = status.label,
                color = statusColor(status.tone),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = SpatialLinkFonts.Technical,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.7.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun FieldNode(
    status: StatusMarkModel,
    modifier: Modifier = Modifier,
) {
    val color = statusColor(status.tone)
    Canvas(modifier = modifier.size(24.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = color.copy(alpha = 0.08f), radius = size.minDimension * 0.5f, center = center)
        drawCircle(color = Color.Black.copy(alpha = 0.58f), radius = size.minDimension * 0.39f, center = center)
        drawCircle(
            color = color.copy(alpha = 0.72f),
            radius = size.minDimension * 0.38f,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(color = color, radius = size.minDimension * 0.16f, center = center)
        drawCircle(
            color = SpatialLinkColors.WarmIvory.copy(alpha = 0.76f),
            radius = size.minDimension * 0.06f,
            center = center,
        )
    }
}

@Composable
private fun SubsystemInstrument(
    title: String,
    status: StatusMarkModel,
    modifier: Modifier = Modifier,
) {
    val color = statusColor(status.tone)
    Canvas(modifier = modifier.size(44.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.38f
        drawCircle(
            color = Color.Black.copy(alpha = 0.62f),
            radius = radius + 3.dp.toPx(),
            center = center + Offset(0f, 2.dp.toPx()),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SpatialLinkColors.CarbonLift, SpatialLinkColors.Obsidian),
                center = center,
                radius = radius + 2.dp.toPx(),
            ),
            radius = radius + 1.dp.toPx(),
            center = center,
        )
        drawCircle(
            color = SpatialLinkColors.BrassShadow.copy(alpha = 0.82f),
            radius = radius + 1.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color = color.copy(alpha = 0.52f),
            radius = radius * 0.72f,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        if (title.contains("secure", ignoreCase = true)) {
            drawShieldOutline(
                center = center,
                radius = radius * 0.46f,
                color = SpatialLinkColors.WarmIvory.copy(alpha = 0.92f),
                strokeWidth = 1.2.dp.toPx(),
            )
            drawLine(
                color = color.copy(alpha = 0.9f),
                start = Offset(center.x - radius * 0.18f, center.y),
                end = Offset(center.x - radius * 0.02f, center.y + radius * 0.16f),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color.copy(alpha = 0.9f),
                start = Offset(center.x - radius * 0.02f, center.y + radius * 0.16f),
                end = Offset(center.x + radius * 0.25f, center.y - radius * 0.18f),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        } else {
            drawArc(
                color = SpatialLinkColors.WarmIvory.copy(alpha = 0.84f),
                startAngle = 205f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.5f, center.y - radius * 0.5f),
                size = androidx.compose.ui.geometry.Size(radius, radius),
                style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = SpatialLinkColors.WarmIvory.copy(alpha = 0.66f),
                startAngle = 205f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.28f, center.y - radius * 0.28f),
                size = androidx.compose.ui.geometry.Size(radius * 0.56f, radius * 0.56f),
                style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round),
            )
            drawCircle(color = color, radius = 1.7.dp.toPx(), center = center)
        }
    }
}

internal fun statusColor(tone: SpatialLinkTone): Color = when (tone) {
    SpatialLinkTone.ACTIVE -> SpatialLinkColors.IceCyan
    SpatialLinkTone.HEALTHY -> SpatialLinkColors.MutedMint
    SpatialLinkTone.QUIET -> SpatialLinkColors.MutedIvory
    SpatialLinkTone.CAUTION -> SpatialLinkColors.ChampagneBrass
    SpatialLinkTone.ERROR -> SpatialLinkColors.SoftRose
}
