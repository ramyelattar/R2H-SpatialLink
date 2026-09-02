package com.r2h.spatiallink.diagnostics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DiagnosticsRoute(
    viewModel: DiagnosticsViewModel,
    onOpenNearby: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DiagnosticsScreen(state = state, onOpenNearby = onOpenNearby, modifier = modifier)
}

@Composable
fun DiagnosticsScreen(
    state: DiagnosticsUiState,
    onOpenNearby: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SpatialLinkShell(state = state, onOpenNearby = onOpenNearby, modifier = modifier)
}
