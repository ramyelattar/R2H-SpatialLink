package com.r2h.spatiallink.identity

import com.r2h.spatiallink.common.DispatcherProvider
import com.r2h.spatiallink.model.KeySecurityLevel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher

class FakeIdentityKeyStorePort(
    private val generatedEntry: PlatformIdentityEntry = PlatformIdentityEntry(
        algorithm = "EC",
        isP256 = true,
        encodedPublicKey = byteArrayOf(1, 2, 3, 4),
        securityLevel = KeySecurityLevel.SOFTWARE,
    ),
) : IdentityKeyStorePort {
    var storedEntry: PlatformIdentityEntry? = null
    var generateCalls: Int = 0
        private set
    var signCalls: Int = 0
        private set
    var verifyCalls: Int = 0
        private set
    var failGeneration: Boolean = false
    var verificationResult: Boolean = true
    var readGate: CompletableDeferred<Unit>? = null

    override suspend fun read(alias: String): PlatformIdentityEntry? {
        readGate?.await()
        return storedEntry
    }

    override suspend fun generate(alias: String): PlatformIdentityEntry {
        generateCalls += 1
        check(!failGeneration) { "test generation failure" }
        storedEntry = generatedEntry
        return generatedEntry
    }

    override suspend fun sign(alias: String, payload: ByteArray): ByteArray {
        signCalls += 1
        return byteArrayOf(10, 11, 12)
    }

    override suspend fun verify(
        encodedPublicKey: ByteArray,
        payload: ByteArray,
        signature: ByteArray,
    ): Boolean {
        verifyCalls += 1
        return verificationResult
    }
}

class IdentityTestDispatcherProvider(
    override val default: CoroutineDispatcher,
    override val io: CoroutineDispatcher = default,
) : DispatcherProvider
