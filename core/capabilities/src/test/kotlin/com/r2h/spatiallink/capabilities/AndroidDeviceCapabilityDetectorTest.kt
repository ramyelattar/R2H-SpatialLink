package com.r2h.spatiallink.capabilities

import com.r2h.spatiallink.common.DispatcherProvider
import com.r2h.spatiallink.model.CapabilityProbeResult
import com.r2h.spatiallink.model.CapabilityState
import com.r2h.spatiallink.model.CapabilitySubsystem
import com.r2h.spatiallink.model.DeviceCapabilityDetector
import com.r2h.spatiallink.model.RangingTechnology
import com.r2h.spatiallink.model.SpatialCapability
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidDeviceCapabilityDetectorTest {
    @Test
    fun one_failed_probe_does_not_discard_successful_sibling_results() = runTest {
        val detector: DeviceCapabilityDetector = AndroidDeviceCapabilityDetector(
            sdkInt = 36,
            androidVersion = "16",
            probes = listOf(
                FakeProbe(
                    subsystem = CapabilitySubsystem.BLUETOOTH,
                    spatial = mapOf(SpatialCapability.BLUETOOTH_LE to CapabilityState.AVAILABLE),
                ),
                FailingProbe(
                    subsystem = CapabilitySubsystem.WIFI,
                    spatialCapabilities = setOf(SpatialCapability.WIFI_DIRECT),
                ),
            ),
            dispatchers = FixedDispatcherProvider(StandardTestDispatcher(testScheduler)),
        )

        val snapshot = detector.detect()

        assertEquals(CapabilityState.AVAILABLE, snapshot.bluetoothLe)
        assertEquals(CapabilityState.UNKNOWN, snapshot.wifiDirect)
        assertTrue(
            snapshot.issues.any {
                it.subsystem == CapabilitySubsystem.WIFI &&
                    it.code == com.r2h.spatiallink.model.CapabilityIssueCode.UNEXPECTED_FAILURE
            },
        )
    }

    @Test
    fun cancellation_is_not_converted_to_a_capability_issue() = runTest {
        val detector = AndroidDeviceCapabilityDetector(
            sdkInt = 36,
            androidVersion = "16",
            probes = listOf(
                object : CapabilityProbe {
                    override val subsystem = CapabilitySubsystem.BLUETOOTH
                    override val spatialCapabilities = setOf(SpatialCapability.BLUETOOTH_LE)
                    override val rangingTechnologies = emptySet<RangingTechnology>()

                    override suspend fun inspect(): CapabilityProbeResult {
                        awaitCancellation()
                    }
                },
            ),
            dispatchers = FixedDispatcherProvider(StandardTestDispatcher(testScheduler)),
        )

        val job = launch { detector.detect() }
        runCurrent()
        job.cancelAndJoin()

        assertTrue(job.isCancelled)
    }

    private class FakeProbe(
        override val subsystem: CapabilitySubsystem,
        private val spatial: Map<SpatialCapability, CapabilityState>,
    ) : CapabilityProbe {
        override val spatialCapabilities: Set<SpatialCapability> = spatial.keys
        override val rangingTechnologies: Set<RangingTechnology> = emptySet()

        override suspend fun inspect(): CapabilityProbeResult = CapabilityProbeResult(
            subsystem = subsystem,
            spatialCapabilities = spatial,
        )
    }

    private class FailingProbe(
        override val subsystem: CapabilitySubsystem,
        override val spatialCapabilities: Set<SpatialCapability>,
    ) : CapabilityProbe {
        override val rangingTechnologies: Set<RangingTechnology> = emptySet()

        override suspend fun inspect(): CapabilityProbeResult {
            error("synthetic probe failure")
        }
    }

    private class FixedDispatcherProvider(
        override val default: kotlinx.coroutines.CoroutineDispatcher,
        override val io: kotlinx.coroutines.CoroutineDispatcher = default,
    ) : DispatcherProvider
}
