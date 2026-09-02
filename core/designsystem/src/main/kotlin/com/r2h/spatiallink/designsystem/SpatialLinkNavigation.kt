package com.r2h.spatiallink.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class NavigationGlyph {
    OVERVIEW,
    FIELD,
    IDENTITY,
}

data class SpatialNavigationItem(
    val key: String,
    val label: String,
    val glyph: NavigationGlyph,
)

@Composable
fun SpatialNavigationRail(
    items: List<SpatialNavigationItem>,
    selectedKey: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val lineY = 13.dp.toPx()
            drawLine(
                color = SpatialLinkColors.BrassDim.copy(alpha = 0.42f),
                start = Offset(22.dp.toPx(), lineY),
                end = Offset(size.width - 22.dp.toPx(), lineY),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = SpatialLinkColors.TechnicalGrid.copy(alpha = 0.24f),
                start = Offset(22.dp.toPx(), lineY + 3.dp.toPx()),
                end = Offset(size.width - 22.dp.toPx(), lineY + 3.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
            repeat(18) { index ->
                val x = size.width * (0.08f + index * 0.05f)
                drawLine(
                    color = SpatialLinkColors.BrassDim.copy(alpha = if (index % 6 == 0) 0.55f else 0.22f),
                    start = Offset(x, lineY - if (index % 6 == 0) 4.dp.toPx() else 2.dp.toPx()),
                    end = Offset(x, lineY + if (index % 6 == 0) 4.dp.toPx() else 2.dp.toPx()),
                    strokeWidth = 0.7.dp.toPx(),
                )
            }
            drawCircle(
                color = SpatialLinkColors.BrassSpecular.copy(alpha = 0.7f),
                radius = 2.dp.toPx(),
                center = Offset(size.width * 0.18f, lineY),
            )
            drawCircle(
                color = SpatialLinkColors.BrassSpecular.copy(alpha = 0.7f),
                radius = 2.dp.toPx(),
                center = Offset(size.width * 0.82f, lineY),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 5.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            items.forEach { item ->
                NavigationItem(
                    item = item,
                    selected = selectedKey == item.key,
                    onClick = { onSelected(item.key) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavigationItem(
    item: SpatialNavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) SpatialLinkColors.IceCyan else SpatialLinkColors.MutedIvory
    Column(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 58.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = item.label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        NavigationGlyphIcon(glyph = item.glyph, color = color)
        Text(
            text = item.label,
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = SpatialLinkFonts.Technical,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.7.sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Canvas(modifier = Modifier.size(if (selected) 24.dp else 8.dp, 1.dp)) {
            drawLine(
                color = if (selected) SpatialLinkColors.CyanCore else Color.Transparent,
                start = Offset.Zero,
                end = Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}

@Composable
private fun NavigationGlyphIcon(
    glyph: NavigationGlyph,
    color: Color,
) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        when (glyph) {
            NavigationGlyph.OVERVIEW -> {
                drawCircle(color = color, radius = 8.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
                drawCircle(color = color.copy(alpha = 0.72f), radius = 2.3.dp.toPx(), center = center)
                drawLine(color = color.copy(alpha = 0.6f), start = Offset(center.x, 0f), end = Offset(center.x, size.height), strokeWidth = 1.dp.toPx())
            }
            NavigationGlyph.FIELD -> {
                drawCircle(color = color, radius = 8.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
                drawArc(
                    color = color.copy(alpha = 0.68f),
                    startAngle = 205f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(center.x - 5.dp.toPx(), center.y - 5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(10.dp.toPx(), 10.dp.toPx()),
                    style = Stroke(1.dp.toPx()),
                )
                drawCircle(color = color, radius = 1.7.dp.toPx(), center = center)
            }
            NavigationGlyph.IDENTITY -> {
                drawCircle(
                    color = color.copy(alpha = 0.18f),
                    radius = 9.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
                drawCircle(
                    color = color.copy(alpha = 0.7f),
                    radius = 6.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
                drawLine(
                    color = color.copy(alpha = 0.9f),
                    start = Offset(center.x, center.y - 6.dp.toPx()),
                    end = Offset(center.x, center.y + 6.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = color.copy(alpha = 0.6f),
                    start = Offset(center.x - 4.dp.toPx(), center.y),
                    end = Offset(center.x + 4.dp.toPx(), center.y),
                    strokeWidth = 1.dp.toPx(),
                )
                drawCircle(color = color, radius = 1.5.dp.toPx(), center = center)
            }
        }
    }
}
