package com.r2h.spatiallink.model

import java.security.MessageDigest

class SpatialDeviceId private constructor(bytes: ByteArray) {
    private val value: ByteArray = bytes.copyOf()

    init {
        require(value.size == BYTE_LENGTH) { "SpatialDeviceId must contain exactly 32 bytes." }
    }

    fun toByteArray(): ByteArray = value.copyOf()

    fun canonicalHex(): String = buildString(value.size * 2) {
        value.forEach { byte ->
            append(HEX_DIGITS[(byte.toInt() ushr 4) and 0x0F])
            append(HEX_DIGITS[byte.toInt() and 0x0F])
        }
    }

    fun shortDisplay(): String = canonicalHex()
        .take(SHORT_HEX_LENGTH)
        .chunked(SHORT_GROUP_LENGTH)
        .joinToString("-")

    override fun equals(other: Any?): Boolean =
        other is SpatialDeviceId && value.contentEquals(other.value)

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = shortDisplay()

    companion object {
        private const val BYTE_LENGTH = 32
        private const val SHORT_HEX_LENGTH = 12
        private const val SHORT_GROUP_LENGTH = 4
        private const val HEX_DIGITS = "0123456789ABCDEF"

        fun fromEncodedPublicKey(encodedPublicKey: ByteArray): SpatialDeviceId {
            require(encodedPublicKey.isNotEmpty()) { "Encoded public key must not be empty." }
            return SpatialDeviceId(
                MessageDigest.getInstance("SHA-256").digest(encodedPublicKey),
            )
        }
    }
}
