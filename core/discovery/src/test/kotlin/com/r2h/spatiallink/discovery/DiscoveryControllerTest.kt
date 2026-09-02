package com.r2h.spatiallink.discovery

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryControllerTest {
    @Test
    fun missing_scan_and_advertise_permissions_publish_awaiting_permission_without_starting_ble() = runTest {
        val fixture = controllerFixture()
        fixture.permissionReader.setMissing(
            DiscoveryOperation.SCAN,
            setOf(DiscoveryPermission.BLUETOOTH_SCAN),
        )
        fixture.permissionReader.setMissing(
            DiscoveryOperation.ADVERTISE,
            setOf(DiscoveryPermission.BLUETOOTH_ADVERTISE),
        )

        val openJob = launch { fixture.controller.open() }
        runCurrent()

        assertEquals(
            DiscoveryState.AwaitingPermission(
                missingPermissions = setOf(
                    DiscoveryPermission.BLUETOOTH_SCAN,
                    DiscoveryPermission.BLUETOOTH_ADVERTISE,
                ),
            ),
            fixture.controller.state.value,
        )
        assertEquals(0, fixture.scanner.startCount)
        assertEquals(0, fixture.advertiser.startCount)

        openJob.cancel()
    }

    @Test
    fun disabled_adapter_publishes_bluetooth_disabled_without_calling_ble_adapters() = runTest {
        val fixture = controllerFixture()
        fixture.bluetoothStateReader.state = BluetoothState.DISABLED

        val openJob = launch { fixture.controller.open() }
        runCurrent()

        assertEquals(DiscoveryState.BluetoothDisabled, fixture.controller.state.value)
        assertEquals(0, fixture.scanner.startCount)
        assertEquals(0, fixture.advertiser.startCount)

        openJob.cancel()
    }

    @Test
    fun unsupported_adapter_publishes_unavailable_not_error() = runTest {
        val fixture = controllerFixture()
        fixture.bluetoothStateReader.state = BluetoothState.UNSUPPORTED

        val openJob = launch { fixture.controller.open() }
        runCurrent()

        assertEquals(DiscoveryState.Unavailable, fixture.controller.state.value)
        assertEquals(0, fixture.scanner.startCount)
        assertEquals(0, fixture.advertiser.startCount)

        openJob.cancel()
    }

    @Test
    fun successful_startup_starts_scanner_before_advertiser_and_publishes_the_session_payload() = runTest {
        val fixture = controllerFixture(sessionId = sessionId(7))

        val openJob = fixture.openInBackground(this)

        assertTrue(fixture.controller.state.value is DiscoveryState.Active)
        assertEquals(1, fixture.scanner.startCount)
        assertEquals(1, fixture.advertiser.startCount)
        assertEquals(1, fixture.scanner.startedOrder)
        assertEquals(2, fixture.advertiser.startedOrder)
        assertArrayEquals(
            V1DiscoveryFrameCodec.encode(sessionId(7)),
            fixture.advertiser.lastPayload,
        )

        openJob.cancel()
    }

    @Test
    fun startup_publishes_active_with_calculated_remaining_time_after_startup_delay() = runTest {
        val fixture = controllerFixture()
        fixture.scanner.onStart = { fixture.clock.advanceBy(15L) }
        fixture.advertiser.onStart = { fixture.clock.advanceBy(10L) }

        val openJob = fixture.openInBackground(this)

        assertEquals(
            DiscoveryState.Active(
                remainingDurationMs = DiscoveryProtocol.SESSION_DURATION_MS - 25L,
                peers = emptyList(),
            ),
            fixture.controller.state.value,
        )

        openJob.cancel()
    }

    @Test
    fun scanner_startup_failure_does_not_start_the_advertiser() = runTest {
        val fixture = controllerFixture()
        fixture.scanner.startThrowable = IllegalStateException("scanner boom")

        val openJob = launch { fixture.controller.open() }
        runCurrent()

        assertEquals(1, fixture.scanner.startCount)
        assertEquals(0, fixture.advertiser.startCount)
        assertEquals(
            DiscoveryState.Error(
                DiscoveryFailure(DiscoveryFailureCode.SCAN_START_FAILED),
            ),
            fixture.controller.state.value,
        )

        openJob.join()
    }

    @Test
    fun advertiser_startup_failure_stops_the_scanner_handle_once() = runTest {
        val fixture = controllerFixture()
        fixture.advertiser.startThrowable = IllegalStateException("advertiser boom")

        val openJob = launch { fixture.controller.open() }
        runCurrent()

        assertEquals(1, fixture.scanner.startCount)
        assertEquals(1, fixture.advertiser.startCount)
        assertEquals(1, fixture.scanner.handle?.stopCount)
        assertEquals(
            DiscoveryState.Error(
                DiscoveryFailure(DiscoveryFailureCode.ADVERTISE_START_FAILED),
            ),
            fixture.controller.state.value,
        )
        assertTrue(
            fixture.publications.any {
                it.state ==
                    DiscoveryState.Error(
                        DiscoveryFailure(DiscoveryFailureCode.ADVERTISE_START_FAILED),
                    ) &&
                    it.scannerStopCount == 1
            },
        )

        openJob.join()
    }

    @Test
    fun callback_failure_stops_both_handles_once_and_publishes_typed_error() = runTest {
        val fixture = controllerFixture()
        val openJob = fixture.openInBackground(this)
        fixture.scanner.emitObservation(
            DiscoveryObservation(
                sessionId = sessionId(42),
                rssiDbm = -60,
                receivedAtElapsedMs = 0L,
            ),
        )
        runCurrent()

        fixture.scanner.fail(DiscoveryFailure(DiscoveryFailureCode.CALLBACK_FAILED))
        runCurrent()

        assertEquals(1, fixture.scanner.handle?.stopCount)
        assertEquals(1, fixture.advertiser.handle?.stopCount)
        assertEquals(
            DiscoveryState.Error(
                DiscoveryFailure(DiscoveryFailureCode.CALLBACK_FAILED),
            ),
            fixture.controller.state.value,
        )
        assertTrue(
            fixture.publications.any {
                it.state ==
                    DiscoveryState.Error(
                        DiscoveryFailure(DiscoveryFailureCode.CALLBACK_FAILED),
                    ) &&
                    !it.hasSessionToken &&
                    it.peerCount == 0 &&
                    it.scannerStopCount == 1 &&
                    it.advertiserStopCount == 1
            },
        )

        openJob.join()
    }

    @Test
    fun timeout_after_thirty_seconds_stops_both_handles_once_and_publishes_completed() = runTest {
        val fixture = controllerFixture()
        val openJob = fixture.openInBackground(this)
        fixture.scanner.emitObservation(
            DiscoveryObservation(
                sessionId = sessionId(42),
                rssiDbm = -60,
                receivedAtElapsedMs = 0L,
            ),
        )
        runCurrent()

        fixture.clock.advanceBy(DiscoveryProtocol.SESSION_DURATION_MS)
        advanceTimeBy(DiscoveryProtocol.SESSION_DURATION_MS)
        runCurrent()

        assertEquals(1, fixture.scanner.handle?.stopCount)
        assertEquals(1, fixture.advertiser.handle?.stopCount)
        assertEquals(DiscoveryState.Completed, fixture.controller.state.value)
        assertTrue(
            fixture.publications.any {
                it.state == DiscoveryState.Completed &&
                    !it.hasSessionToken &&
                    it.peerCount == 0 &&
                    it.scannerStopCount == 1 &&
                    it.advertiserStopCount == 1
            },
        )

        openJob.join()
    }

    @Test
    fun startup_completion_at_session_boundary_publishes_completed_without_active_state() = runTest {
        val fixture = controllerFixture()
        fixture.scanner.onStart = {
            fixture.clock.advanceBy(DiscoveryProtocol.SESSION_DURATION_MS)
        }

        val openJob = fixture.openInBackground(this)

        assertEquals(1, fixture.scanner.startCount)
        assertEquals(1, fixture.advertiser.startCount)
        assertEquals(1, fixture.scanner.handle?.stopCount)
        assertEquals(1, fixture.advertiser.handle?.stopCount)
        assertEquals(DiscoveryState.Completed, fixture.controller.state.value)
        assertTrue(fixture.publications.none { it.state is DiscoveryState.Active })
        assertTrue(
            fixture.publications.any {
                it.state == DiscoveryState.Completed &&
                    !it.hasSessionToken &&
                    it.peerCount == 0 &&
                    it.scannerStopCount == 1 &&
                    it.advertiserStopCount == 1
            },
        )

        openJob.join()
    }

    @Test
    fun startup_completion_after_session_boundary_publishes_completed_without_active_state() = runTest {
        val fixture = controllerFixture()
        fixture.scanner.onStart = {
            fixture.clock.advanceBy(DiscoveryProtocol.SESSION_DURATION_MS + 1L)
        }

        val openJob = fixture.openInBackground(this)

        assertEquals(1, fixture.scanner.startCount)
        assertEquals(1, fixture.advertiser.startCount)
        assertEquals(1, fixture.scanner.handle?.stopCount)
        assertEquals(1, fixture.advertiser.handle?.stopCount)
        assertEquals(DiscoveryState.Completed, fixture.controller.state.value)
        assertTrue(fixture.publications.none { it.state is DiscoveryState.Active })
        assertTrue(
            fixture.publications.any {
                it.state == DiscoveryState.Completed &&
                    !it.hasSessionToken &&
                    it.peerCount == 0 &&
                    it.scannerStopCount == 1 &&
                    it.advertiserStopCount == 1
            },
        )

        openJob.join()
    }

    @Test
    fun cancellation_cleans_up_and_is_not_converted_to_a_product_error() = runTest {
        val fixture = controllerFixture()
        val openJob = fixture.openInBackground(this)

        openJob.cancel()
        runCurrent()

        assertEquals(1, fixture.scanner.handle?.stopCount)
        assertEquals(1, fixture.advertiser.handle?.stopCount)
        assertFalse(fixture.controller.state.value is DiscoveryState.Error)
    }

    @Test
    fun close_runs_cleanup_once_and_returns_to_idle() = runTest {
        val fixture = controllerFixture()
        val openJob = fixture.openInBackground(this)

        fixture.controller.close()
        runCurrent()

        assertEquals(1, fixture.scanner.handle?.stopCount)
        assertEquals(1, fixture.advertiser.handle?.stopCount)
        assertEquals(DiscoveryState.Idle, fixture.controller.state.value)

        openJob.join()
    }

    @Test
    fun second_open_while_owner_session_is_starting_is_a_no_op_and_does_not_replace_cleanup_ownership() = runTest {
        val startupGate = CompletableDeferred<Unit>()
        val startupEntered = CompletableDeferred<Unit>()
        val fixture = controllerFixture()
        fixture.scanner.onStart = {
            startupEntered.complete(Unit)
            startupGate.await()
        }

        val firstOpenJob = launch { fixture.controller.open() }
        startupEntered.await()
        runCurrent()

        val secondOpenJob = launch { fixture.controller.open() }
        runCurrent()

        assertTrue(secondOpenJob.isCompleted)
        assertEquals(1, fixture.sessionIdSource.invocationCount)
        assertEquals(1, fixture.scanner.startCount)
        assertEquals(0, fixture.advertiser.startCount)

        startupGate.complete(Unit)
        runCurrent()

        assertTrue(fixture.controller.state.value is DiscoveryState.Active)
        assertEquals(1, fixture.advertiser.startCount)

        fixture.controller.close()
        runCurrent()

        assertEquals(1, fixture.scanner.handle?.stopCount)
        assertEquals(1, fixture.advertiser.handle?.stopCount)
        assertEquals(DiscoveryState.Idle, fixture.controller.state.value)

        firstOpenJob.join()
        secondOpenJob.join()
    }

    @Test
    fun second_open_while_owner_session_is_active_is_a_no_op_and_original_owner_closes_once() = runTest {
        val fixture = controllerFixture()
        val firstOpenJob = fixture.openInBackground(this)

        val secondOpenJob = launch { fixture.controller.open() }
        runCurrent()

        assertTrue(secondOpenJob.isCompleted)
        assertEquals(1, fixture.sessionIdSource.invocationCount)
        assertEquals(1, fixture.scanner.startCount)
        assertEquals(1, fixture.advertiser.startCount)

        fixture.controller.close()
        runCurrent()

        assertEquals(1, fixture.scanner.handle?.stopCount)
        assertEquals(1, fixture.advertiser.handle?.stopCount)
        assertEquals(DiscoveryState.Idle, fixture.controller.state.value)

        firstOpenJob.join()
        secondOpenJob.join()
    }

    @Test
    fun generation_failure_publishes_typed_error_and_never_starts_adapters() = runTest {
        val fixture = controllerFixture(
            generationResult = SessionIdGenerationResult.Failed,
        )

        val openJob = launch { fixture.controller.open() }
        runCurrent()

        assertEquals(
            DiscoveryState.Error(
                DiscoveryFailure(DiscoveryFailureCode.SESSION_ID_GENERATION_FAILED),
            ),
            fixture.controller.state.value,
        )
        assertEquals(0, fixture.scanner.startCount)
        assertEquals(0, fixture.advertiser.startCount)

        openJob.join()
    }

    @Test
    fun active_scan_permission_loss_publishes_cleared_awaiting_permission_after_exactly_once_cleanup() = runTest {
        val fixture = controllerFixture()
        val openJob = fixture.openInBackground(this)
        fixture.scanner.emitObservation(
            DiscoveryObservation(
                sessionId = sessionId(42),
                rssiDbm = -60,
                receivedAtElapsedMs = 0L,
            ),
        )
        runCurrent()
        assertTrue(
            fixture.publications.any {
                it.state is DiscoveryState.Active && it.hasSessionToken && it.peerCount == 1
            },
        )

        fixture.permissionReader.setMissing(
            DiscoveryOperation.SCAN,
            setOf(DiscoveryPermission.BLUETOOTH_SCAN),
        )
        fixture.clock.advanceBy(DiscoveryProtocol.ACTIVE_TICK_MS)
        advanceTimeBy(DiscoveryProtocol.ACTIVE_TICK_MS)
        runCurrent()

        assertEquals(1, fixture.scanner.handle?.stopCount)
        assertEquals(1, fixture.advertiser.handle?.stopCount)
        assertTrue(
            fixture.publications.any {
                it.state ==
                    DiscoveryState.AwaitingPermission(
                        missingPermissions = setOf(DiscoveryPermission.BLUETOOTH_SCAN),
                    ) &&
                    !it.hasSessionToken &&
                    it.peerCount == 0 &&
                    it.scannerStopCount == 1 &&
                    it.advertiserStopCount == 1
            },
        )
        assertEquals(
            DiscoveryState.AwaitingPermission(
                missingPermissions = setOf(DiscoveryPermission.BLUETOOTH_SCAN),
            ),
            fixture.controller.state.value,
        )

        openJob.join()
    }

    @Test
    fun active_advertise_permission_loss_publishes_cleared_awaiting_permission_after_exactly_once_cleanup() = runTest {
        val fixture = controllerFixture()
        val openJob = fixture.openInBackground(this)
        fixture.scanner.emitObservation(
            DiscoveryObservation(
                sessionId = sessionId(42),
                rssiDbm = -60,
                receivedAtElapsedMs = 0L,
            ),
        )
        runCurrent()
        assertTrue(
            fixture.publications.any {
                it.state is DiscoveryState.Active && it.hasSessionToken && it.peerCount == 1
            },
        )

        fixture.permissionReader.setMissing(
            DiscoveryOperation.ADVERTISE,
            setOf(DiscoveryPermission.BLUETOOTH_ADVERTISE),
        )
        fixture.clock.advanceBy(DiscoveryProtocol.ACTIVE_TICK_MS)
        advanceTimeBy(DiscoveryProtocol.ACTIVE_TICK_MS)
        runCurrent()

        assertEquals(1, fixture.scanner.handle?.stopCount)
        assertEquals(1, fixture.advertiser.handle?.stopCount)
        assertTrue(
            fixture.publications.any {
                it.state ==
                    DiscoveryState.AwaitingPermission(
                        missingPermissions = setOf(DiscoveryPermission.BLUETOOTH_ADVERTISE),
                    ) &&
                    !it.hasSessionToken &&
                    it.peerCount == 0 &&
                    it.scannerStopCount == 1 &&
                    it.advertiserStopCount == 1
            },
        )
        assertEquals(
            DiscoveryState.AwaitingPermission(
                missingPermissions = setOf(DiscoveryPermission.BLUETOOTH_ADVERTISE),
            ),
            fixture.controller.state.value,
        )

        openJob.join()
    }

    @Test
    fun active_bluetooth_disabled_publishes_cleared_bluetooth_disabled_after_exactly_once_cleanup() = runTest {
        val fixture = controllerFixture()
        val openJob = fixture.openInBackground(this)
        fixture.scanner.emitObservation(
            DiscoveryObservation(
                sessionId = sessionId(42),
                rssiDbm = -60,
                receivedAtElapsedMs = 0L,
            ),
        )
        runCurrent()
        assertTrue(
            fixture.publications.any {
                it.state is DiscoveryState.Active && it.hasSessionToken && it.peerCount == 1
            },
        )

        fixture.bluetoothStateReader.state = BluetoothState.DISABLED
        fixture.clock.advanceBy(DiscoveryProtocol.ACTIVE_TICK_MS)
        advanceTimeBy(DiscoveryProtocol.ACTIVE_TICK_MS)
        runCurrent()

        assertEquals(1, fixture.scanner.handle?.stopCount)
        assertEquals(1, fixture.advertiser.handle?.stopCount)
        assertTrue(
            fixture.publications.any {
                it.state == DiscoveryState.BluetoothDisabled &&
                    !it.hasSessionToken &&
                    it.peerCount == 0 &&
                    it.scannerStopCount == 1 &&
                    it.advertiserStopCount == 1
            },
        )
        assertEquals(DiscoveryState.BluetoothDisabled, fixture.controller.state.value)

        openJob.join()
    }

    private fun controllerFixture(
        sessionId: DiscoverySessionId = sessionId(1),
        generationResult: SessionIdGenerationResult = SessionIdGenerationResult.Success(sessionId),
    ): ControllerFixture {
        val clock = TestMonotonicClock()
        val bluetoothStateReader = TestBluetoothStateReader()
        val permissionReader = TestDiscoveryPermissionReader()
        val startOrderRecorder = StartOrderRecorder()
        val scanner = RecordingBleScanner(startOrderRecorder = startOrderRecorder)
        val advertiser = RecordingBleAdvertiser(startOrderRecorder = startOrderRecorder)
        val sessionIdSource = TestDiscoverySessionIdSource(generationResult)
        val publications = mutableListOf<PublishedStateRecord>()
        val controller = DiscoveryController(
            scanner = scanner,
            advertiser = advertiser,
            bluetoothStateReader = bluetoothStateReader,
            permissionReader = permissionReader,
            clock = clock,
            sessionIdSource = sessionIdSource,
            frameCodec = V1DiscoveryFrameCodec,
            onStatePublished = { state, snapshot ->
                publications += PublishedStateRecord(
                    state = state,
                    hasSessionToken = snapshot.hasSessionToken,
                    peerCount = snapshot.peerCount,
                    scannerStopCount = scanner.handle?.stopCount ?: 0,
                    advertiserStopCount = advertiser.handle?.stopCount ?: 0,
                )
            },
        )

        return ControllerFixture(
            controller = controller,
            clock = clock,
            bluetoothStateReader = bluetoothStateReader,
            permissionReader = permissionReader,
            scanner = scanner,
            advertiser = advertiser,
            sessionIdSource = sessionIdSource,
            publications = publications,
        )
    }

    private fun ControllerFixture.openInBackground(scope: TestScope): Job {
        val openJob = scope.launch { controller.open() }
        scope.runCurrent()
        return openJob
    }

    private data class ControllerFixture(
        val controller: DiscoveryController,
        val clock: TestMonotonicClock,
        val bluetoothStateReader: TestBluetoothStateReader,
        val permissionReader: TestDiscoveryPermissionReader,
        val scanner: RecordingBleScanner,
        val advertiser: RecordingBleAdvertiser,
        val sessionIdSource: TestDiscoverySessionIdSource,
        val publications: List<PublishedStateRecord>,
    )

    private data class PublishedStateRecord(
        val state: DiscoveryState,
        val hasSessionToken: Boolean,
        val peerCount: Int,
        val scannerStopCount: Int,
        val advertiserStopCount: Int,
    )

    private fun sessionId(lastByte: Int): DiscoverySessionId =
        DiscoverySessionId.fromBytes(byteArrayOf(0, 0, 0, 0, 0, 0, 0, lastByte.toByte()))!!
}
