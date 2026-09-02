package com.r2h.spatiallink.diagnostics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r2h.spatiallink.designsystem.FieldAnchor
import com.r2h.spatiallink.designsystem.SpatialCommand
import com.r2h.spatiallink.designsystem.SpatialField
import com.r2h.spatiallink.designsystem.SpatialFrame
import com.r2h.spatiallink.designsystem.SpatialLinkColors
import com.r2h.spatiallink.designsystem.SpatialLinkFonts
import com.r2h.spatiallink.designsystem.SpatialLinkTone
import com.r2h.spatiallink.designsystem.SpatialSurface
import com.r2h.spatiallink.designsystem.StatusMark
import com.r2h.spatiallink.designsystem.SystemCluster
import com.r2h.spatiallink.designsystem.TechnicalValue
import com.r2h.spatiallink.designsystem.TrustSeal

@Composable
internal fun OverviewPage(
    state: DiagnosticsUiState,
    onOpenField: () -> Unit,
) {
    val model = state.toOverviewModel()
    SpatialPageColumn(
        horizontalPadding = 22.dp,
        topPadding = 24.dp,
        itemSpacing = 8.dp,
    ) {
        BrandHeader(platformLabel = model.platformLabel)
        Spacer(modifier = Modifier.height(118.dp))
        Text(
            text = "Spatial field",
            color = SpatialLinkColors.WarmIvory,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Light,
                fontSize = 47.sp,
                lineHeight = 54.sp,
                letterSpacing = (-0.8).sp,
            ),
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(18.dp))
        StatusMark(status = model.foundation)
        Spacer(modifier = Modifier.height(13.dp))
        Text(
            text = if (state.isLoading) {
                "Local spatial systems\nare being inspected."
            } else {
                "Local spatial systems\nare ready and trusted."
            },
            color = SpatialLinkColors.MutedIvory,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = SpatialLinkFonts.Technical,
                fontWeight = FontWeight.Light,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
            modifier = Modifier.widthIn(max = 260.dp),
        )
        model.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = SpatialLinkColors.ChampagneBrass,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = SpatialLinkFonts.Technical,
                ),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        OverviewFieldStage(
            model = model,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(14.dp))
        SpatialCommand(
            label = "OPEN FIELD",
            supporting = "Spatial systems",
            onClick = onOpenField,
        )
        Spacer(modifier = Modifier.height(50.dp))
        TrustedDeviceArtifact(model = model)
    }
}

@Composable
private fun OverviewFieldStage(
    model: OverviewModel,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .semantics {
                contentDescription = "Spatial field stage with anchored system signals"
            },
    ) {
        val diameter = (maxWidth * 1.46f).coerceIn(500.dp, 670.dp)
        val stageHeight = (maxWidth * 0.92f).coerceIn(330.dp, 430.dp)
        val fieldOffset = (maxWidth * 0.18f).coerceIn(0.dp, 82.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stageHeight),
        ) {
            SpatialField(
                status = model.fieldState,
                modifier = Modifier
                    .requiredSize(diameter)
                    .offset(x = fieldOffset, y = 0.dp)
                    .align(Alignment.TopStart),
            )
            FieldAnchor(
                title = "Secure link",
                status = model.identity,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = stageHeight * 0.24f - 4.dp),
            )
            FieldAnchor(
                title = "Local network",
                status = model.localLink,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = stageHeight * 0.67f - 4.dp),
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawOverviewConnectors()
            }
        }
    }
}

