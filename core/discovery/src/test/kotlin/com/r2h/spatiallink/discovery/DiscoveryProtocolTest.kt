package com.r2h.spatiallink.discovery

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoveryProtocolTest {
    @Test
    fun exposes_the_v1_protocol_constants() {
        assertEquals("7F83C58D-4C4B-4E97-9B7D-9C06D3A47B91", DiscoveryProtocol.SERVICE_UUID)
        assertEquals(0x01.toByte(), DiscoveryProtocol.V1_VERSION)
        assertEquals(9, DiscoveryProtocol.PAYLOAD_SIZE)
        assertEquals(8, DiscoveryProtocol.SESSION_ID_SIZE)
        assertEquals(30_000L, DiscoveryProtocol.SESSION_DURATION_MS)
        assertEquals(6_000L, DiscoveryProtocol.PEER_TTL_MS)
        assertEquals(1_000L, DiscoveryProtocol.ACTIVE_TICK_MS)
        assertEquals(64, DiscoveryProtocol.MAX_PEERS)
        assertEquals(8, DiscoveryProtocol.MAX_GENERATION_ATTEMPTS)
    }
}
