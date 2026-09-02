package com.r2h.spatiallink.model

class IdentitySignature(
    val algorithm: IdentityAlgorithm,
    bytes: ByteArray,
) {
    private val value: ByteArray = bytes.copyOf()

    init {
        require(value.isNotEmpty()) { "Signature bytes must not be empty." }
    }

    fun toByteArray(): ByteArray = value.copyOf()

    override fun equals(other: Any?): Boolean =
        other is IdentitySignature &&
            algorithm == other.algorithm &&
            value.contentEquals(other.value)

    override fun hashCode(): Int = 31 * algorithm.hashCode() + value.contentHashCode()

    override fun toString(): String =
        "IdentitySignature(algorithm=$algorithm, bytes=redacted)"
}
