package com.r2h.spatiallink.ble

import com.r2h.spatiallink.discovery.DiscoveryFailureCode
import com.r2h.spatiallink.discovery.DiscoveryProtocol
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class LegacyAdvertisementBudgetTest {
    private val validPayload = byteArrayOf(
        DiscoveryProtocol.V1_VERSION,
        1, 2, 3, 4, 5, 6, 7, 8,
    )

    @Test
    fun v1_owned_service_data_is_27_bytes_and_within_the_legacy_bound() {
        val spec = LegacyAdvertisementSpec.forPayload(validPayload)

        assertEquals(27, spec.ownedServiceDataBytes)
        assertTrue(spec.fitsLegacyBudget())
        assertEquals(UUID.fromString(DiscoveryProtocol.SERVICE_UUID), spec.serviceUuid)
        assertEquals(validPayload.toList(), spec.serviceData.toList())
        assertFalse(spec.includeDeviceName)
        assertFalse(spec.includeTxPowerLevel)
        assertEquals(0, spec.manufacturerDataEntries)
        assertEquals(0, spec.serviceUuidEntries)
        assertFalse(spec.scanResponsePresent)
    }

    @Test
    fun oversized_owned_service_data_is_rejected_by_the_legacy_bound() {
        val spec = LegacyAdvertisementSpec(
            serviceUuid = UUID.fromString(DiscoveryProtocol.SERVICE_UUID),
            serviceData = ByteArray(14),
            includeDeviceName = false,
            includeTxPowerLevel = false,
            manufacturerDataEntries = 0,
            serviceUuidEntries = 0,
            scanResponsePresent = false,
        )

        assertEquals(32, spec.ownedServiceDataBytes)
        assertFalse(spec.fitsLegacyBudget())
    }

    @Test
    fun production_spec_rejects_payloads_that_are_not_exactly_nine_bytes() {
        val exception = runCatching {
            LegacyAdvertisementSpec.forPayload(ByteArray(DiscoveryProtocol.PAYLOAD_SIZE - 1))
        }.exceptionOrNull()

        assertTrue(exception is IllegalArgumentException)
    }

    @Test
    fun production_spec_rejects_a_nine_byte_non_v1_payload() {
        val malformedVersionPayload = validPayload.copyOf().also { it[0] = 0x02 }

        val exception = runCatching {
            LegacyAdvertisementSpec.forPayload(malformedVersionPayload)
        }.exceptionOrNull()

        assertTrue(exception is IllegalArgumentException)
    }

    @Test
    fun data_too_large_platform_failure_maps_without_extended_advertising() {
        val platform = RecordingAdvertiserPlatform()
        val advertiser = AndroidBleAdvertiser.forTest(platform)
        val failures = mutableListOf<DiscoveryFailureCode>()

        runBlocking { advertiser.start(validPayload) { failures += it.code } }
        platform.fail(AdvertiseFailureCode.DATA_TOO_LARGE)

        assertEquals(listOf(DiscoveryFailureCode.DATA_TOO_LARGE), failures)
        assertEquals(1, platform.startCount)
        assertFalse(platform.extendedAdvertisingRequested)
    }

    @Test
    fun typed_advertiser_builder_contains_only_the_frozen_legacy_fields() {
        val source = Files.readString(
            Path.of("src/main/kotlin/com/r2h/spatiallink/ble/AndroidBleAdvertiser.kt"),
        )

        assertTrue(source.contains(".addServiceData("))
        assertTrue(source.contains(".setIncludeDeviceName(false)"))
        assertTrue(source.contains(".setIncludeTxPowerLevel(false)"))
        assertTrue(source.contains(".setConnectable(false)"))
        assertTrue(source.contains(".startAdvertising(settings, data, callback)"))
        assertFalse(source.contains(".addManufacturerData("))
        assertFalse(source.contains(".addServiceUuid("))
        assertFalse(source.contains("startAdvertisingSet"))
    }
}
