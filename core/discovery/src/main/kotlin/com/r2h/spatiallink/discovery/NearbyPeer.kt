package com.r2h.spatiallink.discovery

enum class PeerAuthenticity {
    UNVERIFIED,
}

data class NearbyPeer(
    val sessionId: DiscoverySessionId,
    val rssiDbm: Int,
    val lastSeenElapsedMs: Long,
    val authenticity: PeerAuthenticity = PeerAuthenticity.UNVERIFIED,
)
