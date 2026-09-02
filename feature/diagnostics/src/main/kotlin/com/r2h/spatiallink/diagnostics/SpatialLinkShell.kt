package com.r2h.spatiallink.diagnostics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.r2h.spatiallink.designsystem.NavigationGlyph
import com.r2h.spatiallink.designsystem.SpatialAtmosphere
import com.r2h.spatiallink.designsystem.SpatialLinkColors
import com.r2h.spatiallink.designsystem.SpatialLinkTone
import com.r2h.spatiallink.designsystem.SpatialNavigationItem
import com.r2h.spatiallink.designsystem.SpatialNavigationRail

internal enum class SpatialDestination {
    OVERVIEW,
    FIELD,
    IDENTITY,
}

@Composable
internal fun SpatialLinkShell(
    state: DiagnosticsUiState,
    onOpenNearby: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var destinationName by rememberSaveable { mutableStateOf(SpatialDestination.OVERVIEW.name) }
    val destination = runCatching { SpatialDestination.valueOf(destinationName) }
        .getOrDefault(SpatialDestination.OVERVIEW)
    val navigationItems = listOf(
        SpatialNavigationItem(
            key = SpatialDestination.OVERVIEW.name,
            label = "OVERVIEW",
            glyph = NavigationGlyph.OVERVIEW,
        ),
        SpatialNavigationItem(
            key = SpatialDestination.IDENTITY.name,
            label = "IDENTITY",
            glyph = NavigationGlyph.IDENTITY,
        ),
        SpatialNavigationItem(
            key = SpatialDestination.FIELD.name,
            label = "FIELD",
            glyph = NavigationGlyph.FIELD,
        ),
    )

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            SpatialAtmosphere(modifier = Modifier.fillMaxSize()) {
                when (destination) {
                    SpatialDestination.OVERVIEW -> OverviewPage(
                        state = state,
                        onOpenField = onOpenNearby,
                    )
                    SpatialDestination.FIELD -> FieldPage(
                        state = state,
                        onBack = { destinationName = SpatialDestination.OVERVIEW.name },
                    )
                    SpatialDestination.IDENTITY -> IdentityPage(state = state)
                }
            }
        }
        SpatialNavigationRail(
            items = navigationItems,
            selectedKey = destination.name,
            onSelected = { destinationName = it },
        )
    }
}

internal fun surfaceColorFor(tone: SpatialLinkTone): Color = when (tone) {
    SpatialLinkTone.ACTIVE -> SpatialLinkColors.IceCyan.copy(alpha = 0.09f)
    SpatialLinkTone.HEALTHY -> SpatialLinkColors.MutedMint.copy(alpha = 0.09f)
    SpatialLinkTone.QUIET -> SpatialLinkColors.Carbon
    SpatialLinkTone.CAUTION -> SpatialLinkColors.ChampagneBrass.copy(alpha = 0.1f)
    SpatialLinkTone.ERROR -> SpatialLinkColors.SoftRose.copy(alpha = 0.1f)
}
