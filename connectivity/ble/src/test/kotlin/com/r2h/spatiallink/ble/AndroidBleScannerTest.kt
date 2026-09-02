package com.r2h.spatiallink.ble

import com.r2h.spatiallink.discovery.DiscoveryFailureCode
import com.r2h.spatiallink.discovery.DiscoveryProtocol
import com.r2h.spatiallink.discovery.MonotonicClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AndroidBleScannerTest {
    private val payload = byteArrayOf(
        DiscoveryProtocol.V1_VERSION,
        1, 2, 3, 4, 5, 6, 7, 8,
    )

    @Test
    fun scanner_uses_fixed_uuid_version_mask_low_latency_and_no_identity_filters() {
        val platform = RecordingScannerPlatform()
        val scanner = AndroidBleScanner.forTest(platform, FixedClock(1234L))

        runBlocking { scanner.start({}, {}) }

        val request = platform.lastRequest!!
        assertEquals(BleAndroidConstants.serviceUuid, request.serviceUuid)
        assertEquals(DiscoveryProtocol.V1_VERSION, request.versionByte)
        assertEquals(0xFF.toByte(), request.versionMask)
        assertEquals(BleScanMode.LOW_LATENCY, request.scanMode)
        assertEquals(0L, request.reportDelayMs)
        assertTrue(request.hasOnlyServiceDataFilter)
        assertTrue(!request.hasAddressFilter)
        assertTrue(!request.hasDeviceNameFilter)
        assertTrue(!request.hasManufacturerFilter)
    }

    @Test
    fun valid_service_data_becomes_anonymous_observation_with_monotonic_time() {
        val platform = RecordingScannerPlatform()
        val scanner = AndroidBleScanner.forTest(platform, FixedClock(9876L))
        val observations = mutableListOf<com.r2h.spatiallink.discovery.DiscoveryObservation>()

        runBlocking {
            scanner.start({ observation ->
                observations += observation
            }, {})
        }
        platform.emit(payload, -47)

        assertEquals(1, observations.size)
        assertEquals(
            com.r2h.spatiallink.discovery.V1DiscoveryFrameCodec.decode(payload),
            observations.single().sessionId,
        )
        assertEquals(-47, observations.single().rssiDbm)
        assertEquals(9876L, observations.single().receivedAtElapsedMs)
    }

    @Test
    fun malformed_payload_is_ignored_without_failure() {
        val platform = RecordingScannerPlatform()
        val scanner = AndroidBleScanner.forTest(platform, FixedClock(1L))
        val failures = mutableListOf<DiscoveryFailureCode>()
        val observations = mutableListOf<com.r2h.spatiallink.discovery.DiscoveryObservation>()

        runBlocking { scanner.start({ observations += it }, { failures += it.code }) }
        platform.emit(byteArrayOf(DiscoveryProtocol.V1_VERSION), -30)

        assertTrue(observations.isEmpty())
        assertTrue(failures.isEmpty())
    }

    @Test
    fun repeated_stop_invokes_platform_cleanup_once() {
        val platform = RecordingScannerPlatform()
        val scanner = AndroidBleScanner.forTest(platform, FixedClock(1L))
        val handle = runBlocking { scanner.start({}, {}) }

        runBlocking {
            handle.stop()
            handle.stop()
        }

        assertEquals(1, platform.stopCount)
    }

    @Test
    fun scan_callback_failure_maps_and_stops_the_callback_handle_once() {
        val platform = RecordingScannerPlatform()
        val scanner = AndroidBleScanner.forTest(platform, FixedClock(1L))
        val failures = mutableListOf<DiscoveryFailureCode>()

        runBlocking { scanner.start({}, { failures += it.code }) }
        platform.fail(7)

        assertEquals(listOf(DiscoveryFailureCode.SCAN_START_FAILED), failures)
        assertEquals(1, platform.stopCount)
    }

    @Test
    fun attach_and_stop_interleaving_stops_the_platform_once() {
        val firstPlatformHandle = BlockingPlatformHandle()
        val operation = IdempotentBleOperationHandle()
        operation.attach(firstPlatformHandle)

        val stopper = Thread { operation.stopBlocking() }.also { it.start() }
        assertTrue(firstPlatformHandle.stopEntered.await(1, TimeUnit.SECONDS))

        val latePlatformHandle = RecordingPlatformHandle()
        val attachFinished = CountDownLatch(1)
        val attacher = Thread {
            operation.attach(latePlatformHandle)
            attachFinished.countDown()
        }.also { it.start() }

        assertFalse(attachFinished.await(100, TimeUnit.MILLISECONDS))
        firstPlatformHandle.releaseStop.countDown()
        stopper.join(1_000)
        attacher.join(1_000)

        assertEquals(1, firstPlatformHandle.stopCount)
        assertEquals(0, latePlatformHandle.stopCount)
    }
}

private class FixedClock(private val value: Long) : MonotonicClock {
    override fun elapsedRealtimeMs(): Long = value
}

private class BlockingPlatformHandle : AndroidBlePlatformHandle {
    val stopEntered = CountDownLatch(1)
    val releaseStop = CountDownLatch(1)
    var stopCount: Int = 0
        private set

    override fun stop() {
        stopCount += 1
        stopEntered.countDown()
        releaseStop.await(1, TimeUnit.SECONDS)
    }
}
