package com.r2h.spatiallink.discovery

object DiscoveryProtocol {
    const val SERVICE_UUID: String = "7F83C58D-4C4B-4E97-9B7D-9C06D3A47B91"
    const val V1_VERSION: Byte = 0x01
    const val PAYLOAD_SIZE: Int = 9
    const val SESSION_ID_SIZE: Int = 8
    const val SESSION_DURATION_MS: Long = 30_000L
    const val PEER_TTL_MS: Long = 6_000L
    const val ACTIVE_TICK_MS: Long = 1_000L
    const val MAX_PEERS: Int = 64
    const val MAX_GENERATION_ATTEMPTS: Int = 8
}
