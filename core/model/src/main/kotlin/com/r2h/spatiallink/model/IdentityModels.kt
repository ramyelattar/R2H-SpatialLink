package com.r2h.spatiallink.model

enum class IdentityAlgorithm {
    ECDSA_P256_SHA256,
}

enum class KeySecurityLevel {
    STRONGBOX,
    TRUSTED_ENVIRONMENT,
    SOFTWARE,
    UNKNOWN_SECURE,
    UNKNOWN,
}

enum class IdentitySelfTestStatus {
    NOT_RUN,
    PASSED,
    FAILED,
}

enum class SignatureVerification {
    VERIFIED,
    REJECTED,
}

enum class IdentityFailureCode {
    NOT_FOUND,
    GENERATION_FAILED,
    KEYSTORE_UNAVAILABLE,
    INVALID_KEY_ENTRY,
    PUBLIC_KEY_UNAVAILABLE,
    SIGNING_FAILED,
    RECOVERY_REQUIRED,
    PLATFORM_FAILURE,
}

data class IdentityFailure(
    val code: IdentityFailureCode,
    val detail: String? = null,
)

data class IdentityInspection(
    val identity: DeviceIdentity,
    val selfTest: IdentitySelfTestStatus,
)

sealed interface IdentityResult<out T> {
    data class Success<T>(val value: T) : IdentityResult<T>

    data class Failure(val error: IdentityFailure) : IdentityResult<Nothing>
}

sealed interface IdentityRepositoryResult {
    data class Available(val inspection: IdentityInspection) : IdentityRepositoryResult

    data object NotCreated : IdentityRepositoryResult

    data class Failed(val error: IdentityFailure) : IdentityRepositoryResult
}
