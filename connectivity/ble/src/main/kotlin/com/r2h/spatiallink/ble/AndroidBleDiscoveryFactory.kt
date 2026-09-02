package com.r2h.spatiallink.ble

import android.os.Build
import android.os.SystemClock
import android.content.Context
import com.r2h.spatiallink.discovery.BleAdvertiser
import com.r2h.spatiallink.discovery.BleScanner
import com.r2h.spatiallink.discovery.BluetoothStateReader
import com.r2h.spatiallink.discovery.DiscoveryPermissionReader
import com.r2h.spatiallink.discovery.MonotonicClock

class AndroidMonotonicClock : MonotonicClock {
    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
}

class AndroidBleDiscoveryFactory(
    context: Context,
    private val clock: MonotonicClock = AndroidMonotonicClock(),
    sdkInt: Int = Build.VERSION.SDK_INT,
) {
    private val applicationContext = context.applicationContext
    private val adapterProvider = ContextAndroidBleAdapterProvider(applicationContext)

    val bluetoothStateReader: BluetoothStateReader =
        AndroidBluetoothStateReader(applicationContext, adapterProvider)
    val permissionReader: DiscoveryPermissionReader =
        AndroidDiscoveryPermissionReader(applicationContext, sdkInt)

    fun createScanner(): BleScanner =
        AndroidBleScanner(applicationContext, adapterProvider, clock)

    fun createAdvertiser(): BleAdvertiser =
        AndroidBleAdvertiser(applicationContext, adapterProvider)
}
