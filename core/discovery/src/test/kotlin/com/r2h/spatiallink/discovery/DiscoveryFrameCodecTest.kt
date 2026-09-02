package com.r2h.spatiallink.discovery

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class DiscoveryFrameCodecTest {
    @Test
    fun encode_returns_a_nine_byte_v1_payload() {
        val sessionId = DiscoverySessionId.fromBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))!!

        val payload = V1DiscoveryFrameCodec.encode(sessionId)

        assertEquals(DiscoveryProtocol.PAYLOAD_SIZE, payload.size)
        assertEquals(DiscoveryProtocol.V1_VERSION, payload[0])
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8), payload.copyOfRange(1, 9))
    }

    @Test
    fun encode_writes_the_session_token_in_most_significant_byte_first_order() {
        val token = 0x0102030405060708L
        val expectedBytes = ByteArray(Long.SIZE_BYTES) { index ->
            val shift = (Long.SIZE_BYTES - 1 - index) * Byte.SIZE_BITS
            ((token ushr shift) and 0xFF).toByte()
        }
        val sessionId = DiscoverySessionId.fromBytes(expectedBytes)!!

        val payload = V1DiscoveryFrameCodec.encode(sessionId)

        assertArrayEquals(expectedBytes, payload.copyOfRange(1, 9))
    }

    @Test
    fun decode_accepts_one_valid_frame() {
        val payload = byteArrayOf(0x01, 1, 2, 3, 4, 5, 6, 7, 8)

        val decoded = V1DiscoveryFrameCodec.decode(payload)

        assertNotNull(decoded)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8), decoded?.toByteArray())
    }

    @Test
    fun decode_rejects_wrong_length() {
        assertNull(V1DiscoveryFrameCodec.decode(byteArrayOf(0x01, 1, 2, 3, 4, 5, 6, 7)))
        assertNull(V1DiscoveryFrameCodec.decode(byteArrayOf(0x01, 1, 2, 3, 4, 5, 6, 7, 8, 9)))
    }

    @Test
    fun decode_rejects_wrong_version() {
        assertNull(V1DiscoveryFrameCodec.decode(byteArrayOf(0x02, 1, 2, 3, 4, 5, 6, 7, 8)))
    }

    @Test
    fun decode_rejects_all_zero_token() {
        assertNull(V1DiscoveryFrameCodec.decode(ByteArray(DiscoveryProtocol.PAYLOAD_SIZE).also { it[0] = 0x01 }))
    }

    @Test
    fun decode_returns_null_for_malformed_input_without_throwing() {
        assertNull(V1DiscoveryFrameCodec.decode(byteArrayOf()))
        assertNull(V1DiscoveryFrameCodec.decode(byteArrayOf(0x01)))
        assertNull(V1DiscoveryFrameCodec.decode(ByteArray(DiscoveryProtocol.PAYLOAD_SIZE)))
    }
}
