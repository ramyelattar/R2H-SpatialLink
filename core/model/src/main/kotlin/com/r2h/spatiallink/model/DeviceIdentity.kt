package com.r2h.spatiallink.model

class IdentityPublicKey(encodedBytes: ByteArray) {
    private val value: ByteArray = encodedBytes.copyOf()

    init {
        require(value.isNotEmpty()) { "Encoded public key must not be empty." }
    }

    fun encodedBytes(): ByteArray = value.copyOf()

    override fun equals(other: Any?): Boolean =
        other is IdentityPublicKey && value.contentEquals(other.value)

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = "IdentityPublicKey(encodedBytes=redacted)"
}

data class DeviceIdentity(
    val id: SpatialDeviceId,
    val algorithm: IdentityAlgorithm,
    val publicKey: IdentityPublicKey,
    val securityLevel: KeySecurityLevel,
) {
    init {
        require(id == SpatialDeviceId.fromEncodedPublicKey(publicKey.encodedBytes())) {
            "SpatialDeviceId must be derived from the encoded public key."
        }
    }
}
