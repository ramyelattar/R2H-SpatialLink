package com.r2h.spatiallink.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CapabilitySnapshotAssemblerTest {
    @Test
    fun available_wins_over_other_states() {
        val snapshot = CapabilitySnapshotAssembler.assemble(
            sdkInt = 36,
            androidVersion = "16",
            results = listOf(
                CapabilityProbeResult(
                    subsystem = CapabilitySubsystem.BLUETOOTH,
                    spatialCapabilities = mapOf(SpatialCapability.BLUETOOTH_LE to CapabilityState.UNAVAILABLE),
                ),
                CapabilityProbeResult(
                    subsystem = CapabilitySubsystem.WIFI,
                    spatialCapabilities = mapOf(SpatialCapability.BLUETOOTH_LE to CapabilityState.AVAILABLE),
                ),
            ),
        )

        assertEquals(CapabilityState.AVAILABLE, snapshot.bluetoothLe)
    }

    @Test
    fun unknown_wins_when_no_available_state_exists() {
        val snapshot = CapabilitySnapshotAssembler.assemble(
            sdkInt = 36,
            androidVersion = "16",
            results = listOf(
                CapabilityProbeResult(
                    subsystem = CapabilitySubsystem.BLUETOOTH,
                    spatialCapabilities = mapOf(SpatialCapability.BLUETOOTH_LE to CapabilityState.UNAVAILABLE),
                ),
                CapabilityProbeResult(
                    subsystem = CapabilitySubsystem.WIFI,
                    spatialCapabilities = mapOf(SpatialCapability.BLUETOOTH_LE to CapabilityState.UNKNOWN),
                ),
            ),
        )

        assertEquals(CapabilityState.UNKNOWN, snapshot.bluetoothLe)
    }

    @Test
    fun hardware_absence_is_unavailable_without_partial_status() {
        val capabilities = CapabilitySnapshotAssembler.assemble(
            sdkInt = 29,
            androidVersion = "10",
            results = listOf(CapabilityProbeResult.allUnavailable()),
        )

        assertEquals(CapabilityState.UNAVAILABLE, capabilities.bluetoothLe)
        assertEquals(FoundationStatus.READY, deriveFoundationStatus(capabilities))
    }

    @Test
    fun legacy_ranging_fallback_uses_meaningful_platform_capabilities() {
        val capabilities = CapabilitySnapshotAssembler.assemble(
            sdkInt = 35,
            androidVersion = "15",
            results = listOf(
                CapabilityProbeResult(
                    subsystem = CapabilitySubsystem.BLUETOOTH,
                    spatialCapabilities = mapOf(SpatialCapability.BLUETOOTH_LE to CapabilityState.AVAILABLE),
                ),
                CapabilityProbeResult(
                    subsystem = CapabilitySubsystem.WIFI,
                    spatialCapabilities = mapOf(
                        SpatialCapability.WIFI_AWARE to CapabilityState.AVAILABLE,
                        SpatialCapability.WIFI_RTT to CapabilityState.AVAILABLE,
                    ),
                ),
                CapabilityProbeResult(
                    subsystem = CapabilitySubsystem.NFC_AND_UWB,
                    spatialCapabilities = mapOf(SpatialCapability.UWB to CapabilityState.AVAILABLE),
                ),
            ),
        )

        assertEquals(CapabilityState.AVAILABLE, capabilities.ranging[RangingTechnology.UWB])
        assertEquals(CapabilityState.AVAILABLE, capabilities.ranging[RangingTechnology.BLE_RSSI])
        assertEquals(CapabilityState.AVAILABLE, capabilities.ranging[RangingTechnology.WIFI_NAN_RTT])
        assertEquals(CapabilityState.UNAVAILABLE, capabilities.ranging[RangingTechnology.BLE_CHANNEL_SOUNDING])
        assertEquals(CapabilityState.UNAVAILABLE, capabilities.ranging[RangingTechnology.WIFI_PROXIMITY_DETECTION])
    }
}