private fun DrawScope.drawOverviewConnectors() {
    fun drawConnector(start: Offset, elbow: Offset, end: Offset, color: Color) {
        drawLine(
            color = Color.Black.copy(alpha = 0.62f),
            start = start + Offset(0f, 2.dp.toPx()),
            end = elbow + Offset(0f, 2.dp.toPx()),
            strokeWidth = 3.4.dp.toPx(),
        )
        drawLine(
            color = color.copy(alpha = 0.72f),
            start = start,
            end = elbow,
            strokeWidth = 0.8.dp.toPx(),
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.62f),
            start = elbow + Offset(0f, 2.dp.toPx()),
            end = end + Offset(0f, 2.dp.toPx()),
            strokeWidth = 3.4.dp.toPx(),
        )
        drawLine(
            color = color.copy(alpha = 0.72f),
            start = elbow,
            end = end,
            strokeWidth = 0.8.dp.toPx(),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.82f),
            radius = 10.dp.toPx(),
            center = end + Offset(0f, 1.5.dp.toPx()),
        )
        drawCircle(
            color = color.copy(alpha = 0.18f),
            radius = 8.dp.toPx(),
            center = end,
        )
        drawCircle(
            color = color.copy(alpha = 0.74f),
            radius = 5.dp.toPx(),
            center = end,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color = color.copy(alpha = 0.96f),
            radius = 2.2.dp.toPx(),
            center = end,
        )
        drawCircle(
            color = SpatialLinkColors.WarmIvory.copy(alpha = 0.78f),
            radius = 0.75.dp.toPx(),
            center = end - Offset(0.7.dp.toPx(), 0.7.dp.toPx()),
        )
    }

    val secureTop = size.height * 0.24f - 4.dp.toPx()
    val localTop = size.height * 0.67f - 4.dp.toPx()
    val secureConnectorY = secureTop - 10.dp.toPx()
    val localConnectorY = localTop - 10.dp.toPx()
    drawConnector(
        start = Offset(55.dp.toPx(), secureTop + 18.dp.toPx()),
        elbow = Offset(76.dp.toPx(), secureConnectorY),
        end = Offset(size.width * 0.48f, secureConnectorY),
        color = SpatialLinkColors.BrassEdge,
    )
    drawConnector(
        start = Offset(55.dp.toPx(), localTop + 18.dp.toPx()),
        elbow = Offset(76.dp.toPx(), localConnectorY),
        end = Offset(size.width * 0.43f, localConnectorY),
        color = SpatialLinkColors.CyanGlow,
    )
}

@Composable
private fun TrustedDeviceArtifact(model: OverviewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(
                color = SpatialLinkColors.BrassDim.copy(alpha = 0.48f),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx(),
            )
            drawCircle(
                color = SpatialLinkColors.ChampagneBrass.copy(alpha = 0.82f),
                radius = 2.dp.toPx(),
                center = Offset(size.width * 0.5f, 0f),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrustSeal(
                status = model.identity,
                modifier = Modifier.size(72.dp),
                contentDescription = "Trust seal ${model.identity.label.lowercase()}",
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "TRUSTED DEVICE / LOCAL",
                    color = SpatialLinkColors.ChampagneBrass,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = SpatialLinkFonts.Technical,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = model.identityId,
                    color = SpatialLinkColors.WarmIvory,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpatialLinkFonts.Technical,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.1.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics {
                        contentDescription = "Spatial ID value"
                    },
                )
                Text(
                    text = "ECDSA P-256 · KEY PROTECTED",
                    color = SpatialLinkColors.MutedIvory,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = SpatialLinkFonts.Technical,
                        letterSpacing = 0.7.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun FieldPage(
    state: DiagnosticsUiState,
    onBack: () -> Unit,
) {
    val model = state.toOverviewModel()
    val clusters = state.toSystemClusters()
    val defaultExpandedTitles = listOfNotNull(clusters.getOrNull(1)?.title)
    var expandedClusterTitles by rememberSaveable(clusters.map { it.title }.toTypedArray()) {
        mutableStateOf(defaultExpandedTitles)
    }
    SpatialPageColumn {
        BrandHeader(platformLabel = model.platformLabel)
        Spacer(modifier = Modifier.height(118.dp))
        PageTitle(eyebrow = "FIELD / SYSTEM", title = "Field instrumentation")
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 330.dp, max = 430.dp)
                .clipToBounds(),
        ) {
            val diameter = (maxWidth * 1.26f).coerceIn(410.dp, 560.dp)
            SpatialField(
                status = model.fieldState,
                modifier = Modifier
                    .size(diameter)
                    .offset(x = maxWidth * 0.1f, y = (-18).dp)
                    .align(Alignment.TopStart),
            )
        }
        StatusMark(status = model.fieldState)
        SpatialSurface(
            fill = surfaceColorFor(SpatialLinkTone.QUIET),
            contentPadding = PaddingValues(17.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "No active spatial session",
                    color = SpatialLinkColors.WarmIvory,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpatialLinkFonts.Technical,
                        fontWeight = FontWeight.Normal,
                    ),
                )
                Text(
                    text = "This surface reports local readiness only. Peer discovery and ranging sessions are not active in this foundation.",
                    color = SpatialLinkColors.MutedIvory,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpatialLinkFonts.Technical),
                )
            }
        }
        Text(
            text = "SYSTEM READOUT",
            color = SpatialLinkColors.ChampagneBrass,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = SpatialLinkFonts.Technical,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.2.sp,
            ),
        )
        clusters.forEach { cluster ->
            SystemCluster(
                title = cluster.title,
                subtitle = cluster.subtitle,
                tiles = cluster.tiles.map { it.title to it.status },
                expanded = expandedClusterTitles.contains(cluster.title),
                onExpandedChange = { expanded ->
                    expandedClusterTitles = if (expanded) {
                        (expandedClusterTitles + cluster.title).distinct()
                    } else {
                        expandedClusterTitles.filterNot { it == cluster.title }
                    }
                },
            )
        }
        SecondaryAction(label = "RETURN TO OVERVIEW", onClick = onBack)
    }
}

