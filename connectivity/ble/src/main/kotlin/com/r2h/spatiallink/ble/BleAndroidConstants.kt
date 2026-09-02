package com.r2h.spatiallink.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.content.Context
import android.os.ParcelUuid
import com.r2h.spatiallink.discovery.DiscoveryProtocol
import com.r2h.spatiallink.discovery.BleOperationHandle
import java.util.UUID

object BleAndroidConstants {
    val serviceUuid: UUID = UUID.fromString(DiscoveryProtocol.SERVICE_UUID)
    val serviceDataParcelUuid: ParcelUuid = ParcelUuid(serviceUuid)
    const val legacyAdvertisingLimitBytes: Int = 31
    const val ownedServiceDataOverheadBytes: Int = 18
}

interface AndroidBleAdapterProvider {
    fun bluetoothAdapter(): BluetoothAdapter?
}

class ContextAndroidBleAdapterProvider(context: Context) : AndroidBleAdapterProvider {
    private val applicationContext = context.applicationContext

    override fun bluetoothAdapter(): BluetoothAdapter? =
        applicationContext.getSystemService(BluetoothManager::class.java)?.adapter
}

internal enum class BleScanMode {
    LOW_LATENCY,
}

internal data class BleScanRequest(
    val serviceUuid: UUID,
    val versionByte: Byte,
    val versionMask: Byte,
    val scanMode: BleScanMode,
    val reportDelayMs: Long,
    val hasOnlyServiceDataFilter: Boolean,
    val hasAddressFilter: Boolean,
    val hasDeviceNameFilter: Boolean,
    val hasManufacturerFilter: Boolean,
)

internal interface AndroidBlePlatformHandle {
    fun stop()
}

internal interface AndroidBleScannerPlatform {
    fun start(
        request: BleScanRequest,
        onResult: (payload: ByteArray, rssiDbm: Int) -> Unit,
        onFailure: (errorCode: Int) -> Unit,
    ): AndroidBlePlatformHandle
}

internal interface AndroidBleAdvertiserPlatform {
    fun start(
        spec: LegacyAdvertisementSpec,
        onFailure: (errorCode: Int) -> Unit,
    ): AndroidBlePlatformHandle
}

internal object AdvertiseFailureCode {
    const val DATA_TOO_LARGE: Int = AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE
}

internal class IdempotentBleOperationHandle : BleOperationHandle {
    private val lock = Any()
    private var stopped = false
    private var platformStopped = false
    private var delegate: AndroidBlePlatformHandle? = null

    override suspend fun stop() {
        stopBlocking()
    }

    fun stopBlocking() {
        synchronized(lock) {
            if (stopped) return
            stopped = true
            delegate?.let { platformHandle ->
                platformStopped = true
                platformHandle.stop()
            }
        }
    }

    fun attach(platformHandle: AndroidBlePlatformHandle) {
        synchronized(lock) {
            if (!stopped) {
                delegate = platformHandle
            } else if (!platformStopped) {
                platformStopped = true
                platformHandle.stop()
            }
        }
    }
}

internal object NoOpBleOperationHandle : BleOperationHandle {
    override suspend fun stop() = Unit
}
