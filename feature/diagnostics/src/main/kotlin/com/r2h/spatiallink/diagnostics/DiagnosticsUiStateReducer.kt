package com.r2h.spatiallink.diagnostics

import com.r2h.spatiallink.model.DeviceCapabilities
import com.r2h.spatiallink.model.FoundationStatus
import com.r2h.spatiallink.model.IdentityFailureCode
import com.r2h.spatiallink.model.IdentityRepositoryResult
import com.r2h.spatiallink.model.IdentitySelfTestStatus
import com.r2h.spatiallink.model.PermissionSnapshot
import com.r2h.spatiallink.model.deriveFoundationStatus

object DiagnosticsUiStateReducer {
    fun loading(): DiagnosticsUiState = DiagnosticsUiState(
        isLoading = true,
        capabilities = null,
        permissions = null,
        error = null,
        foundationStatus = FoundationStatus.ERROR,
        identity = IdentityUiState(
            status = IdentityStatus.LOADING,
            shortId = null,
            algorithm = null,
            securityLevel = null,
            selfTest = IdentitySelfTestStatus.NOT_RUN,
            failureCode = null,
        ),
    )

    fun success(
        capabilities: DeviceCapabilities,
        permissions: PermissionSnapshot,
        identity: IdentityRepositoryResult = IdentityRepositoryResult.NotCreated,
    ): DiagnosticsUiState = DiagnosticsUiState(
        isLoading = false,
        capabilities = capabilities,
        permissions = permissions,
        error = null,
        foundationStatus = deriveFoundationStatus(capabilities),
        identity = identityState(identity),
    )

    fun failure(
        capabilities: DeviceCapabilities?,
        permissions: PermissionSnapshot?,
        error: DiagnosticsError,
        identity: IdentityRepositoryResult = IdentityRepositoryResult.NotCreated,
    ): DiagnosticsUiState {
        val derivedStatus = deriveFoundationStatus(capabilities)
        val foundationStatus = if (
            error == DiagnosticsError.PermissionInspectionFailed &&
            derivedStatus == FoundationStatus.READY
        ) {
            FoundationStatus.PARTIAL
        } else {
            derivedStatus
        }

        return DiagnosticsUiState(
            isLoading = false,
            capabilities = capabilities,
            permissions = permissions,
            error = error,
            foundationStatus = foundationStatus,
            identity = if (
                error == DiagnosticsError.IdentityInspectionFailed &&
                    identity == IdentityRepositoryResult.NotCreated
            ) {
                identityErrorState()
            } else {
                identityState(identity)
            },
        )
    }

    private fun identityState(result: IdentityRepositoryResult): IdentityUiState = when (result) {
        is IdentityRepositoryResult.Available -> IdentityUiState(
            status = IdentityStatus.READY,
            shortId = result.inspection.identity.id.shortDisplay(),
            algorithm = result.inspection.identity.algorithm.name,
            securityLevel = result.inspection.identity.securityLevel.name,
            selfTest = result.inspection.selfTest,
            failureCode = null,
        )

        IdentityRepositoryResult.NotCreated -> IdentityUiState(
            status = IdentityStatus.NOT_CREATED,
            shortId = null,
            algorithm = null,
            securityLevel = null,
            selfTest = IdentitySelfTestStatus.NOT_RUN,
            failureCode = null,
        )

        is IdentityRepositoryResult.Failed -> IdentityUiState(
            status = when (result.error.code) {
                IdentityFailureCode.NOT_FOUND -> IdentityStatus.NOT_CREATED
                IdentityFailureCode.RECOVERY_REQUIRED -> IdentityStatus.RECOVERY_REQUIRED
                else -> IdentityStatus.UNAVAILABLE
            },
            shortId = null,
            algorithm = null,
            securityLevel = null,
            selfTest = if (result.error.code == IdentityFailureCode.RECOVERY_REQUIRED) {
                IdentitySelfTestStatus.FAILED
            } else {
                IdentitySelfTestStatus.NOT_RUN
            },
            failureCode = result.error.code,
        )
    }

    private fun identityErrorState(): IdentityUiState = IdentityUiState(
        status = IdentityStatus.ERROR,
        shortId = null,
        algorithm = null,
        securityLevel = null,
        selfTest = IdentitySelfTestStatus.NOT_RUN,
        failureCode = IdentityFailureCode.PLATFORM_FAILURE,
    )
}
