package com.r2h.spatiallink.nearby

import com.r2h.spatiallink.common.DispatcherProvider
import com.r2h.spatiallink.discovery.BleAdvertiser
import com.r2h.spatiallink.discovery.BleOperationHandle
import com.r2h.spatiallink.discovery.BleScanner
import com.r2h.spatiallink.discovery.BluetoothState
import com.r2h.spatiallink.discovery.BluetoothStateReader
import com.r2h.spatiallink.discovery.DiscoveryController
import com.r2h.spatiallink.discovery.DiscoveryFailure
import com.r2h.spatiallink.discovery.DiscoveryObservation
import com.r2h.spatiallink.discovery.DiscoveryOperation
import com.r2h.spatiallink.discovery.DiscoveryPermission
import com.r2h.spatiallink.discovery.DiscoveryPermissionReader
import com.r2h.spatiallink.discovery.DiscoveryProtocol
import com.r2h.spatiallink.discovery.DiscoverySessionId
import com.r2h.spatiallink.discovery.DiscoverySessionIdSource
import com.r2h.spatiallink.discovery.MonotonicClock
import com.r2h.spatiallink.discovery.SessionIdGenerationResult
import com.r2h.spatiallink.discovery.V1DiscoveryFrameCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NearbyViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_state_is_idle_and_emits_no_effect() = runTest {
        val fixture = fixture()
        val viewModel = fixture.viewModel(this)
        val effects = collectEffects(viewModel)

        assertEquals(NearbyUiState.Idle, viewModel.uiState.value)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun open_field_requests_only_scan_and_advertise_permissions() = runTest {
        val fixture = fixture()
        fixture.permissions.setMissing(DiscoveryOperation.SCAN, setOf(DiscoveryPermission.BLUETOOTH_SCAN))
        fixture.permissions.setMissing(
            DiscoveryOperation.ADVERTISE,
            setOf(DiscoveryPermission.BLUETOOTH_ADVERTISE),
        )
        val viewModel = fixture.viewModel(this)
        val effects = collectEffects(viewModel)

        viewModel.openField()
        runCurrent()

        assertEquals(
            listOf(
                NearbyEffect.RequestRuntimePermissions(
                    setOf(
                        DiscoveryPermission.BLUETOOTH_SCAN,
                        DiscoveryPermission.BLUETOOTH_ADVERTISE,
                    ),
                ),
            ),
            effects,
        )
        assertEquals(
            NearbyUiState.AwaitingPermission(
                setOf(
                    DiscoveryPermission.BLUETOOTH_SCAN,
                    DiscoveryPermission.BLUETOOTH_ADVERTISE,
                ),
            ),
            viewModel.uiState.value,
        )
        assertEquals(0, fixture.scanner.startCount)
        assertEquals(0, fixture.advertiser.startCount)
    }

    @Test
    fun enabled_discovery_does_not_request_bluetooth_connect() = runTest {
        val fixture = fixture()
        val viewModel = fixture.viewModel(this)
        val effects = collectEffects(viewModel)

        viewModel.openField()
        runCurrent()

        assertTrue(viewModel.uiState.value is NearbyUiState.Active)
        assertTrue(effects.none { effect ->
            effect is NearbyEffect.RequestRuntimePermissions &&
                DiscoveryPermission.BLUETOOTH_CONNECT in effect.permissions
        })
        viewModel.requestClose()
        runCurrent()
    }

    @Test
    fun disabled_bluetooth_requests_connect_only_at_enable_boundary() = runTest {
        val fixture = fixture(bluetoothState = BluetoothState.DISABLED)
        fixture.permissions.setMissing(
            DiscoveryOperation.ENABLE_BLUETOOTH,
            setOf(DiscoveryPermission.BLUETOOTH_CONNECT),
        )
        val viewModel = fixture.viewModel(this)
        val effects = collectEffects(viewModel)

        viewModel.openField()
        runCurrent()

        assertEquals(
            listOf(
                NearbyEffect.RequestRuntimePermissions(
                    setOf(DiscoveryPermission.BLUETOOTH_CONNECT),
                ),
            ),
            effects,
        )

        fixture.permissions.setMissing(DiscoveryOperation.ENABLE_BLUETOOTH, emptySet())
        viewModel.onRuntimePermissionsResult()
        runCurrent()

        assertEquals(
            listOf(
                NearbyEffect.RequestRuntimePermissions(
                    setOf(DiscoveryPermission.BLUETOOTH_CONNECT),
                ),
                NearbyEffect.RequestBluetoothEnable,
            ),
            effects,
        )
    }

    @Test
    fun denied_runtime_permission_returns_to_typed_waiting_state() = runTest {
        val fixture = fixture()
        fixture.permissions.setMissing(
            DiscoveryOperation.SCAN,
            setOf(DiscoveryPermission.BLUETOOTH_SCAN),
        )
        val viewModel = fixture.viewModel(this)
        collectEffects(viewModel)

        viewModel.openField()
        runCurrent()
        viewModel.onRuntimePermissionsResult()
        runCurrent()

        assertEquals(
            NearbyUiState.AwaitingPermission(setOf(DiscoveryPermission.BLUETOOTH_SCAN)),
            viewModel.uiState.value,
        )
    }

    @Test
    fun active_controller_state_is_exposed_as_anonymous_ui_state() = runTest {
        val fixture = fixture()
        val viewModel = fixture.viewModel(this)

        viewModel.openField()
        runCurrent()
        fixture.scanner.emitObservation(
            DiscoveryObservation(
                sessionId = sessionId(2),
                rssiDbm = -42,
                receivedAtElapsedMs = 0L,
            ),
        )
        runCurrent()

        assertEquals(
            NearbyUiState.Active(
                remainingDurationMs = DiscoveryProtocol.SESSION_DURATION_MS,
                peerCount = 1,
                strongestPeerRssiDbm = -42,
            ),
            viewModel.uiState.value,
        )
        viewModel.requestClose()
        runCurrent()
    }

    @Test
    fun request_close_emits_leave_only_after_controller_cleanup() = runTest {
        val fixture = fixture()
        val viewModel = fixture.viewModel(this)
        val effects = collectEffects(viewModel)

        viewModel.openField()
        runCurrent()
        viewModel.requestClose()
        runCurrent()

        assertEquals(NearbyUiState.Idle, viewModel.uiState.value)
        assertEquals(listOf(NearbyEffect.LeaveCompleted), effects)
        assertEquals(1, fixture.scanner.handle.stopCount)
        assertEquals(1, fixture.advertiser.handle.stopCount)
    }

    @Test
    fun foreground_loss_stops_active_field_without_restart_or_navigation_effect() = runTest {
        val fixture = fixture()
        val viewModel = fixture.viewModel(this)
        val effects = collectEffects(viewModel)

        viewModel.openField()
        runCurrent()
        viewModel.onForegroundLost()
        runCurrent()

        assertEquals(NearbyUiState.Idle, viewModel.uiState.value)
        assertTrue(effects.none { it is NearbyEffect.LeaveCompleted })
        assertEquals(1, fixture.scanner.handle.stopCount)
        assertEquals(1, fixture.advertiser.handle.stopCount)
        assertEquals(1, fixture.scanner.startCount)
        assertEquals(1, fixture.advertiser.startCount)
    }

    private fun fixture(bluetoothState: BluetoothState = BluetoothState.ENABLED): Fixture {
        val permissions = MutableTestPermissionReader()
        val scanner = RecordingScanner()
        val advertiser = RecordingAdvertiser()
        val controller = DiscoveryController(
            scanner = scanner,
            advertiser = advertiser,
            bluetoothStateReader = object : BluetoothStateReader {
                override fun read(): BluetoothState = bluetoothState
            },
            permissionReader = permissions,
            clock = TestClock(),
            sessionIdSource = object : DiscoverySessionIdSource {
                override fun next(): SessionIdGenerationResult =
                    SessionIdGenerationResult.Success(sessionId(1))
            },
            frameCodec = V1DiscoveryFrameCodec,
        )
        return Fixture(controller, permissions, scanner, advertiser)
    }

    private fun Fixture.viewModel(scope: TestScope): NearbyViewModel {
        val dispatcher = StandardTestDispatcher(scope.testScheduler)
        Dispatchers.setMain(dispatcher)
        return NearbyViewModel(
            controller = controller,
            permissionReader = permissions,
            dispatchers = TestDispatcherProvider(dispatcher),
        )
    }

    private fun TestScope.collectEffects(viewModel: NearbyViewModel): MutableList<NearbyEffect> {
        val effects = mutableListOf<NearbyEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        runCurrent()
        return effects
    }

    private data class Fixture(
        val controller: DiscoveryController,
        val permissions: MutableTestPermissionReader,
        val scanner: RecordingScanner,
        val advertiser: RecordingAdvertiser,
    )

    private class MutableTestPermissionReader : DiscoveryPermissionReader {
        private val missing = mutableMapOf<DiscoveryOperation, Set<DiscoveryPermission>>()

        override fun missing(operation: DiscoveryOperation): Set<DiscoveryPermission> =
            missing[operation].orEmpty()

        fun setMissing(operation: DiscoveryOperation, permissions: Set<DiscoveryPermission>) {
            missing[operation] = permissions
        }
    }

    private class TestDispatcherProvider(
        override val default: kotlinx.coroutines.CoroutineDispatcher,
    ) : DispatcherProvider {
        override val io: kotlinx.coroutines.CoroutineDispatcher = default
    }

    private class TestClock : MonotonicClock {
        override fun elapsedRealtimeMs(): Long = 0L
    }

    private class RecordingHandle : BleOperationHandle {
        var stopCount = 0
            private set

        override suspend fun stop() {
            stopCount += 1
        }
    }

    private class RecordingScanner : BleScanner {
        var startCount = 0
        val handle = RecordingHandle()
        private var observationCallback: ((DiscoveryObservation) -> Unit)? = null

        override suspend fun start(
            onObservation: (DiscoveryObservation) -> Unit,
            onFailure: (DiscoveryFailure) -> Unit,
        ): BleOperationHandle {
            startCount += 1
            observationCallback = onObservation
            return handle
        }

        fun emitObservation(observation: DiscoveryObservation) {
            observationCallback?.invoke(observation)
        }
    }

    private class RecordingAdvertiser : BleAdvertiser {
        var startCount = 0
        val handle = RecordingHandle()

        override suspend fun start(
            payload: ByteArray,
            onFailure: (DiscoveryFailure) -> Unit,
        ): BleOperationHandle {
            startCount += 1
            return handle
        }
    }

    private fun sessionId(lastByte: Int): DiscoverySessionId =
        DiscoverySessionId.fromBytes(byteArrayOf(0, 0, 0, 0, 0, 0, 0, lastByte.toByte()))!!
}
