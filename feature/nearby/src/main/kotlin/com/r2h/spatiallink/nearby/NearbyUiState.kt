package com.r2h.spatiallink.nearby

import com.r2h.spatiallink.discovery.DiscoveryFailure
import com.r2h.spatiallink.discovery.DiscoveryPermission

sealed interface NearbyUiState {
    data object Idle : NearbyUiState
    data object Starting : NearbyUiState
    data class AwaitingPermission(
        val missingPermissions: Set<DiscoveryPermission>,
    ) : NearbyUiState
    data object BluetoothDisabled : NearbyUiState
    data class Active(
        val remainingDurationMs: Long,
        val peerCount: Int,
        val strongestPeerRssiDbm: Int?,
    ) : NearbyUiState
    data object Stopping : NearbyUiState
    data object Completed : NearbyUiState
    data object Unavailable : NearbyUiState
    data class Error(val failure: DiscoveryFailure) : NearbyUiState
}
