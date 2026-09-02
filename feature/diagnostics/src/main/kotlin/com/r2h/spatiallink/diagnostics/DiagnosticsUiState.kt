package com.r2h.spatiallink.diagnostics

import com.r2h.spatiallink.model.DeviceCapabilities
import com.r2h.spatiallink.model.FoundationStatus
import com.r2h.spatiallink.model.IdentityFailureCode
import com.r2h.spatiallink.model.IdentitySelfTestStatus
import com.r2h.spatiallink.model.PermissionSnapshot

enum class IdentityStatus {
    LOADING,
    READY,
    NOT_CREATED,
    UNAVAILABLE,
    RECOVERY_REQUIRED,
    ERROR,
}

data class IdentityUiState(
    val status: IdentityStatus,
    val shortId: String?,
    val algorithm: String?,
    val securityLevel: String?,
    val selfTest: IdentitySelfTestStatus,
    val failureCode: IdentityFailureCode?,
)

data class DiagnosticsUiState(
    val isLoading: Boolean,
    val capabilities: DeviceCapabilities?,
    val permissions: PermissionSnapshot?,
    val error: DiagnosticsError?,
    val foundationStatus: FoundationStatus,
    val identity: IdentityUiState,
)

sealed interface DiagnosticsError {
    data object CapabilityInspectionFailed : DiagnosticsError
    data object PermissionInspectionFailed : DiagnosticsError
    data object IdentityInspectionFailed : DiagnosticsError
}
