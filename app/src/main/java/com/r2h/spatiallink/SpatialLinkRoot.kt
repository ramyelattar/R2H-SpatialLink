package com.r2h.spatiallink

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.r2h.spatiallink.diagnostics.DiagnosticsRoute
import com.r2h.spatiallink.diagnostics.DiagnosticsViewModel
import com.r2h.spatiallink.nearby.NearbyRoute
import com.r2h.spatiallink.nearby.NearbyViewModel

@Composable
fun SpatialLinkRoot(
    diagnosticsViewModel: DiagnosticsViewModel,
    nearbyViewModel: NearbyViewModel,
    modifier: Modifier = Modifier,
) {
    var showNearby by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (showNearby) {
            NearbyRoute(
                viewModel = nearbyViewModel,
                onClosed = { showNearby = false },
            )
        } else {
            DiagnosticsRoute(
                viewModel = diagnosticsViewModel,
                onOpenNearby = { showNearby = true },
            )
        }
    }
}
