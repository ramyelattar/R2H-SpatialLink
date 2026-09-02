package com.r2h.spatiallink.model

enum class SpatialCapability {
    BLUETOOTH_LE,
    BLE_ADVERTISING,
    WIFI_DIRECT,
    WIFI_AWARE,
    WIFI_RTT,
    NFC,
    NFC_HCE,
    UWB,
    PLATFORM_RANGING,
}

enum class RangingTechnology {
    UWB,
    BLE_CHANNEL_SOUNDING,
    WIFI_NAN_RTT,
    BLE_RSSI,
    WIFI_PROXIMITY_DETECTION,
}

enum class CapabilitySubsystem {
    BLUETOOTH,
    WIFI,
    NFC_AND_UWB,
    PLATFORM_RANGING,
    SYNTHETIC,
}

internal fun combineCapabilityStates(states: Iterable<CapabilityState>): CapabilityState {
    val collected = states.toList()
    return when {
        CapabilityState.AVAILABLE in collected -> CapabilityState.AVAILABLE
        CapabilityState.UNKNOWN in collected -> CapabilityState.UNKNOWN
        collected.isNotEmpty() -> CapabilityState.UNAVAILABLE
        else -> CapabilityState.UNKNOWN
    }
}
