package com.r2h.spatiallink.discovery

interface MonotonicClock {
    fun elapsedRealtimeMs(): Long
}

enum class BluetoothState {
    UNSUPPORTED,
    DISABLED,
    ENABLED,
}

interface BluetoothStateReader {
    fun read(): BluetoothState
}

interface DiscoveryPermissionReader {
    fun missing(operation: DiscoveryOperation): Set<DiscoveryPermission>
}

fun interface DiscoveryControllerFactory {
    fun create(): DiscoveryController
}

interface BleOperationHandle {
    suspend fun stop()
}

interface BleScanner {
    suspend fun start(
        onObservation: (DiscoveryObservation) -> Unit,
        onFailure: (DiscoveryFailure) -> Unit,
    ): BleOperationHandle
}

interface BleAdvertiser {
    suspend fun start(
        payload: ByteArray,
        onFailure: (DiscoveryFailure) -> Unit,
    ): BleOperationHandle
}
