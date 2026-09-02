package com.r2h.spatiallink.discovery

interface DiscoveryFrameCodec {
    fun encode(sessionId: DiscoverySessionId): ByteArray
    fun decode(payload: ByteArray): DiscoverySessionId?
}

object V1DiscoveryFrameCodec : DiscoveryFrameCodec {
    override fun encode(sessionId: DiscoverySessionId): ByteArray {
        val payload = ByteArray(DiscoveryProtocol.PAYLOAD_SIZE)
        payload[0] = DiscoveryProtocol.V1_VERSION
        sessionId.toByteArray().copyInto(
            destination = payload,
            destinationOffset = 1,
            startIndex = 0,
            endIndex = DiscoveryProtocol.SESSION_ID_SIZE,
        )
        return payload
    }

    override fun decode(payload: ByteArray): DiscoverySessionId? {
        if (payload.size != DiscoveryProtocol.PAYLOAD_SIZE) {
            return null
        }
        if (payload[0] != DiscoveryProtocol.V1_VERSION) {
            return null
        }

        return DiscoverySessionId.fromBytes(payload.copyOfRange(1, DiscoveryProtocol.PAYLOAD_SIZE))
    }
}
