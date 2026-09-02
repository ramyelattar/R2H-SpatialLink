package com.r2h.spatiallink.discovery

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverySessionIdTest {
    @Test
    fun from_bytes_returns_a_defensive_copy() {
        val source = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

        val sessionId = DiscoverySessionId.fromBytes(source)

        source[0] = 9

        assertNotNull(sessionId)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8), sessionId?.toByteArray())
    }

    @Test
    fun to_byte_array_returns_a_defensive_copy() {
        val sessionId = DiscoverySessionId.fromBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))

        val exported = sessionId!!.toByteArray()
        exported[0] = 9

        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8), sessionId.toByteArray())
    }

    @Test
    fun equal_content_produces_equal_ids_and_hash_codes() {
        val first = DiscoverySessionId.fromBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
        val second = DiscoverySessionId.fromBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))

        assertEquals(first, second)
        assertEquals(first?.hashCode(), second?.hashCode())
    }

    @Test
    fun rejects_non_eight_byte_values() {
        assertNull(DiscoverySessionId.fromBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7)))
        assertNull(DiscoverySessionId.fromBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9)))
    }

    @Test
    fun rejects_all_zero_value() {
        assertNull(DiscoverySessionId.fromBytes(ByteArray(DiscoveryProtocol.SESSION_ID_SIZE)))
    }

    @Test
    fun zero_then_non_zero_returns_a_valid_id() {
        val zeroBytes = ByteArray(DiscoveryProtocol.SESSION_ID_SIZE)
        val nonZeroBytes = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1)
        val random = ScriptedRandomByteSource(zeroBytes, nonZeroBytes)

        val result = SecureRandomDiscoverySessionIdSource(random).next()

        assertTrue(result is SessionIdGenerationResult.Success)
        assertEquals(2, random.drawCount)
        assertArrayEquals(
            nonZeroBytes,
            (result as SessionIdGenerationResult.Success).value.toByteArray(),
        )
    }

    @Test
    fun eight_zero_draws_return_typed_generation_failure() {
        val zeroBytes = ByteArray(DiscoveryProtocol.SESSION_ID_SIZE)
        val random = ScriptedRandomByteSource(
            zeroBytes,
            zeroBytes,
            zeroBytes,
            zeroBytes,
            zeroBytes,
            zeroBytes,
            zeroBytes,
            zeroBytes,
            byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1),
        )

        assertEquals(
            SessionIdGenerationResult.Failed,
            SecureRandomDiscoverySessionIdSource(random).next(),
        )
        assertEquals(8, random.drawCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun source_requires_positive_attempt_count() {
        SecureRandomDiscoverySessionIdSource(
            random = ScriptedRandomByteSource(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1)),
            maxAttempts = 0,
        )
    }

    private class ScriptedRandomByteSource(vararg draws: ByteArray) : RandomByteSource {
        private val scriptedDraws = draws.map(ByteArray::copyOf)
        var drawCount: Int = 0
            private set

        override fun fill(target: ByteArray) {
            val next = scriptedDraws.getOrNull(drawCount)
                ?: error("No scripted draw available for attempt ${drawCount + 1}")

            drawCount += 1
            next.copyInto(target)
        }
    }
}
