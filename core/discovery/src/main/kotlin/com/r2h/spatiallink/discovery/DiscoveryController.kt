package com.r2h.spatiallink.discovery

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DiscoveryController(
    private val scanner: BleScanner,
    private val advertiser: BleAdvertiser,
    private val bluetoothStateReader: BluetoothStateReader,
    private val permissionReader: DiscoveryPermissionReader,
    private val clock: MonotonicClock,
    private val sessionIdSource: DiscoverySessionIdSource,
    private val frameCodec: DiscoveryFrameCodec,
    private val onStatePublished: ((DiscoveryState, DiscoveryEphemeralSnapshot) -> Unit)? = null,
) {
    private val mutableState = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val state: StateFlow<DiscoveryState> = mutableState.asStateFlow()

    private val lifecycleMutex = Mutex()
    private var currentOwner: OwnerSession? = null

    suspend fun open() {
        val ownerJob = currentCoroutineContext()[Job]
            ?: error("DiscoveryController requires a parent Job")
        val ownerSession = OwnerSession(job = ownerJob)
        val cleanupCoordinator = CleanupCoordinator(
            ownerSession = ownerSession,
            releaseOwner = ::releaseOwner,
            publishState = ::publishState,
        )

        val acquiredOwner = lifecycleMutex.withLock {
            if (currentOwner != null) {
                false
            } else {
                currentOwner = ownerSession
                true
            }
        }
        if (!acquiredOwner) {
            return
        }

        try {
            val missingPermissions = missingDiscoveryPermissions()
            if (missingPermissions.isNotEmpty()) {
                cleanupCoordinator.cleanup(DiscoveryState.AwaitingPermission(missingPermissions))
                return
            }

            when (bluetoothStateReader.read()) {
                BluetoothState.UNSUPPORTED -> {
                    cleanupCoordinator.cleanup(DiscoveryState.Unavailable)
                    return
                }

                BluetoothState.DISABLED -> {
                    cleanupCoordinator.cleanup(DiscoveryState.BluetoothDisabled)
                    return
                }

                BluetoothState.ENABLED -> Unit
            }

            publishState(DiscoveryState.Starting)

            val generatedSessionId = when (val generation = sessionIdSource.next()) {
                is SessionIdGenerationResult.Success -> generation.value
                SessionIdGenerationResult.Failed -> {
                    cleanupCoordinator.cleanup(
                        DiscoveryState.Error(
                        DiscoveryFailure(DiscoveryFailureCode.SESSION_ID_GENERATION_FAILED),
                    )
                    )
                    return
                }
            }

            val startedAtElapsedMs = clock.elapsedRealtimeMs()
            val localPeerCache = PeerCache(
                localSessionId = generatedSessionId,
                clock = clock,
            )
            ownerSession.activeSessionId = generatedSessionId
            ownerSession.peerCache = localPeerCache
            val observationHandler = observationHandler(
                ownerJob = ownerJob,
                startedAtElapsedMs = startedAtElapsedMs,
                localPeerCache = localPeerCache,
            )
            val failureHandler = failureHandler(ownerJob)

            val scannerHandle = try {
                scanner.start(
                    onObservation = observationHandler,
                    onFailure = failureHandler,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                cleanupCoordinator.cleanup(
                    finalState = DiscoveryState.Error(
                        DiscoveryFailure(DiscoveryFailureCode.SCAN_START_FAILED),
                    ),
                )
                return
            }
            cleanupCoordinator.attachScannerHandle(scannerHandle)

            val advertiserHandle = try {
                advertiser.start(
                    payload = frameCodec.encode(generatedSessionId),
                    onFailure = failureHandler,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                cleanupCoordinator.cleanup(
                    finalState = DiscoveryState.Error(
                        DiscoveryFailure(DiscoveryFailureCode.ADVERTISE_START_FAILED),
                    ),
                )
                return
            }
            cleanupCoordinator.attachAdvertiserHandle(advertiserHandle)

            val remainingAfterStartup = remainingDurationMs(startedAtElapsedMs)
            if (remainingAfterStartup <= 0L) {
                cleanupCoordinator.cleanup(DiscoveryState.Completed)
                return
            }

            publishState(
                DiscoveryState.Active(
                    remainingDurationMs = remainingAfterStartup,
                    peers = localPeerCache.snapshot(),
                ),
            )

            while (true) {
                delay(DiscoveryProtocol.ACTIVE_TICK_MS)

                val missingDuringActive = missingDiscoveryPermissions()
                if (missingDuringActive.isNotEmpty()) {
                    cleanupCoordinator.cleanup(
                        finalState = DiscoveryState.AwaitingPermission(missingDuringActive),
                    )
                    return
                }

                when (bluetoothStateReader.read()) {
                    BluetoothState.UNSUPPORTED -> {
                        cleanupCoordinator.cleanup(DiscoveryState.Unavailable)
                        return
                    }

                    BluetoothState.DISABLED -> {
                        cleanupCoordinator.cleanup(DiscoveryState.BluetoothDisabled)
                        return
                    }

                    BluetoothState.ENABLED -> Unit
                }

                val remainingDurationMs = remainingDurationMs(startedAtElapsedMs)
                if (remainingDurationMs <= 0L) {
                    cleanupCoordinator.cleanup(DiscoveryState.Completed)
                    return
                }

                publishState(
                    DiscoveryState.Active(
                        remainingDurationMs = remainingDurationMs,
                        peers = localPeerCache.snapshot(),
                    ),
                )
            }
        } catch (terminal: TerminalCancellationException) {
            cleanupCoordinator.cleanup(terminal.finalState)
            throw terminal
        } catch (cancelled: CancellationException) {
            cleanupCoordinator.cleanup(DiscoveryState.Idle)
            throw cancelled
        } catch (unexpected: Throwable) {
            cleanupCoordinator.cleanup(
                DiscoveryState.Error(
                    DiscoveryFailure(
                        code = DiscoveryFailureCode.UNEXPECTED_PLATFORM_FAILURE,
                        detail = unexpected.message,
                    ),
                ),
            )
        }
    }

    suspend fun close() {
        val ownerToClose = lifecycleMutex.withLock { currentOwner }
        if (ownerToClose == null) {
            publishState(DiscoveryState.Idle)
            return
        }

        if (ownerToClose.job != currentCoroutineContext()[Job]) {
            publishState(DiscoveryState.Stopping)
        }
        ownerToClose.job.cancel(ManualCloseCancellationException)
        ownerToClose.job.join()
    }

    private fun publishState(state: DiscoveryState) {
        val owner = currentOwner
        onStatePublished?.invoke(
            state,
            DiscoveryEphemeralSnapshot(
                hasSessionToken = owner?.activeSessionId != null,
                peerCount = owner?.peerCache?.snapshot()?.size ?: 0,
            ),
        )
        mutableState.value = state
    }

    private suspend fun releaseOwner(ownerSession: OwnerSession) {
        lifecycleMutex.withLock {
            if (currentOwner !== ownerSession) {
                return
            }

            ownerSession.peerCache?.clear()
            ownerSession.peerCache = null
            ownerSession.activeSessionId = null
            currentOwner = null
        }
    }

    private fun missingDiscoveryPermissions(): Set<DiscoveryPermission> =
        permissionReader.missing(DiscoveryOperation.SCAN) +
            permissionReader.missing(DiscoveryOperation.ADVERTISE)

    private fun observationHandler(
        ownerJob: Job,
        startedAtElapsedMs: Long,
        localPeerCache: PeerCache,
    ): (DiscoveryObservation) -> Unit = { observation ->
        try {
            localPeerCache.observe(observation)
            publishState(
                DiscoveryState.Active(
                    remainingDurationMs = remainingDurationMs(startedAtElapsedMs),
                    peers = localPeerCache.snapshot(),
                ),
            )
        } catch (_: Throwable) {
            ownerJob.cancel(
                CallbackFailureCancellationException(
                    DiscoveryFailure(DiscoveryFailureCode.CALLBACK_FAILED),
                ),
            )
        }
    }

    private fun failureHandler(ownerJob: Job): (DiscoveryFailure) -> Unit = { failure ->
        ownerJob.cancel(CallbackFailureCancellationException(failure))
    }

    private fun remainingDurationMs(startedAtElapsedMs: Long): Long =
        (DiscoveryProtocol.SESSION_DURATION_MS - (clock.elapsedRealtimeMs() - startedAtElapsedMs))
            .coerceAtLeast(0L)

    private class OwnerSession(
        val job: Job,
    ) {
        var activeSessionId: DiscoverySessionId? = null
        var peerCache: PeerCache? = null
    }

    private class CleanupCoordinator(
        private val ownerSession: OwnerSession,
        private val releaseOwner: suspend (OwnerSession) -> Unit,
        private val publishState: (DiscoveryState) -> Unit,
    ) {
        private val cleaned = AtomicBoolean(false)
        private var scannerHandle: BleOperationHandle? = null
        private var advertiserHandle: BleOperationHandle? = null

        fun attachScannerHandle(handle: BleOperationHandle) {
            scannerHandle = handle
        }

        fun attachAdvertiserHandle(handle: BleOperationHandle) {
            advertiserHandle = handle
        }

        suspend fun cleanup(finalState: DiscoveryState) {
            if (!cleaned.compareAndSet(false, true)) {
                return
            }

            if (finalState == DiscoveryState.Idle) {
                publishState(DiscoveryState.Stopping)
            }
            scannerHandle?.stop()
            advertiserHandle?.stop()
            releaseOwner(ownerSession)
            publishState(finalState)
        }
    }

    private sealed class TerminalCancellationException(
        val finalState: DiscoveryState,
    ) : CancellationException()

    private class CallbackFailureCancellationException(
        failure: DiscoveryFailure,
    ) : TerminalCancellationException(DiscoveryState.Error(failure))

    private data object ManualCloseCancellationException :
        TerminalCancellationException(DiscoveryState.Idle)
}
