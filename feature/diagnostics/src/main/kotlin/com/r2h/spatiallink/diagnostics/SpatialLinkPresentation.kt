package com.r2h.spatiallink.diagnostics

import com.r2h.spatiallink.designsystem.SpatialLinkTone
import com.r2h.spatiallink.designsystem.StatusMarkModel
import com.r2h.spatiallink.model.CapabilityState
import com.r2h.spatiallink.model.DeviceCapabilities
import com.r2h.spatiallink.model.FoundationStatus
import com.r2h.spatiallink.model.IdentitySelfTestStatus
import com.r2h.spatiallink.model.PermissionKey
import com.r2h.spatiallink.model.PermissionState
import com.r2h.spatiallink.model.RangingTechnology
import com.r2h.spatiallink.model.SpatialCapability
import com.r2h.spatiallink.model.spatialStates

data class OverviewModel(
    val foundation: StatusMarkModel,
    val identity: StatusMarkModel,
    val localLink: StatusMarkModel,
    val platformLabel: String,
    val identityId: String,
    val identityAlgorithm: String,
    val identityProtection: String,
    val identityVerification: String,
    val fieldState: StatusMarkModel,
    val isLoading: Boolean,
    val errorMessage: String?,
)

data class SystemTileModel(
    val title: String,
    val status: StatusMarkModel,
)

data class SystemClusterModel(
    val title: String,
    val subtitle: String,
    val tiles: List<SystemTileModel>,
)

internal fun DiagnosticsUiState.toOverviewModel(): OverviewModel = OverviewModel(
    foundation = foundationStatus.toStatusMark(),
    identity = identity.toOverviewStatus(),
    localLink = capabilities?.localLinkStatus() ?: StatusMarkModel(
        label = "UNKNOWN",
        tone = SpatialLinkTone.CAUTION,
    ),
    platformLabel = capabilities?.let { "Android ${it.androidVersion} · API ${it.sdkInt}" }
        ?: "Platform snapshot pending",
    identityId = identity.shortId ?: "LOCAL ID PENDING",
    identityAlgorithm = identity.algorithm.toReadableAlgorithm(),
    identityProtection = identity.securityLevel.toReadableSecurityLevel(),
    identityVerification = identity.selfTest.toReadableVerification(),
    fieldState = if (isLoading) {
        StatusMarkModel("INSPECTING", SpatialLinkTone.ACTIVE)
    } else {
        foundationStatus.toFieldStatus()
    },
    isLoading = isLoading,
    errorMessage = error?.displayMessage(),
)

internal fun DiagnosticsUiState.toSystemClusters(): List<SystemClusterModel> = listOf(
    SystemClusterModel(
        title = "Environment",
        subtitle = capabilities?.let { "Android ${it.androidVersion} · API ${it.sdkInt}" }
            ?: "Platform snapshot pending",
        tiles = listOf(
            SystemTileModel("Foundation", if (isLoading) {
                StatusMarkModel("INSPECTING", SpatialLinkTone.ACTIVE)
            } else {
                foundationStatus.toStatusMark()
            }),
            SystemTileModel(
                title = "Capability issues",
                status = StatusMarkModel(
                    label = capabilities?.issues?.size?.toString() ?: "UNKNOWN",
                    tone = if (capabilities?.issues?.isEmpty() == true) {
                        SpatialLinkTone.HEALTHY
                    } else {
                        SpatialLinkTone.CAUTION
                    },
                ),
            ),
        ),
    ),
    SystemClusterModel(
        title = "Spatial capabilities",
        subtitle = capabilities?.spatialStates()?.let { states ->
            "${states.count { it == CapabilityState.AVAILABLE }} available · " +
                "${states.count { it == CapabilityState.UNAVAILABLE }} unavailable"
        } ?: "Capability inspection pending",
        tiles = SpatialCapability.values().map { capability ->
            SystemTileModel(
                title = capability.displayLabel(),
                status = capabilityStateStatus(capabilities?.stateOf(capability)),
            )
        },
    ),
    SystemClusterModel(
        title = "Ranging technologies",
        subtitle = "Platform technology inspection",
        tiles = RangingTechnology.values().map { technology ->
            SystemTileModel(
                title = technology.displayLabel(),
                status = capabilityStateStatus(capabilities?.ranging?.get(technology)),
            )
        },
    ),
    SystemClusterModel(
        title = "Runtime permissions",
        subtitle = "Inspection only · no automatic requests",
        tiles = PermissionKey.values().map { key ->
            SystemTileModel(
                title = key.displayLabel(),
                status = permissionStateStatus(state = permissions?.get(key)),
            )
        },
    ),
)

