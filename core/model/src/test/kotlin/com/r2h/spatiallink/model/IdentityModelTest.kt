package com.r2h.spatiallink.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class IdentityModelTest {
    @Test
    fun device_identity_requires_id_derived_from_its_public_key() {
        val encoded = byteArrayOf(0x30, 0x03, 0x01, 0x01, 0x00)
        val publicKey = IdentityPublicKey(encoded)

        val identity = DeviceIdentity(
            id = SpatialDeviceId.fromEncodedPublicKey(encoded),
            algorithm = IdentityAlgorithm.ECDSA_P256_SHA256,
            publicKey = publicKey,
            securityLevel = KeySecurityLevel.SOFTWARE,
        )

        assertEquals(publicKey, identity.publicKey)
        assertThrows(IllegalArgumentException::class.java) {
            DeviceIdentity(
                id = SpatialDeviceId.fromEncodedPublicKey(byteArrayOf(1, 2, 3)),
                algorithm = IdentityAlgorithm.ECDSA_P256_SHA256,
                publicKey = publicKey,
                securityLevel = KeySecurityLevel.UNKNOWN,
            )
        }
    }

    @Test
    fun public_key_and_signature_bytes_are_defensively_copied() {
        val publicKeyBytes = byteArrayOf(1, 2, 3)
        val signatureBytes = byteArrayOf(4, 5, 6)
        val publicKey = IdentityPublicKey(publicKeyBytes)
        val signature = IdentitySignature(
            algorithm = IdentityAlgorithm.ECDSA_P256_SHA256,
            bytes = signatureBytes,
        )

        publicKeyBytes[0] = 9
        signatureBytes[0] = 9
        val returnedPublicKey = publicKey.encodedBytes()
        val returnedSignature = signature.toByteArray()
        returnedPublicKey[1] = 9
        returnedSignature[1] = 9

        assertArrayEquals(byteArrayOf(1, 2, 3), publicKey.encodedBytes())
        assertArrayEquals(byteArrayOf(4, 5, 6), signature.toByteArray())
    }

    @Test
    fun identity_result_keeps_failures_typed() {
        val result: IdentityResult<IdentitySignature> = IdentityResult.Failure(
            IdentityFailure(IdentityFailureCode.SIGNING_FAILED),
        )

        assertEquals(
            IdentityFailureCode.SIGNING_FAILED,
            (result as IdentityResult.Failure).error.code,
        )
    }
}
