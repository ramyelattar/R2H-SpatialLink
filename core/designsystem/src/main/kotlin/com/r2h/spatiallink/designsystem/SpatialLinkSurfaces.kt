package com.r2h.spatiallink.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpatialSurface(
    modifier: Modifier = Modifier,
    fill: Color = SpatialLinkColors.Carbon,
    stroke: Color = SpatialLinkColors.CarbonEdge,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(18.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, stroke, shape)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun SpatialFrame(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
    }
    Box(
        modifier = modifier
            .then(interactionModifier)
            .clip(CutCornerShape(13.dp))
            .background(SpatialLinkColors.Carbon)
            .border(1.dp, SpatialLinkColors.BrassDim, CutCornerShape(13.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        content = content,
    )
}

@Composable
fun SpatialCommand(
    label: String,
    supporting: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val frameShape = CutCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .clip(frameShape)
            .clickable(role = Role.Button, onClick = onClick)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        SpatialLinkColors.CarbonDeep,
                        SpatialLinkColors.CarbonLift.copy(alpha = 0.78f),
                        SpatialLinkColors.BronzeBlack.copy(alpha = 0.74f),
                        SpatialLinkColors.CarbonDeep,
                    ),
                ),
            )
            .border(1.dp, SpatialLinkColors.BrassAntique, frameShape)
            .padding(horizontal = 26.dp, vertical = 20.dp)
            .semantics { contentDescription = label },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SpatialLinkColors.BrassHighlight.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.38f, size.height * 0.22f),
                    radius = size.maxDimension * 0.9f,
                ),
            )
            val inset = 7.dp.toPx()
            drawRect(
                color = SpatialLinkColors.BronzeDeep.copy(alpha = 0.7f),
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    width = size.width - inset * 2f,
                    height = size.height - inset * 2f,
                ),
                style = Stroke(width = 1.dp.toPx()),
            )
            drawLine(
                color = SpatialLinkColors.BrassPeak.copy(alpha = 0.46f),
                start = Offset(18.dp.toPx(), 1.dp.toPx()),
                end = Offset(size.width * 0.42f, 1.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = SpatialLinkColors.CyanDim.copy(alpha = 0.5f),
                start = Offset(size.width * 0.3f, size.height - 2.dp.toPx()),
                end = Offset(size.width * 0.72f, size.height - 2.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
            drawCircle(
                color = SpatialLinkColors.CyanCore.copy(alpha = 0.9f),
                radius = 1.35.dp.toPx(),
                center = Offset(size.width * 0.3f, size.height - 2.dp.toPx()),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpatialEntryGlyph(modifier = Modifier.size(48.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = label,
                    color = SpatialLinkColors.WarmIvory,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = SpatialLinkFonts.Technical,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 3.2.sp,
                    ),
                )
                Text(
                    text = supporting,
                    color = SpatialLinkColors.MutedIvory,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = SpatialLinkFonts.Technical,
                        fontSize = 11.sp,
                        letterSpacing = 1.15.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ActionArrow(color = SpatialLinkColors.BrassPeak, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
fun ActionArrow(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val y = size.height / 2f
        val right = size.width * 0.84f
        drawLine(
            color = color,
            start = Offset(size.width * 0.12f, y),
            end = Offset(right, y),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(right - 7.dp.toPx(), y - 7.dp.toPx()),
            end = Offset(right, y),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(right - 7.dp.toPx(), y + 7.dp.toPx()),
            end = Offset(right, y),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun SystemTile(
    title: String,
    status: StatusMarkModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .border(1.dp, SpatialLinkColors.CarbonEdge, RoundedCornerShape(12.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            color = SpatialLinkColors.WarmIvory,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = SpatialLinkFonts.Technical,
                fontWeight = FontWeight.Normal,
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        StatusMark(status = status, compact = true)
    }
}

@Composable
fun SystemCluster(
    title: String,
    subtitle: String,
    tiles: List<Pair<String, StatusMarkModel>>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SpatialSurface(
        modifier = modifier.fillMaxWidth(),
        fill = SpatialLinkColors.Carbon.copy(alpha = 0.88f),
        stroke = SpatialLinkColors.CarbonEdge,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClick = { onExpandedChange(!expanded) },
                    )
                    .defaultMinSize(minWidth = 48.dp, minHeight = 54.dp)
                    .padding(horizontal = 17.dp, vertical = 15.dp)
                    .semantics {
                        contentDescription = "$title ${if (expanded) "open" else "closed"}"
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = title,
                        color = SpatialLinkColors.WarmIvory,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = SpatialLinkFonts.Technical,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.9.sp,
                        ),
                    )
                    Text(
                        text = subtitle,
                        color = SpatialLinkColors.MutedIvory,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = SpatialLinkFonts.Technical,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = if (expanded) "CLOSE" else "OPEN",
                    color = SpatialLinkColors.ChampagneBrass,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = SpatialLinkFonts.Technical,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.8.sp,
                    ),
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    tiles.chunked(2).forEach { rowTiles ->
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            rowTiles.forEach { tile ->
                                SystemTile(title = tile.first, status = tile.second, modifier = Modifier.weight(1f))
                            }
                            if (rowTiles.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
