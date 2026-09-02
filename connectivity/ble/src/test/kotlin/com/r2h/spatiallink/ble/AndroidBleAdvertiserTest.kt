package com.r2h.spatiallink.ble

import android.bluetooth.le.AdvertiseCallback
import com.r2h.spatiallink.discovery.DiscoveryFailureCode
import com.r2h.spatiallink.discovery.DiscoveryProtocol
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidBleAdvertiserTest {
    private val payload = byteArrayOf(
        DiscoveryProtocol.V1_VERSION,
        1, 2, 3, 4, 5, 6, 7, 8,
    )

    @Test
    fun successful_start_captures_exact_legacy_spec() {
        val platform = RecordingAdvertiserPlatform()
        val advertiser = AndroidBleAdvertiser.forTest(platform)

        runBlocking { advertiser.start(payload) {} }

        val spec = platform.lastSpec!!
        assertEquals(27, spec.ownedServiceDataBytes)
        assertFalse(spec.includeDeviceName)
        assertFalse(spec.includeTxPowerLevel)
        assertEquals(0, spec.manufacturerDataEntries)
        assertEquals(0, spec.serviceUuidEntries)
        assertFalse(spec.scanResponsePresent)
        assertFalse(platform.extendedAdvertisingRequested)
    }

    @Test
    fun data_too_large_callback_maps_and_closes_handle() {
        val platform = RecordingAdvertiserPlatform()
        val advertiser = AndroidBleAdvertiser.forTest(platform)
        val failures = mutableListOf<DiscoveryFailureCode>()
        val handle = runBlocking { advertiser.start(payload) { failures += it.code } }

        platform.fail(AdvertiseFailureCode.DATA_TOO_LARGE)
        runBlocking { handle.stop() }

        assertEquals(listOf(DiscoveryFailureCode.DATA_TOO_LARGE), failures)
        assertEquals(1, platform.stopCount)
    }

    @Test
    fun repeated_stop_calls_platform_once() {
        val platform = RecordingAdvertiserPlatform()
        val advertiser = AndroidBleAdvertiser.forTest(platform)
        val handle = runBlocking { advertiser.start(payload) {} }

        runBlocking {
            handle.stop()
            handle.stop()
        }

        assertEquals(1, platform.stopCount)
    }

    @Test
    fun budget_failure_prevents_platform_start() {
        val platform = RecordingAdvertiserPlatform()
        val advertiser = AndroidBleAdvertiser.forTest(platform)

        runBlocking { advertiser.start(ByteArray(DiscoveryProtocol.PAYLOAD_SIZE + 5)) {} }

        assertEquals(0, platform.startCount)
    }

    @Test
    fun synchronous_platform_exception_maps_to_typed_start_failure() {
        val platform = RecordingAdvertiserPlatform().also {
            it.startThrowable = IllegalStateException("advertiser unavailable")
        }
        val advertiser = AndroidBleAdvertiser.forTest(platform)
        val failures = mutableListOf<DiscoveryFailureCode>()

        runBlocking { advertiser.start(payload) { failures += it.code } }

        assertEquals(listOf(DiscoveryFailureCode.ADVERTISE_START_FAILED), failures)
        assertEquals(0, platform.stopCount)
    }
}