private fun FoundationStatus.toStatusMark(): StatusMarkModel = when (this) {
    FoundationStatus.READY -> StatusMarkModel("READY", SpatialLinkTone.HEALTHY)
    FoundationStatus.PARTIAL -> StatusMarkModel("PARTIAL", SpatialLinkTone.CAUTION)
    FoundationStatus.ERROR -> StatusMarkModel("ERROR", SpatialLinkTone.ERROR)
}

private fun FoundationStatus.toFieldStatus(): StatusMarkModel = when (this) {
    FoundationStatus.READY -> StatusMarkModel("READY", SpatialLinkTone.ACTIVE)
    FoundationStatus.PARTIAL -> StatusMarkModel("PARTIAL", SpatialLinkTone.CAUTION)
    FoundationStatus.ERROR -> StatusMarkModel("ERROR", SpatialLinkTone.ERROR)
}

private fun IdentityUiState.toOverviewStatus(): StatusMarkModel = when {
    status == IdentityStatus.LOADING -> StatusMarkModel("INSPECTING", SpatialLinkTone.ACTIVE)
    status == IdentityStatus.READY && selfTest == IdentitySelfTestStatus.PASSED -> {
        StatusMarkModel("TRUSTED", SpatialLinkTone.HEALTHY)
    }
    status == IdentityStatus.READY -> StatusMarkModel("VERIFY FAILED", SpatialLinkTone.ERROR)
    status == IdentityStatus.NOT_CREATED -> StatusMarkModel("NOT CREATED", SpatialLinkTone.QUIET)
    status == IdentityStatus.RECOVERY_REQUIRED -> {
        StatusMarkModel("RECOVERY REQUIRED", SpatialLinkTone.CAUTION)
    }
    status == IdentityStatus.ERROR -> StatusMarkModel("ERROR", SpatialLinkTone.ERROR)
    else -> StatusMarkModel("UNAVAILABLE", SpatialLinkTone.QUIET)
}

private fun DeviceCapabilities.localLinkStatus(): StatusMarkModel {
    val localStates = listOf(bluetoothLe, bleAdvertising, wifiDirect, wifiAware, wifiRtt)
    return when {
        localStates.any { it == CapabilityState.AVAILABLE } -> {
            StatusMarkModel("READY", SpatialLinkTone.HEALTHY)
        }
        localStates.all { it == CapabilityState.UNAVAILABLE } -> {
            StatusMarkModel("UNAVAILABLE", SpatialLinkTone.QUIET)
        }
        else -> StatusMarkModel("UNKNOWN", SpatialLinkTone.CAUTION)
    }
}

private fun DeviceCapabilities.stateOf(capability: SpatialCapability): CapabilityState = when (capability) {
    SpatialCapability.BLUETOOTH_LE -> bluetoothLe
    SpatialCapability.BLE_ADVERTISING -> bleAdvertising
    SpatialCapability.WIFI_DIRECT -> wifiDirect
    SpatialCapability.WIFI_AWARE -> wifiAware
    SpatialCapability.WIFI_RTT -> wifiRtt
    SpatialCapability.NFC -> nfc
    SpatialCapability.NFC_HCE -> nfcHce
    SpatialCapability.UWB -> uwb
    SpatialCapability.PLATFORM_RANGING -> platformRanging
}

