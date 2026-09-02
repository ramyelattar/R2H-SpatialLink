package com.r2h.spatiallink.handshake

import kotlinx.coroutines.flow.Flow

enum class CryptoFailureCode {
    CRYPTO_UNAVAILABLE,
    INVALID_INPUT,
    OPERATION_FAILED,
}

sealed interface CryptoResult<out T> {
    data class Success<T>(val value: T) : CryptoResult<T>

    data class Failure(
        val code: CryptoFailureCode,
        val detail: String? = null,
    ) : CryptoResult<Nothing>
}

data class EphemeralKeyPair(
    val publicKey: ByteArray,
    val privateKeyPkcs8: ByteArray,
)

enum class ChannelFailureCode {
    CHANNEL_CLOSED,
    SEND_FAILED,
}

sealed interface ChannelResult {
    data object Success : ChannelResult

    data class Failure(
        val code: ChannelFailureCode,
        val detail: String? = null,
    ) : ChannelResult
}

interface HandshakeCryptoPort {
    fun generateEphemeral(): CryptoResult<EphemeralKeyPair>

    fun deriveX25519(
        privateKey: EphemeralKeyPair,
        peerPublicKey: ByteArray,
    ): CryptoResult<ByteArray>

    fun sha256(input: ByteArray): ByteArray

    fun hmacSha256(
        key: ByteArray,
        input: ByteArray,
    ): ByteArray

    fun aeadEncrypt(
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
        plaintext: ByteArray,
    ): CryptoResult<ByteArray>

    fun aeadDecrypt(
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
        ciphertextAndTag: ByteArray,
    ): CryptoResult<ByteArray>

    fun verifyP256(
        publicKeySpki: ByteArray,
        input: ByteArray,
        signature: ByteArray,
    ): CryptoResult<Boolean>
}

interface HandshakeChannel {
    val incomingFrames: Flow<ByteArray>

    suspend fun send(frame: ByteArray): ChannelResult

    suspend fun close()
}
