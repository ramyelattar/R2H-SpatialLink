package com.r2h.spatiallink.identity

import com.r2h.spatiallink.model.KeySecurityLevel
import com.r2h.spatiallink.model.IdentityFailureCode

data class PlatformIdentityEntry(
    val algorithm: String,
    val isP256: Boolean,
    val encodedPublicKey: ByteArray,
    val securityLevel: KeySecurityLevel,
)

interface IdentityKeyStorePort {
    suspend fun read(alias: String): PlatformIdentityEntry?

    suspend fun generate(alias: String): PlatformIdentityEntry

    suspend fun sign(alias: String, payload: ByteArray): ByteArray

    suspend fun verify(
        encodedPublicKey: ByteArray,
        payload: ByteArray,
        signature: ByteArray,
    ): Boolean
}

internal class IdentityPlatformException(
    val failureCode: IdentityFailureCode,
) : IllegalStateException()
