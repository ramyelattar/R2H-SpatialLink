package com.r2h.spatiallink.ble

import android.content.Context
import com.r2h.spatiallink.discovery.BluetoothState
import com.r2h.spatiallink.discovery.BluetoothStateReader

internal interface AndroidBluetoothStatePlatform {
    fun probe(): AndroidBluetoothStateSnapshot
}

internal data class AndroidBluetoothStateSnapshot(
    val adapterAvailable: Boolean,
    val enabled: Boolean,
)

private class TypedAndroidBluetoothStatePlatform(
    private val adapterProvider: AndroidBleAdapterProvider,
) : AndroidBluetoothStatePlatform {
    override fun probe(): AndroidBluetoothStateSnapshot {
        val adapter = adapterProvider.bluetoothAdapter()
            ?: return AndroidBluetoothStateSnapshot(false, false)
        return runCatching {
            AndroidBluetoothStateSnapshot(
                adapterAvailable = true,
                enabled = adapter.isEnabled,
            )
        }.getOrDefault(AndroidBluetoothStateSnapshot(false, false))
    }
}

class AndroidBluetoothStateReader private constructor(
    private val platform: AndroidBluetoothStatePlatform,
) : BluetoothStateReader {
    constructor(
        context: Context,
        adapterProvider: AndroidBleAdapterProvider =
            ContextAndroidBleAdapterProvider(context.applicationContext),
    ) : this(TypedAndroidBluetoothStatePlatform(adapterProvider))

    override fun read(): BluetoothState {
        val snapshot = platform.probe()
        return when {
            !snapshot.adapterAvailable -> BluetoothState.UNSUPPORTED
            !snapshot.enabled -> BluetoothState.DISABLED
            else -> BluetoothState.ENABLED
        }
    }

    companion object {
        internal fun forTest(platform: AndroidBluetoothStatePlatform): AndroidBluetoothStateReader =
            AndroidBluetoothStateReader(platform)
    }
}
