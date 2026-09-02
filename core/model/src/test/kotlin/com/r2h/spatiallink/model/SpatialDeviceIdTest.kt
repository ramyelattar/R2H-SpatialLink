package com.r2h.spatiallink.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SpatialDeviceIdTest {
    @Test
    fun canonical_id_is_sha256_of_the_encoded_public_key() {
        val encoded = byteArrayOf(0x30, 0x03, 0x01, 0x01, 0x00)

        val id = SpatialDeviceId.fromEncodedPublicKey(encoded)

        assertEquals(
            "139A28812E7519F744D2D3BF3C6009155566F1C52DF8605F822D7714F1477AFA",
            id.canonicalHex(),
        )
        assertEquals("139A-2881-2E75", id.shortDisplay())
        assertEquals(32, id.toByteArray().size)
    }

    @Test
    fun input_and_output_bytes_are_defensively_copied() {
        val encoded = byteArrayOf(0x30, 0x03, 0x01, 0x01, 0x00)
        val id = SpatialDeviceId.fromEncodedPublicKey(encoded)
        val before = id.toByteArray()

        encoded[0] = 0x7F
        val returned = id.toByteArray()
        returned[0] = 0x7F

        assertArrayEquals(before, id.toByteArray())
    }

    @Test
    fun equal_digest_values_have_content_equality_and_hash() {
        val first = SpatialDeviceId.fromEncodedPublicKey(byteArrayOf(1, 2, 3))
        val second = SpatialDeviceId.fromEncodedPublicKey(byteArrayOf(1, 2, 3))
        val different = SpatialDeviceId.fromEncodedPublicKey(byteArrayOf(1, 2, 4))

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, different)
    }
}
