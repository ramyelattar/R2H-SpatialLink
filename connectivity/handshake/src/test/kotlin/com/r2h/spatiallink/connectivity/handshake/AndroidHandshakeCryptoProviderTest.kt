package com.r2h.spatiallink.connectivity.handshake

import com.r2h.spatiallink.handshake.CryptoFailureCode
import com.r2h.spatiallink.handshake.CryptoResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidHandshakeCryptoProviderTest {
    @Test
    fun api34_prefers_platform_xdh_provider() {
        val selector = RecordingPlatformSelector()
        val provider = AndroidHandshakeCryptoProvider.forTest(
            sdkInt = 34,
            selector = selector,
        )

        val result = provider.generateEphemeral()

        assertTrue(result is CryptoResult.Success)
        assertEquals(1, selector.platformSelections)
        assertEquals(0, selector.conscryptSelections)
        assertEquals(ProviderPath.PLATFORM_XDH, provider.providerPathForTest())
    }

    @Test
    fun api30_uses_conscrypt_provider_path() {
        val selector = RecordingPlatformSelector()
        val provider = AndroidHandshakeCryptoProvider.forTest(
            sdkInt = 30,
            selector = selector,
        )

        val result = provider.generateEphemeral()

        assertTrue(result is CryptoResult.Success)
        assertEquals(0, selector.platformSelections)
        assertEquals(1, selector.conscryptSelections)
        assertEquals(ProviderPath.CONSCRYPT_XDH, provider.providerPathForTest())
    }

    @Test
    fun provider_failure_maps_to_crypto_unavailable() {
        val provider = unavailableProvider()

        assertUnavailable(provider.generateEphemeral(), "missing provider")
        assertUnavailable(
            provider.deriveX25519(
                privateKey = com.r2h.spatiallink.handshake.EphemeralKeyPair(
                    publicKey = ByteArray(32),
                    privateKeyPkcs8 = byteArrayOf(1),
                ),
                peerPublicKey = ByteArray(32),
            ),
            "missing provider",
        )
        assertUnavailable(
            provider.aeadEncrypt(
                key = ByteArray(32) { 1 },
                nonce = ByteArray(12) { 2 },
                aad = byteArrayOf(3),
                plaintext = byteArrayOf(4),
            ),
            "missing provider",
        )
        assertUnavailable(
            provider.aeadDecrypt(
                key = ByteArray(32) { 1 },
                nonce = ByteArray(12) { 2 },
                aad = byteArrayOf(3),
                ciphertextAndTag = byteArrayOf(4),
            ),
            "missing provider",
        )
        assertUnavailable(
            provider.verifyP256(
                publicKeySpki = byteArrayOf(1, 2, 3),
                input = byteArrayOf(4),
                signature = byteArrayOf(5),
            ),
            "missing provider",
        )
    }

    @Test
    fun provider_does_not_fallback_to_p256_agreement() {
        val selector = object : PlatformCryptoSelector {
            override fun select(sdkInt: Int): PlatformCryptoSelection =
                PlatformCryptoSelection.available(
                    path = ProviderPath.CONSCRYPT_XDH,
                    primitives = object : PlatformCryptoPrimitives {
                        override fun generateEphemeral(): CryptoResult<com.r2h.spatiallink.handshake.EphemeralKeyPair> =
                            CryptoResult.Failure(
                                code = CryptoFailureCode.CRYPTO_UNAVAILABLE,
                                detail = "x25519 unavailable",
                            )

                        override fun deriveX25519(
                            keyPair: com.r2h.spatiallink.handshake.EphemeralKeyPair,
                            peerPublicKey: ByteArray,
                        ): CryptoResult<ByteArray> = error("not used")

                        override fun sha256(input: ByteArray): ByteArray = error("not used")

                        override fun hmacSha256(key: ByteArray, input: ByteArray): ByteArray = error("not used")

                        override fun aeadEncrypt(
                            key: ByteArray,
                            nonce: ByteArray,
                            aad: ByteArray,
                            plaintext: ByteArray,
                        ): CryptoResult<ByteArray> = error("not used")

                        override fun aeadDecrypt(
                            key: ByteArray,
                            nonce: ByteArray,
                            aad: ByteArray,
                            ciphertextAndTag: ByteArray,
                        ): CryptoResult<ByteArray> = error("not used")

                        override fun verifyP256(
                            publicKeySpki: ByteArray,
                            input: ByteArray,
                            signature: ByteArray,
                        ): CryptoResult<Boolean> {
                            throw AssertionError("P-256 verification must not be used as X25519 fallback")
                        }
                    },
                )
        }
        val provider = AndroidHandshakeCryptoProvider.forTest(
            sdkInt = 30,
            selector = selector,
        )

        val result = provider.generateEphemeral()

        assertEquals(
            CryptoResult.Failure(
                code = CryptoFailureCode.CRYPTO_UNAVAILABLE,
                detail = "x25519 unavailable",
            ),
            result,
        )
    }

    private class RecordingPlatformSelector : PlatformCryptoSelector {
        var platformSelections: Int = 0
            private set
        var conscryptSelections: Int = 0
            private set

        override fun select(sdkInt: Int): PlatformCryptoSelection {
            if (sdkInt >= 33) {
                platformSelections += 1
                return PlatformCryptoSelection.available(
                    path = ProviderPath.PLATFORM_XDH,
                    primitives = FakePlatformCryptoPrimitives(),
                )
            }
            conscryptSelections += 1
            return PlatformCryptoSelection.available(
                path = ProviderPath.CONSCRYPT_XDH,
                primitives = FakePlatformCryptoPrimitives(),
            )
        }
    }

    private class FakePlatformCryptoPrimitives : PlatformCryptoPrimitives {
        override fun generateEphemeral(): CryptoResult<com.r2h.spatiallink.handshake.EphemeralKeyPair> =
            CryptoResult.Success(
                com.r2h.spatiallink.handshake.EphemeralKeyPair(
                    publicKey = ByteArray(32) { 1 },
                    privateKeyPkcs8 = byteArrayOf(48, 42),
                ),
            )

        override fun deriveX25519(
            keyPair: com.r2h.spatiallink.handshake.EphemeralKeyPair,
            peerPublicKey: ByteArray,
        ): CryptoResult<ByteArray> = CryptoResult.Success(ByteArray(32) { 7 })

        override fun sha256(input: ByteArray): ByteArray = input.copyOf()

        override fun hmacSha256(key: ByteArray, input: ByteArray): ByteArray = key + input

        override fun aeadEncrypt(
            key: ByteArray,
            nonce: ByteArray,
            aad: ByteArray,
            plaintext: ByteArray,
        ): CryptoResult<ByteArray> = CryptoResult.Success(plaintext.copyOf())

        override fun aeadDecrypt(
            key: ByteArray,
            nonce: ByteArray,
            aad: ByteArray,
            ciphertextAndTag: ByteArray,
        ): CryptoResult<ByteArray> = CryptoResult.Success(ciphertextAndTag.copyOf())

        override fun verifyP256(
            publicKeySpki: ByteArray,
            input: ByteArray,
            signature: ByteArray,
        ): CryptoResult<Boolean> = CryptoResult.Success(true)
    }

    private fun unavailableProvider(): AndroidHandshakeCryptoProvider =
        AndroidHandshakeCryptoProvider.forTest(
            sdkInt = 30,
            selector = PlatformCryptoSelector {
                PlatformCryptoSelection.unavailable("missing provider")
            },
        )

    private fun assertUnavailable(
        result: CryptoResult<*>,
        detail: String,
    ) {
        assertEquals(
            CryptoResult.Failure(
                code = CryptoFailureCode.CRYPTO_UNAVAILABLE,
                detail = detail,
            ),
            result,
        )
    }
}
