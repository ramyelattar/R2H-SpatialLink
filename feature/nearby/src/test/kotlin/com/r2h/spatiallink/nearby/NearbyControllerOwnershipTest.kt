package com.r2h.spatiallink.nearby

import com.r2h.spatiallink.discovery.BleAdvertiser
import com.r2h.spatiallink.discovery.BleOperationHandle
import com.r2h.spatiallink.discovery.BleScanner
import com.r2h.spatiallink.discovery.BluetoothState
import com.r2h.spatiallink.discovery.BluetoothStateReader
import com.r2h.spatiallink.discovery.DiscoveryController
import com.r2h.spatiallink.discovery.DiscoveryControllerFactory
import com.r2h.spatiallink.discovery.DiscoveryFailure
import com.r2h.spatiallink.discovery.DiscoverySessionId
import com.r2h.spatiallink.discovery.DiscoverySessionIdSource
import com.r2h.spatiallink.discovery.DiscoveryPermissionReader
import com.r2h.spatiallink.discovery.DiscoveryPermission
import com.r2h.spatiallink.discovery.DiscoveryOperation
import com.r2h.spatiallink.discovery.DiscoveryObservation
import com.r2h.spatiallink.discovery.MonotonicClock
import com.r2h.spatiallink.discovery.SessionIdGenerationResult
import com.r2h.spatiallink.discovery.V1DiscoveryFrameCodec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NearbyControllerOwnershipTest {
    @Test
    fun factory_gives_a_fresh_usable_controller_after_previous_owner_closes() = runTest {
        val created = mutableListOf<DiscoveryController>()
        val factory = DiscoveryControllerFactory {
            newController().also { created += it }
        }

        val first = factory.create()
        val firstJob = launch { first.open() }
        runCurrent()
        first.close()
        runCurrent()
        firstJob.join()

        val second = factory.create()
        val secondJob = launch { second.open() }
        runCurrent()

        assertTrue(firstJob.isCompleted)
        assertNotSame(first, second)
        assertTrue(second.state.value is com.r2h.spatiallink.discovery.DiscoveryState.Active)
        assertTrue(created.size == 2)

        second.close()
        runCurrent()
        secondJob.join()
    }

    private fun newController(): DiscoveryController {
        val noOpHandle = object : BleOperationHandle {
            override suspend fun stop() = Unit
        }
        val scanner = object : BleScanner {
            override suspend fun start(
                onObservation: (DiscoveryObservation) -> Unit,
                onFailure: (DiscoveryFailure) -> Unit,
            ): BleOperationHandle = noOpHandle
        }
        val advertiser = object : BleAdvertiser {
            override suspend fun start(
                payload: ByteArray,
                onFailure: (DiscoveryFailure) -> Unit,
            ): BleOperationHandle = noOpHandle
        }
        return DiscoveryController(
            scanner = scanner,
            advertiser = advertiser,
            bluetoothStateReader = object : BluetoothStateReader {
                override fun read(): BluetoothState = BluetoothState.ENABLED
            },
            permissionReader = object : DiscoveryPermissionReader {
                override fun missing(operation: DiscoveryOperation): Set<DiscoveryPermission> = emptySet()
            },
            clock = object : MonotonicClock {
                override fun elapsedRealtimeMs(): Long = 0L
            },
            sessionIdSource = object : DiscoverySessionIdSource {
                override fun next(): SessionIdGenerationResult =
                    SessionIdGenerationResult.Success(sessionId)
            },
            frameCodec = V1DiscoveryFrameCodec,
        )
    }

    private companion object {
        val sessionId: DiscoverySessionId =
            DiscoverySessionId.fromBytes(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1))!!
    }
}
