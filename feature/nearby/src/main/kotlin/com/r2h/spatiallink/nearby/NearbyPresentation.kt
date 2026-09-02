package com.r2h.spatiallink.nearby

import com.r2h.spatiallink.designsystem.SpatialLinkTone
import com.r2h.spatiallink.designsystem.StatusMarkModel

data class NearbyPresentation(
    val status: StatusMarkModel,
    val presence: NearbyPresencePresentation,
    val isRunning: Boolean,
)

data class NearbyPresencePresentation(
    val count: Int,
    val label: String,
    val strengthLabel: String?,
)

internal fun NearbyUiState.toPresentation(): NearbyPresentation = when (this) {
    NearbyUiState.Idle -> inactivePresentation("IDLE", SpatialLinkTone.QUIET)
    NearbyUiState.Unavailable -> inactivePresentation("UNAVAILABLE", SpatialLinkTone.QUIET)
    is NearbyUiState.AwaitingPermission -> {
        inactivePresentation("AWAITING PERMISSION", SpatialLinkTone.CAUTION)
    }
    NearbyUiState.BluetoothDisabled -> inactivePresentation("BLUETOOTH DISABLED", SpatialLinkTone.CAUTION)
    NearbyUiState.Starting -> NearbyPresentation(
        status = StatusMarkModel("STARTING", SpatialLinkTone.ACTIVE),
        presence = noPresence(),
        isRunning = true,
    )
    is NearbyUiState.Active -> {
        val boundedCount = peerCount.coerceIn(0, MAX_PRESENCE_COUNT)
        NearbyPresentation(
            status = StatusMarkModel("ACTIVE", SpatialLinkTone.ACTIVE),
            presence = NearbyPresencePresentation(
                count = boundedCount,
                label = presenceLabel(boundedCount),
                strengthLabel = strongestPeerRssiDbm?.let { "$it dBm" },
            ),
            isRunning = true,
        )
    }
    NearbyUiState.Stopping -> inactivePresentation("STOPPING", SpatialLinkTone.CAUTION)
    NearbyUiState.Completed -> inactivePresentation("COMPLETED", SpatialLinkTone.HEALTHY)
    is NearbyUiState.Error -> inactivePresentation("ERROR", SpatialLinkTone.ERROR)
}

private fun inactivePresentation(label: String, tone: SpatialLinkTone): NearbyPresentation =
    NearbyPresentation(
        status = StatusMarkModel(label, tone),
        presence = noPresence(),
        isRunning = false,
    )

private fun noPresence() = NearbyPresencePresentation(
    count = 0,
    label = "NO ANONYMOUS PRESENCE",
    strengthLabel = null,
)

private fun presenceLabel(count: Int): String = when (count) {
    0 -> "NO ANONYMOUS PRESENCE"
    1 -> "1 ANONYMOUS PRESENCE"
    else -> "$count ANONYMOUS PRESENCES"
}

private const val MAX_PRESENCE_COUNT = 64
