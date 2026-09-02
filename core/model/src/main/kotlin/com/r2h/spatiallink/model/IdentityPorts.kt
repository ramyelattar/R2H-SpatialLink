package com.r2h.spatiallink.model

interface DeviceIdentityRepository {
    suspend fun get(): IdentityRepositoryResult

    suspend fun getOrCreate(): IdentityRepositoryResult

    suspend fun sign(payload: ByteArray): IdentityResult<IdentitySignature>

    suspend fun verify(
        payload: ByteArray,
        signature: IdentitySignature,
    ): IdentityResult<SignatureVerification>
}
