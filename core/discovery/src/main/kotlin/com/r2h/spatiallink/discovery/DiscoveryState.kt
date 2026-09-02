package com.r2h.spatiallink.discovery

enum class DiscoveryFailureCode {
    SESSION_ID_GENERATION_FAILED,
    SCAN_START_FAILED,
    ADVERTISE_START_FAILED,
    DATA_TOO_LARGE,
    CALLBACK_FAILED,
    UNEXPECTED_PLATFORM_FAILURE,
}

data class DiscoveryFailure(
    val code: DiscoveryFailureCode,
    val detail: String? = null,
)

sealed interface DiscoveryState {
    data object Idle : DiscoveryState

    data object Unavailable : DiscoveryState

    data class AwaitingPermission(
        val missingPermissions: Set<DiscoveryPermission>,
    ) : DiscoveryState

    data object BluetoothDisabled : DiscoveryState

    data object Starting : DiscoveryState

    data class Active(
        val remainingDurationMs: Long,
        val peers: List<NearbyPeer>,
    ) : DiscoveryState

    data object Stopping : DiscoveryState

    data object Completed : DiscoveryState

    data class Error(
        val failure: DiscoveryFailure,
    ) : DiscoveryState
}
