package com.r2h.spatiallink.model

object CapabilitySnapshotAssembler {
    fun assemble(
        sdkInt: Int,
        androidVersion: String,
        results: List<CapabilityProbeResult>,
    ): DeviceCapabilities {
        val spatial = SpatialCapability.values().associateWith { capability ->
            when (capability) {
                SpatialCapability.PLATFORM_RANGING -> CapabilityState.UNAVAILABLE
                else -> stateForSpatial(results, capability, CapabilityState.UNKNOWN)
            }
        }

        val rangingStates = if (sdkInt >= 36) {
            RangingTechnology.values().associateWith { technology ->
                stateForRanging(results, technology, CapabilityState.UNAVAILABLE)
            }
        } else {
            legacyRangingStates(sdkInt, spatial)
        }

        val platformRanging = stateForSpatial(
            results = results,
            capability = SpatialCapability.PLATFORM_RANGING,
            default = if (sdkInt < 36) {
                CapabilityState.UNAVAILABLE
            } else {
                derivePlatformRangingState(rangingStates)
            },
        )

        return DeviceCapabilities(
            androidVersion = androidVersion,
            sdkInt = sdkInt,
            bluetoothLe = spatial.getValue(SpatialCapability.BLUETOOTH_LE),
            bleAdvertising = spatial.getValue(SpatialCapability.BLE_ADVERTISING),
            wifiDirect = spatial.getValue(SpatialCapability.WIFI_DIRECT),
            wifiAware = spatial.getValue(SpatialCapability.WIFI_AWARE),
            wifiRtt = spatial.getValue(SpatialCapability.WIFI_RTT),
            nfc = spatial.getValue(SpatialCapability.NFC),
            nfcHce = spatial.getValue(SpatialCapability.NFC_HCE),
            uwb = spatial.getValue(SpatialCapability.UWB),
            platformRanging = platformRanging,
            ranging = RangingCapabilities(
                technologies = rangingStates.toMap(),
                issues = results.flatMap { it.issues }.toList(),
            ),
            issues = results.flatMap { it.issues }.toList(),
        )
    }

    private fun stateForSpatial(
        results: List<CapabilityProbeResult>,
        capability: SpatialCapability,
        default: CapabilityState,
    ): CapabilityState = combineCapabilityStates(
        results.mapNotNull { it.spatialCapabilities[capability] }.ifEmpty { listOf(default) },
    )

    private fun stateForRanging(
        results: List<CapabilityProbeResult>,
        technology: RangingTechnology,
        default: CapabilityState,
    ): CapabilityState = combineCapabilityStates(
        results.mapNotNull { it.rangingTechnologies[technology] }.ifEmpty { listOf(default) },
    )

    private fun legacyRangingStates(
        sdkInt: Int,
        spatial: Map<SpatialCapability, CapabilityState>,
    ): Map<RangingTechnology, CapabilityState> {
        val wifiNanRtt = combineCapabilityStates(
            listOf(
                spatial.getValue(SpatialCapability.WIFI_AWARE),
                spatial.getValue(SpatialCapability.WIFI_RTT),
            ),
        )

        return mapOf(
            RangingTechnology.UWB to if (sdkInt >= 31) {
                spatial.getValue(SpatialCapability.UWB)
            } else {
                CapabilityState.UNAVAILABLE
            },
            RangingTechnology.BLE_CHANNEL_SOUNDING to CapabilityState.UNAVAILABLE,
            RangingTechnology.WIFI_NAN_RTT to wifiNanRtt,
            RangingTechnology.BLE_RSSI to spatial.getValue(SpatialCapability.BLUETOOTH_LE),
            RangingTechnology.WIFI_PROXIMITY_DETECTION to CapabilityState.UNAVAILABLE,
        )
    }

    private fun derivePlatformRangingState(
        rangingStates: Map<RangingTechnology, CapabilityState>,
    ): CapabilityState = combineCapabilityStates(rangingStates.values)
}