@Composable
internal fun IdentityPage(state: DiagnosticsUiState) {
    val model = state.toOverviewModel()
    SpatialPageColumn {
        BrandHeader(platformLabel = model.platformLabel)
        Spacer(modifier = Modifier.height(118.dp))
        PageTitle(
            eyebrow = "TRUST ARTIFACT",
            title = "Spatial identity",
            titleIsArtifact = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrustSeal(
                status = model.identity,
                modifier = Modifier.size(142.dp),
                contentDescription = "Trust seal ${model.identity.label.lowercase()}",
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatusMark(status = model.identity)
                Text(
                    text = model.identityId,
                    color = SpatialLinkColors.WarmIvory,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpatialLinkFonts.Technical,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.8.sp,
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics {
                        contentDescription = "Spatial ID value"
                    },
                )
                Text(
                    text = "Safe display metadata only",
                    color = SpatialLinkColors.MutedIvory,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = SpatialLinkFonts.Technical),
                )
            }
        }
        SpatialSurface(
            fill = surfaceColorFor(model.identity.tone),
            contentPadding = PaddingValues(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                TechnicalValue(label = "Algorithm", value = model.identityAlgorithm)
                TechnicalValue(label = "Key protection", value = model.identityProtection)
                TechnicalValue(label = "Signing verification", value = model.identityVerification)
            }
        }
        Text(
            text = "Private key material, certificates, and signatures never appear in the product surface.",
            color = SpatialLinkColors.MutedIvory,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = SpatialLinkFonts.Technical),
        )
    }
}

@Composable
private fun SpatialPageColumn(
    horizontalPadding: androidx.compose.ui.unit.Dp = 22.dp,
    topPadding: androidx.compose.ui.unit.Dp = 20.dp,
    itemSpacing: androidx.compose.ui.unit.Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding, vertical = topPadding),
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        content()
        Spacer(modifier = Modifier.height(88.dp))
    }
}

@Composable
private fun BrandHeader(platformLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark(modifier = Modifier.size(24.dp))
            Text(
                text = "SPATIALLINK",
                color = SpatialLinkColors.WarmIvory,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = SpatialLinkFonts.Technical,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 3.1.sp,
                ),
            )
        }
        Text(
            text = platformLabel,
            color = SpatialLinkColors.BrassDim.copy(alpha = 0.78f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = SpatialLinkFonts.Technical,
                fontWeight = FontWeight.Light,
                fontSize = 10.sp,
                letterSpacing = 1.1.sp,
            ),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 132.dp),
        )
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.38f
        val mark = androidx.compose.ui.graphics.Path().apply {
            moveTo(center.x, center.y - radius)
            lineTo(center.x + radius * 0.25f, center.y - radius * 0.25f)
            lineTo(center.x + radius, center.y)
            lineTo(center.x + radius * 0.25f, center.y + radius * 0.25f)
            lineTo(center.x, center.y + radius)
            lineTo(center.x - radius * 0.25f, center.y + radius * 0.25f)
            lineTo(center.x - radius, center.y)
            lineTo(center.x - radius * 0.25f, center.y - radius * 0.25f)
            close()
        }
        drawPath(
            path = mark,
            color = SpatialLinkColors.ChampagneBrass,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
        )
        drawLine(
            color = SpatialLinkColors.IceCyan.copy(alpha = 0.72f),
            start = Offset(center.x, 0f),
            end = Offset(center.x, size.height),
            strokeWidth = 1.dp.toPx(),
        )
        drawCircle(
            color = SpatialLinkColors.WarmIvory,
            radius = 1.5.dp.toPx(),
            center = center,
        )
    }
}

@Composable
private fun PageTitle(
    eyebrow: String,
    title: String,
    titleIsArtifact: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            text = eyebrow,
            color = SpatialLinkColors.IceCyan,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = SpatialLinkFonts.Technical,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.1.sp,
            ),
        )
        Text(
            text = title,
            color = SpatialLinkColors.WarmIvory,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = if (titleIsArtifact) FontFamily.Serif else SpatialLinkFonts.Technical,
                fontWeight = FontWeight.Light,
            ),
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun SecondaryAction(
    label: String,
    onClick: () -> Unit,
) {
    SpatialFrame(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Text(
            text = label,
            color = SpatialLinkColors.ChampagneBrass,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = SpatialLinkFonts.Technical,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp,
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}


