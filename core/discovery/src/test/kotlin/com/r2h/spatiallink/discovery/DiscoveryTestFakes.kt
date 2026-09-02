package com.r2h.spatiallink.discovery

import kotlinx.coroutines.CompletableDeferred

class TestMonotonicClock(
    private var nowElapsedMs: Long = 0L,
) : MonotonicClock {
    override fun elapsedRealtimeMs(): Long = nowElapsedMs

    fun setNow(elapsedMs: Long) {
        nowElapsedMs = elapsedMs
    }

    fun advanceBy(deltaMs: Long) {
        nowElapsedMs += deltaMs
    }
}

class TestBluetoothStateReader(
    var state: BluetoothState = BluetoothState.ENABLED,
) : BluetoothStateReader {
    override fun read(): BluetoothState = state
}

class TestDiscoveryPermissionReader : DiscoveryPermissionReader {
    private val missingByOperation = mutableMapOf<DiscoveryOperation, Set<DiscoveryPermission>>()

    override fun missing(operation: DiscoveryOperation): Set<DiscoveryPermission> =
        missingByOperation[operation].orEmpty()

    fun setMissing(
        operation: DiscoveryOperation,
        permissions: Set<DiscoveryPermission>,
    ) {
        missingByOperation[operation] = permissions.toSet()
    }
}

class TestDiscoverySessionIdSource(
    var nextResult: SessionIdGenerationResult,
) : DiscoverySessionIdSource {
    var invocationCount: Int = 0
        private set

    override fun next(): SessionIdGenerationResult {
        invocationCount += 1
        return nextResult
    }
}

class RecordingBleOperationHandle : BleOperationHandle {
    var stopCount: Int = 0
        private set

    override suspend fun stop() {
        stopCount += 1
    }
}

class StartOrderRecorder {
    private var nextOrdinal: Int = 0

    fun next(): Int {
        nextOrdinal += 1
        return nextOrdinal
    }
}

class RecordingBleScanner(
    private val handleFactory: () -> RecordingBleOperationHandle = { RecordingBleOperationHandle() },
    private val startOrderRecorder: StartOrderRecorder = StartOrderRecorder(),
) : BleScanner {
    var startCount: Int = 0
        private set
    var startedOrder: Int? = null
    var handle: RecordingBleOperationHandle? = null
    var startThrowable: Throwable? = null
    var onStart: suspend () -> Unit = {}
    var lastObservationCallback: ((DiscoveryObservation) -> Unit)? = null
    var lastFailureCallback: ((DiscoveryFailure) -> Unit)? = null

    override suspend fun start(
        onObservation: (DiscoveryObservation) -> Unit,
        onFailure: (DiscoveryFailure) -> Unit,
    ): BleOperationHandle {
        startCount += 1
        startedOrder = startOrderRecorder.next()
        onStart()
        startThrowable?.let { throw it }
        lastObservationCallback = onObservation
        lastFailureCallback = onFailure
        return handleFactory().also { created ->
            handle = created
        }
    }

    fun emitObservation(observation: DiscoveryObservation) {
        lastObservationCallback?.invoke(observation)
    }

    fun fail(failure: DiscoveryFailure) {
        lastFailureCallback?.invoke(failure)
    }
}

class RecordingBleAdvertiser(
    private val handleFactory: () -> RecordingBleOperationHandle = { RecordingBleOperationHandle() },
    private val startOrderRecorder: StartOrderRecorder = StartOrderRecorder(),
) : BleAdvertiser {
    var startCount: Int = 0
        private set
    var startedOrder: Int? = null
    var handle: RecordingBleOperationHandle? = null
    var startThrowable: Throwable? = null
    var onStart: suspend () -> Unit = {}
    var lastPayload: ByteArray? = null
    var lastFailureCallback: ((DiscoveryFailure) -> Unit)? = null

    override suspend fun start(
        payload: ByteArray,
        onFailure: (DiscoveryFailure) -> Unit,
    ): BleOperationHandle {
        startCount += 1
        startedOrder = startOrderRecorder.next()
        onStart()
        startThrowable?.let { throw it }
        lastPayload = payload.copyOf()
        lastFailureCallback = onFailure
        return handleFactory().also { created ->
            handle = created
        }
    }

    fun fail(failure: DiscoveryFailure) {
        lastFailureCallback?.invoke(failure)
    }
}