private fun capabilityStateStatus(state: CapabilityState?): StatusMarkModel = when (state) {
    CapabilityState.AVAILABLE -> StatusMarkModel("AVAILABLE", SpatialLinkTone.ACTIVE)
    CapabilityState.UNAVAILABLE -> StatusMarkModel("UNAVAILABLE", SpatialLinkTone.QUIET)
    CapabilityState.UNKNOWN,
    null,
    -> StatusMarkModel("UNKNOWN", SpatialLinkTone.CAUTION)
}

private fun permissionStateStatus(state: PermissionState?): StatusMarkModel = when (state) {
    PermissionState.GRANTED -> StatusMarkModel("GRANTED", SpatialLinkTone.HEALTHY)
    PermissionState.DENIED -> StatusMarkModel("DENIED", SpatialLinkTone.ERROR)
    PermissionState.NOT_REQUIRED_ON_THIS_OS -> {
        StatusMarkModel("NOT REQUIRED", SpatialLinkTone.QUIET)
    }
    PermissionState.NOT_DECLARED -> StatusMarkModel("NOT DECLARED", SpatialLinkTone.CAUTION)
    PermissionState.UNKNOWN,
    null,
    -> StatusMarkModel("UNKNOWN", SpatialLinkTone.CAUTION)
}

private fun SpatialCapability.displayLabel(): String = when (this) {
    SpatialCapability.BLUETOOTH_LE -> "Bluetooth LE"
    SpatialCapability.BLE_ADVERTISING -> "BLE advertising"
    SpatialCapability.WIFI_DIRECT -> "Wi-Fi Direct"
    SpatialCapability.WIFI_AWARE -> "Wi-Fi Aware"
    SpatialCapability.WIFI_RTT -> "Wi-Fi RTT"
    SpatialCapability.NFC -> "NFC"
    SpatialCapability.NFC_HCE -> "NFC host card emulation"
    SpatialCapability.UWB -> "UWB"
    SpatialCapability.PLATFORM_RANGING -> "Platform ranging"
}

private fun RangingTechnology.displayLabel(): String = when (this) {
    RangingTechnology.UWB -> "UWB"
    RangingTechnology.BLE_CHANNEL_SOUNDING -> "BLE Channel Sounding"
    RangingTechnology.WIFI_NAN_RTT -> "Wi-Fi NAN RTT"
    RangingTechnology.BLE_RSSI -> "BLE RSSI"
    RangingTechnology.WIFI_PROXIMITY_DETECTION -> "Wi-Fi Proximity Detection"
}

private fun PermissionKey.displayLabel(): String = when (this) {
    PermissionKey.BLUETOOTH_SCAN -> "Bluetooth scan"
    PermissionKey.BLUETOOTH_ADVERTISE -> "Bluetooth advertise"
    PermissionKey.BLUETOOTH_CONNECT -> "Bluetooth connect"
    PermissionKey.NEARBY_WIFI_DEVICES -> "Nearby Wi-Fi"
    PermissionKey.ACCESS_LOCAL_NETWORK -> "Local network"
    PermissionKey.RANGING -> "Ranging"
}

private fun String?.toReadableAlgorithm(): String = when (this) {
    "ECDSA_P256_SHA256" -> "ECDSA P-256 / SHA-256"
    null -> "UNKNOWN"
    else -> replace('_', ' ')
}

private fun String?.toReadableSecurityLevel(): String = when (this) {
    null -> "UNKNOWN"
    else -> replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
}

private fun IdentitySelfTestStatus.toReadableVerification(): String = when (this) {
    IdentitySelfTestStatus.NOT_RUN -> "NOT RUN"
    IdentitySelfTestStatus.PASSED -> "VERIFIED"
    IdentitySelfTestStatus.FAILED -> "FAILED"
}

internal fun DiagnosticsError.displayMessage(): String = when (this) {
    DiagnosticsError.CapabilityInspectionFailed -> "Capability inspection unavailable"
    DiagnosticsError.PermissionInspectionFailed -> "Permission inspection unavailable"
    DiagnosticsError.IdentityInspectionFailed -> "Identity inspection unavailable"
}
