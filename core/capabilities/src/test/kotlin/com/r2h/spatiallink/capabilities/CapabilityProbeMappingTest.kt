package com.r2h.spatiallink.capabilities

import com.r2h.spatiallink.model.CapabilityState
import com.r2h.spatiallink.model.CapabilitySubsystem
import com.r2h.spatiallink.model.CapabilityIssue
import com.r2h.spatiallink.model.CapabilityIssueCode
import com.r2h.spatiallink.model.RangingTechnology
import com.r2h.spatiallink.model.SpatialCapability
import android.ranging.RangingCapabilities
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityProbeMappingTest {
    @Test
    fun bluetooth_maps_advertising_absence_without_hiding_le() = runTest {
        val probe = AndroidBluetoothCapabilityProbe(
            access = FakeBluetoothAccess(
                BluetoothInspection(
                    lowEnergyFeaturePresent = true,
                    adapterPresent = true,
                    advertisingSupported = false,
                ),
            ),
        )

        val result = probe.inspect()

        assertEquals(CapabilityState.AVAILABLE, result.spatialCapabilities[SpatialCapability.BLUETOOTH_LE])
        assertEquals(CapabilityState.UNAVAILABLE, result.spatialCapabilities[SpatialCapability.BLE_ADVERTISING])
    }

    @Test
    fun bluetooth_feature_absence_is_hardware_unavailable() = runTest {
        val probe = AndroidBluetoothCapabilityProbe(
            access = FakeBluetoothAccess(
                BluetoothInspection(
                    lowEnergyFeaturePresent = false,
                    adapterPresent = false,
                    advertisingSupported = null,
                ),
            ),
        )

        val result = probe.inspect()

        assertEquals(CapabilityState.UNAVAILABLE, result.spatialCapabilities[SpatialCapability.BLUETOOTH_LE])
        assertEquals(CapabilityState.UNAVAILABLE, result.spatialCapabilities[SpatialCapability.BLE_ADVERTISING])
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun wifi_capabilities_are_mapped_independently() = runTest {
        val probe = AndroidWifiCapabilityProbe(
            access = FakeWifiAccess(
                WifiInspection(
                    wifiDirect = CapabilityState.AVAILABLE,
                    wifiAware = CapabilityState.UNAVAILABLE,
                    wifiRtt = CapabilityState.UNKNOWN,
                ),
            ),
        )

        val result = probe.inspect()

        assertEquals(CapabilityState.AVAILABLE, result.spatialCapabilities[SpatialCapability.WIFI_DIRECT])
        assertEquals(CapabilityState.UNAVAILABLE, result.spatialCapabilities[SpatialCapability.WIFI_AWARE])
        assertEquals(CapabilityState.UNKNOWN, result.spatialCapabilities[SpatialCapability.WIFI_RTT])
    }

    @Test
    fun nfc_adapter_failure_does_not_discard_hce_or_uwb_feature_state() = runTest {
        val issue = com.r2h.spatiallink.model.CapabilityIssue(
            subsystem = CapabilitySubsystem.NFC_AND_UWB,
            code = com.r2h.spatiallink.model.CapabilityIssueCode.SERVICE_UNAVAILABLE,
        )
        val probe = AndroidNfcUwbCapabilityProbe(
            sdkInt = 36,
            access = FakeNfcUwbAccess(
                NfcUwbInspection(
                    nfc = CapabilityState.UNKNOWN,
                    nfcHce = CapabilityState.AVAILABLE,
                    uwb = CapabilityState.AVAILABLE,
                    issues = listOf(issue),
                ),
            ),
        )

        val result = probe.inspect()

        assertEquals(CapabilityState.UNKNOWN, result.spatialCapabilities[SpatialCapability.NFC])
        assertEquals(CapabilityState.AVAILABLE, result.spatialCapabilities[SpatialCapability.NFC_HCE])
        assertEquals(CapabilityState.AVAILABLE, result.spatialCapabilities[SpatialCapability.UWB])
        assertEquals(listOf(issue), result.issues)
    }

    @Test
    fun uwb_is_unavailable_before_api_31() = runTest {
        val probe = AndroidNfcUwbCapabilityProbe(
            sdkInt = 29,
            access = FakeNfcUwbAccess(
                NfcUwbInspection(
                    nfc = CapabilityState.UNAVAILABLE,
                    nfcHce = CapabilityState.UNAVAILABLE,
                    uwb = CapabilityState.AVAILABLE,
                ),
            ),
        )

        val result = probe.inspect()

        assertEquals(CapabilityState.UNAVAILABLE, result.spatialCapabilities[SpatialCapability.UWB])
    }

    @Test
    fun api_36_ranging_maps_typed_statuses_and_keeps_api_37_technology_unavailable() = runTest {
        val probe = AndroidPlatformRangingCapabilityProbe(
            source = FakeRangingSource(
                PlatformRangingInspection(
                    availability = mapOf(
                        Api36RangingTechnologyIds.UWB to RangingCapabilities.ENABLED,
                        Api36RangingTechnologyIds.BLE_CS to RangingCapabilities.NOT_SUPPORTED,
                        Api36RangingTechnologyIds.WIFI_NAN_RTT to RangingCapabilities.DISABLED_USER,
                        Api36RangingTechnologyIds.BLE_RSSI to 999,
                    ),
                ),
            ),
            sdkInt = 36,
            wifiProximityDetectionId = null,
        )

        val result = probe.inspect()

        assertEquals(CapabilityState.AVAILABLE, result.rangingTechnologies[RangingTechnology.UWB])
        assertEquals(CapabilityState.UNAVAILABLE, result.rangingTechnologies[RangingTechnology.BLE_CHANNEL_SOUNDING])
        assertEquals(CapabilityState.UNAVAILABLE, result.rangingTechnologies[RangingTechnology.WIFI_NAN_RTT])
        assertEquals(CapabilityState.UNKNOWN, result.rangingTechnologies[RangingTechnology.BLE_RSSI])
        assertEquals(
            CapabilityState.UNAVAILABLE,
            result.rangingTechnologies[RangingTechnology.WIFI_PROXIMITY_DETECTION],
        )
        assertEquals(CapabilityState.AVAILABLE, result.spatialCapabilities[SpatialCapability.PLATFORM_RANGING])
        assertTrue(result.issues.any { it.code == CapabilityIssueCode.UNKNOWN_STATUS })
    }

    @Test
    fun ranging_source_failure_is_unknown_with_a_typed_issue() = runTest {
        val issue = CapabilityIssue(
            subsystem = CapabilitySubsystem.PLATFORM_RANGING,
            code = CapabilityIssueCode.CALLBACK_FAILURE,
        )
        val result = AndroidPlatformRangingCapabilityProbe(
            source = FakeRangingSource(PlatformRangingInspection(issues = listOf(issue))),
            sdkInt = 36,
            wifiProximityDetectionId = null,
        ).inspect()

        assertEquals(CapabilityState.UNKNOWN, result.spatialCapabilities[SpatialCapability.PLATFORM_RANGING])
        assertEquals(CapabilityState.UNKNOWN, result.rangingTechnologies[RangingTechnology.UWB])
        assertEquals(CapabilityState.UNAVAILABLE, result.rangingTechnologies[RangingTechnology.WIFI_PROXIMITY_DETECTION])
        assertEquals(listOf(issue), result.issues)
    }

    private class FakeBluetoothAccess(
        private val inspection: BluetoothInspection,
    ) : BluetoothPlatformAccess {
        override fun inspect(): BluetoothInspection = inspection
    }

    private class FakeWifiAccess(
        private val inspection: WifiInspection,
    ) : WifiPlatformAccess {
        override fun inspect(): WifiInspection = inspection
    }

    private class FakeNfcUwbAccess(
        private val inspection: NfcUwbInspection,
    ) : NfcUwbPlatformAccess {
        override fun inspect(): NfcUwbInspection = inspection
    }

    private class FakeRangingSource(
        private val inspection: PlatformRangingInspection,
    ) : PlatformRangingCapabilitySource {
        override suspend fun inspect(): PlatformRangingInspection = inspection
    }
}
