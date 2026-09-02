package com.r2h.spatiallink.ble

import android.annotation.SuppressLint
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.os.SystemClock
import com.r2h.spatiallink.discovery.BleOperationHandle
import com.r2h.spatiallink.discovery.BleScanner
import com.r2h.spatiallink.discovery.DiscoveryFailure
import com.r2h.spatiallink.discovery.DiscoveryFailureCode
import com.r2h.spatiallink.discovery.DiscoveryObservation
import com.r2h.spatiallink.discovery.DiscoveryProtocol
import com.r2h.spatiallink.discovery.MonotonicClock
import com.r2h.spatiallink.discovery.V1DiscoveryFrameCodec

class AndroidBleScanner private constructor(
    private val platform: AndroidBleScannerPlatform,
    private val clock: MonotonicClock,
) : BleScanner {
    constructor(
        context: android.content.Context,
        adapterProvider: AndroidBleAdapterProvider,
        clock: MonotonicClock,
    ) : this(
        platform = TypedAndroidBleScannerPlatform(adapterProvider),
        clock = clock,
    )

    override suspend fun start(
        onObservation: (DiscoveryObservation) -> Unit,
        onFailure: (DiscoveryFailure) -> Unit,
    ): BleOperationHandle = startWith(platform, clock, onObservation, onFailure)

    companion object {
        internal fun forTest(
            platform: AndroidBleScannerPlatform,
            clock: MonotonicClock,
        ): AndroidBleScanner = AndroidBleScanner(platform, clock)
    }
}

private fun AndroidBleScanner.startWith(
    platform: AndroidBleScannerPlatform,
    clock: MonotonicClock,
    onObservation: (DiscoveryObservation) -> Unit,
    onFailure: (DiscoveryFailure) -> Unit,
): BleOperationHandle {
    val handle = IdempotentBleOperationHandle()
    val request = BleScanRequest(
        serviceUuid = BleAndroidConstants.serviceUuid,
        versionByte = DiscoveryProtocol.V1_VERSION,
        versionMask = 0xFF.toByte(),
        scanMode = BleScanMode.LOW_LATENCY,
        reportDelayMs = 0L,
        hasOnlyServiceDataFilter = true,
        hasAddressFilter = false,
        hasDeviceNameFilter = false,
        hasManufacturerFilter = false,
    )
    val platformHandle = try {
        platform.start(
            request = request,
            onResult = { payload, rssiDbm ->
                V1DiscoveryFrameCodec.decode(payload)?.let { sessionId ->
                    onObservation(
                        DiscoveryObservation(
                            sessionId = sessionId,
                            rssiDbm = rssiDbm,
                            receivedAtElapsedMs = clock.elapsedRealtimeMs(),
                        ),
                    )
                }
            },
            onFailure = { errorCode ->
                handle.stopBlocking()
                onFailure(
                    DiscoveryFailure(
                        code = DiscoveryFailureCode.SCAN_START_FAILED,
                        detail = "Bluetooth scan callback failure: $errorCode",
                    ),
                )
            },
        )
    } catch (throwable: Throwable) {
        onFailure(
            DiscoveryFailure(
                code = DiscoveryFailureCode.SCAN_START_FAILED,
                detail = throwable.javaClass.simpleName,
            ),
        )
        return NoOpBleOperationHandle
    }
    return handle.also { it.attach(platformHandle) }
}

private class TypedAndroidBleScannerPlatform(
    private val adapterProvider: AndroidBleAdapterProvider,
) : AndroidBleScannerPlatform {
    override fun start(
        request: BleScanRequest,
        onResult: (ByteArray, Int) -> Unit,
        onFailure: (Int) -> Unit,
    ): AndroidBlePlatformHandle {
        val scanner: BluetoothLeScanner =
            adapterProvider.bluetoothAdapter()?.bluetoothLeScanner
                ?: throw IllegalStateException("Bluetooth LE scanner unavailable")
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val payload = result.scanRecord
                    ?.getServiceData(ParcelUuid(request.serviceUuid))
                    ?: return
                onResult(payload.copyOf(), result.rssi)
            }

            override fun onScanFailed(errorCode: Int) {
                onFailure(errorCode)
            }
        }
        val filter = ScanFilter.Builder()
            .setServiceData(
                ParcelUuid(request.serviceUuid),
                byteArrayOf(request.versionByte),
                byteArrayOf(request.versionMask),
            )
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(request.reportDelayMs)
            .build()
        scanner.startScan(listOf(filter), settings, callback)
        return object : AndroidBlePlatformHandle {
            @SuppressLint("MissingPermission")
            override fun stop() {
                scanner.stopScan(callback)
            }
        }
    }
}
