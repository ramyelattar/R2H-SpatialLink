package com.r2h.spatiallink.identity

import com.r2h.spatiallink.common.DefaultDispatcherProvider
import com.r2h.spatiallink.common.DispatcherProvider
import com.r2h.spatiallink.model.DeviceIdentity
import com.r2h.spatiallink.model.DeviceIdentityRepository
import com.r2h.spatiallink.model.IdentityAlgorithm
import com.r2h.spatiallink.model.IdentityFailure
import com.r2h.spatiallink.model.IdentityFailureCode
import com.r2h.spatiallink.model.IdentityInspection
import com.r2h.spatiallink.model.IdentityPublicKey
import com.r2h.spatiallink.model.IdentityRepositoryResult
import com.r2h.spatiallink.model.IdentityResult
import com.r2h.spatiallink.model.IdentitySelfTestStatus
import com.r2h.spatiallink.model.IdentitySignature
import com.r2h.spatiallink.model.SignatureVerification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.SecureRandom

private fun createSecureChallenge(): ByteArray = ByteArray(32).also {
    SecureRandom().nextBytes(it)
}

class AndroidKeyStoreIdentityRepository(
    private val alias: String = "spatiallink.identity.signing.v1",
    private val keyStore: IdentityKeyStorePort,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    private val challengeSource: () -> ByteArray = ::createSecureChallenge,
) : DeviceIdentityRepository {
    private val operationMutex = Mutex()

    override suspend fun get(): IdentityRepositoryResult = withContext(dispatchers.io) {
        operationMutex.withLock {
            when (val entry = loadEntry()) {
                EntryResult.Missing -> IdentityRepositoryResult.NotCreated
                is EntryResult.Invalid -> IdentityRepositoryResult.Failed(entry.error)
                is EntryResult.Present -> inspect(entry)
            }
        }
    }

    override suspend fun getOrCreate(): IdentityRepositoryResult = withContext(dispatchers.io) {
        operationMutex.withLock {
            when (val entry = loadEntry()) {
                EntryResult.Missing -> generateAndInspect()
                is EntryResult.Invalid -> IdentityRepositoryResult.Failed(entry.error)
                is EntryResult.Present -> inspect(entry)
            }
        }
    }

    override suspend fun sign(payload: ByteArray): IdentityResult<IdentitySignature> =
        withContext(dispatchers.io) {
            operationMutex.withLock {
                when (val entry = loadEntry()) {
                    EntryResult.Missing -> failure(IdentityFailureCode.NOT_FOUND)
                    is EntryResult.Invalid -> IdentityResult.Failure(entry.error)
                    is EntryResult.Present -> try {
                        val signatureBytes = keyStore.sign(alias, payload.copyOf())
                        if (signatureBytes.isEmpty()) {
                            failure(IdentityFailureCode.SIGNING_FAILED)
                        } else {
                            IdentityResult.Success(
                                IdentitySignature(
                                    algorithm = IdentityAlgorithm.ECDSA_P256_SHA256,
                                    bytes = signatureBytes,
                                ),
                            )
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: IdentityPlatformException) {
                        IdentityResult.Failure(IdentityFailure(failure.failureCode))
                    } catch (_: Exception) {
                        failure(IdentityFailureCode.SIGNING_FAILED)
                    }
                }
            }
        }

    override suspend fun verify(
        payload: ByteArray,
        signature: IdentitySignature,
    ): IdentityResult<SignatureVerification> = withContext(dispatchers.io) {
        operationMutex.withLock {
            when (val entry = loadEntry()) {
                EntryResult.Missing -> failure(IdentityFailureCode.NOT_FOUND)
                is EntryResult.Invalid -> IdentityResult.Failure(entry.error)
                is EntryResult.Present -> {
                    if (signature.algorithm != IdentityAlgorithm.ECDSA_P256_SHA256) {
                        return@withLock IdentityResult.Success(SignatureVerification.REJECTED)
                    }

                    try {
                        val verified = keyStore.verify(
                            encodedPublicKey = entry.identity.publicKey.encodedBytes(),
                            payload = payload.copyOf(),
                            signature = signature.toByteArray(),
                        )
                        IdentityResult.Success(
                            if (verified) {
                                SignatureVerification.VERIFIED
                            } else {
                                SignatureVerification.REJECTED
                            },
                        )
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: IdentityPlatformException) {
                        IdentityResult.Failure(IdentityFailure(failure.failureCode))
                    } catch (_: Exception) {
                        IdentityResult.Failure(
                            IdentityFailure(IdentityFailureCode.PLATFORM_FAILURE),
                        )
                    }
                }
            }
        }
    }

    private suspend fun generateAndInspect(): IdentityRepositoryResult {
        val generated = try {
            keyStore.generate(alias)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: IdentityPlatformException) {
            return IdentityRepositoryResult.Failed(IdentityFailure(failure.failureCode))
        } catch (_: Exception) {
            return IdentityRepositoryResult.Failed(
                IdentityFailure(IdentityFailureCode.GENERATION_FAILED),
            )
        }

        return when (val entry = validatedEntry(generated)) {
            is EntryResult.Invalid -> IdentityRepositoryResult.Failed(entry.error)
            is EntryResult.Present -> inspect(entry)
            EntryResult.Missing -> IdentityRepositoryResult.Failed(
                IdentityFailure(IdentityFailureCode.PUBLIC_KEY_UNAVAILABLE),
            )
        }
    }

    private suspend fun loadEntry(): EntryResult = try {
        when (val entry = keyStore.read(alias)) {
            null -> EntryResult.Missing
            else -> validatedEntry(entry)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: IdentityPlatformException) {
        EntryResult.Invalid(IdentityFailure(failure.failureCode))
    } catch (_: Exception) {
        EntryResult.Invalid(IdentityFailure(IdentityFailureCode.KEYSTORE_UNAVAILABLE))
    }

    private fun validatedEntry(entry: PlatformIdentityEntry): EntryResult = when {
        entry.algorithm != ANDROID_EC_ALGORITHM || !entry.isP256 ->
            EntryResult.Invalid(IdentityFailure(IdentityFailureCode.INVALID_KEY_ENTRY))

        entry.encodedPublicKey.isEmpty() ->
            EntryResult.Invalid(IdentityFailure(IdentityFailureCode.PUBLIC_KEY_UNAVAILABLE))

        else -> try {
            val publicKey = IdentityPublicKey(entry.encodedPublicKey)
            EntryResult.Present(
                identity = DeviceIdentity(
                    id = com.r2h.spatiallink.model.SpatialDeviceId.fromEncodedPublicKey(
                        publicKey.encodedBytes(),
                    ),
                    algorithm = IdentityAlgorithm.ECDSA_P256_SHA256,
                    publicKey = publicKey,
                    securityLevel = entry.securityLevel,
                ),
            )
        } catch (_: IllegalArgumentException) {
            EntryResult.Invalid(IdentityFailure(IdentityFailureCode.INVALID_KEY_ENTRY))
        }
    }

    private suspend fun inspect(entry: EntryResult.Present): IdentityRepositoryResult {
        val challenge = try {
            challengeSource().copyOf()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return IdentityRepositoryResult.Failed(
                IdentityFailure(IdentityFailureCode.RECOVERY_REQUIRED),
            )
        }

        if (challenge.isEmpty()) {
            return IdentityRepositoryResult.Failed(
                IdentityFailure(IdentityFailureCode.RECOVERY_REQUIRED),
            )
        }

        return try {
            val signature = keyStore.sign(alias, challenge)
            val verified = keyStore.verify(
                encodedPublicKey = entry.identity.publicKey.encodedBytes(),
                payload = challenge,
                signature = signature.copyOf(),
            )
            if (verified) {
                IdentityRepositoryResult.Available(
                    IdentityInspection(
                        identity = entry.identity,
                        selfTest = IdentitySelfTestStatus.PASSED,
                    ),
                )
            } else {
                IdentityRepositoryResult.Failed(
                    IdentityFailure(IdentityFailureCode.RECOVERY_REQUIRED),
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            IdentityRepositoryResult.Failed(
                IdentityFailure(IdentityFailureCode.RECOVERY_REQUIRED),
            )
        }
    }

    private sealed interface EntryResult {
        data object Missing : EntryResult

        data class Present(val identity: DeviceIdentity) : EntryResult

        data class Invalid(val error: IdentityFailure) : EntryResult
    }

    private fun <T> failure(code: IdentityFailureCode): IdentityResult<T> =
        IdentityResult.Failure(IdentityFailure(code))

    companion object {
        const val PRODUCTION_ALIAS: String = "spatiallink.identity.signing.v1"
        private const val ANDROID_EC_ALGORITHM = "EC"
    }
}
