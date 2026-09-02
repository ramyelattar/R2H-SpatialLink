package com.r2h.spatiallink.discovery

data class DiscoveryObservation(
    val sessionId: DiscoverySessionId,
    val rssiDbm: Int,
    val receivedAtElapsedMs: Long,
)

/**
 * Privacy-safe observation of ephemeral controller state for internal diagnostics and tests.
 *
 * The session token itself never crosses the controller's public state or publication seam.
 */
data class DiscoveryEphemeralSnapshot(
    val hasSessionToken: Boolean,
    val peerCount: Int,
)
