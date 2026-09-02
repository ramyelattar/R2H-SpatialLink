package com.r2h.spatiallink.model

data class DeviceCapabilities(
    val androidVersion: String,
    val sdkInt: Int,
    val bluetoothLe: CapabilityState,
    val bleAdvertising: CapabilityState,
    val wifiDirect: CapabilityState,
    val wifiAware: CapabilityState,
    val wifiRtt: CapabilityState,
    val nfc: CapabilityState,
    val nfcHce: CapabilityState,
    val uwb: CapabilityState,
    val platformRanging: CapabilityState,
    val ranging: RangingCapabilities,
    val issues: List<CapabilityIssue>,
)

fun DeviceCapabilities.spatialStates(): List<CapabilityState> = listOf(
    bluetoothLe,
    bleAdvertising,
    wifiDirect,
    wifiAware,
    wifiRtt,
    nfc,
    nfcHce,
    uwb,
    platformRanging,
)
