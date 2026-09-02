package com.r2h.spatiallink.connectivity.handshake

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.r2h.spatiallink.handshake.CryptoFailureCode
import com.r2h.spatiallink.handshake.CryptoResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECFieldFp
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@RunWith(AndroidJUnit4::class)
class AndroidHandshakeCryptoInstrumentedTest {
    @Test
    fun provider_supports_required_crypto_matrix_for_current_api_level() {
        val provider = AndroidHandshakeCryptoProvider()
        val alice = provider.generateEphemeral().requireSuccess()
        val bob = provider.generateEphemeral().requireSuccess()

        assertEquals(32, alice.publicKey.size)
        assertEquals(32, bob.publicKey.size)

        val aliceShared = provider.deriveX25519(alice, bob.publicKey).requireSuccess()
        val bobShared = provider.deriveX25519(bob, alice.publicKey).requireSuccess()
        assertEquals(32, aliceShared.size)
        assertTrue(aliceShared.contentEquals(bobShared))
        assertTrue(aliceShared.any { it.toInt() != 0 })

        val key = ByteArray(32) { (it + 1).toByte() }
        val nonce = ByteArray(12) { (it + 9).toByte() }
        val aad = "spatiallink-p3-aad".encodeToByteArray()
        val plaintext = "spatiallink-p3-plaintext".encodeToByteArray()
        val ciphertext = provider.aeadEncrypt(key, nonce, aad, plaintext).requireSuccess()
        assertTrue(ciphertext.size > plaintext.size)
        val decrypted = provider.aeadDecrypt(key, nonce, aad, ciphertext).requireSuccess()
        assertTrue(plaintext.contentEquals(decrypted))

        assertEquals(
            "175a162945f40e77e14ca88b51bd93b4b85b84ad903248ede55d92251967b959",
            provider.sha256("spatiallink-p3-sha".encodeToByteArray()).toHex(),
        )
        assertEquals(
            "cf893960626c642c34d345e90039a3d88bedc96a1d5b7271220a9cff88eb2a4d",
            provider.hmacSha256(
                key = "spatiallink-p3-hmac-key".encodeToByteArray(),
                input = "spatiallink-p3-hmac-input".encodeToByteArray(),
            ).toHex(),
        )

        val signer = KeyPairGenerator.getInstance("EC").apply {
            initialize(256)
        }.generateKeyPair()
        val payload = "spatiallink-p3-signature".encodeToByteArray()
        val signature = Signature.getInstance("SHA256withECDSA").apply {
            initSign(signer.private)
            update(payload)
        }.sign()
        val verified = provider.verifyP256(signer.public.encoded, payload, signature).requireSuccess()
        assertTrue(verified)
        assertEquals(provider.expectedProviderPathForCurrentApi(), provider.providerPathForTest())
    }

    @Test
    fun provider_failure_is_typed_as_crypto_unavailable() {
        val provider = AndroidHandshakeCryptoProvider.forTest(
            sdkInt = 30,
            selector = object : PlatformCryptoSelector {
                override fun select(sdkInt: Int): PlatformCryptoSelection =
                    PlatformCryptoSelection.unavailable("forced provider failure")
            },
        )

        val expected = CryptoResult.Failure(
            code = CryptoFailureCode.CRYPTO_UNAVAILABLE,
            detail = "forced provider failure",
        )
        assertEquals(expected, provider.generateEphemeral())
        assertEquals(
            expected,
            provider.deriveX25519(
                privateKey = com.r2h.spatiallink.handshake.EphemeralKeyPair(
                    publicKey = ByteArray(32),
                    privateKeyPkcs8 = byteArrayOf(1),
                ),
                peerPublicKey = ByteArray(32),
            ),
        )

        val key = ByteArray(32) { (it + 1).toByte() }
        val nonce = ByteArray(12) { (it + 9).toByte() }
        val aad = "spatiallink-p3-aad".encodeToByteArray()
        val plaintext = "spatiallink-p3-plaintext".encodeToByteArray()
        val ciphertext = Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            updateAAD(aad)
            doFinal(plaintext)
        }
        assertEquals(expected, provider.aeadEncrypt(key, nonce, aad, plaintext))
        assertEquals(expected, provider.aeadDecrypt(key, nonce, aad, ciphertext))

        val signer = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()
        val signature = Signature.getInstance("SHA256withECDSA").apply {
            initSign(signer.private)
            update(plaintext)
        }.sign()
        assertEquals(expected, provider.verifyP256(signer.public.encoded, plaintext, signature))
    }

    @Test
    fun aead_encrypt_rejects_non_aes256_key_lengths() {
        val provider = AndroidHandshakeCryptoProvider()

        listOf(16, 24, 31, 33).forEach { keyLength ->
            val result = provider.aeadEncrypt(
                key = ByteArray(keyLength) { 1 },
                nonce = ByteArray(12) { 2 },
                aad = "aad".encodeToByteArray(),
                plaintext = "plaintext".encodeToByteArray(),
            )

            assertEquals(
                "key length $keyLength",
                CryptoResult.Failure(
                    code = CryptoFailureCode.INVALID_INPUT,
                    detail = "AES-256-GCM key must be 32 bytes.",
                ),
                result,
            )
        }
    }

    @Test
    fun verify_p256_rejects_valid_non_p256_ec_key() {
        val provider = AndroidHandshakeCryptoProvider()
        val payload = "spatiallink-p3-signature".encodeToByteArray()
        val p384 = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp384r1"))
        }.generateKeyPair()
        val signature = Signature.getInstance("SHA256withECDSA").apply {
            initSign(p384.private)
            update(payload)
        }.sign()

        val result = provider.verifyP256(p384.public.encoded, payload, signature)

        assertEquals(
            CryptoResult.Failure(
                code = CryptoFailureCode.INVALID_INPUT,
                detail = "EC public key must use secp256r1.",
            ),
            result,
        )
    }

    @Test
    fun verify_p256_accepts_valid_secp256r1_key() {
        val provider = AndroidHandshakeCryptoProvider()
        val payload = "spatiallink-p3-signature".encodeToByteArray()
        val p256 = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()
        val signature = Signature.getInstance("SHA256withECDSA").apply {
            initSign(p256.private)
            update(payload)
        }.sign()

        val result = provider.verifyP256(p256.public.encoded, payload, signature).requireSuccess()

        assertTrue(result)
        val importedKey = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(p256.public.encoded)) as ECPublicKey
        assertTrue(importedKey.params.matches(expectedP256Params()))
    }

    private fun <T> CryptoResult<T>.requireSuccess(): T = when (this) {
        is CryptoResult.Success -> value
        is CryptoResult.Failure -> throw AssertionError("expected success but was $this")
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }

    private fun expectedP256Params(): ECParameterSpec =
        AlgorithmParameters.getInstance("EC").apply {
            init(ECGenParameterSpec("secp256r1"))
        }.getParameterSpec(ECParameterSpec::class.java)

    private fun ECParameterSpec.matches(other: ECParameterSpec): Boolean {
        val thisField = curve.field as? ECFieldFp ?: return false
        val otherField = other.curve.field as? ECFieldFp ?: return false
        return thisField.p == otherField.p &&
            curve.a == other.curve.a &&
            curve.b == other.curve.b &&
            generator.affineX == other.generator.affineX &&
            generator.affineY == other.generator.affineY &&
            order == other.order &&
            cofactor == other.cofactor
    }
}
