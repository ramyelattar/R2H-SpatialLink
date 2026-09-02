package com.r2h.spatiallink.ble

import android.annotation.SuppressLint
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.ParcelUuid
import com.r2h.spatiallink.discovery.BleAdvertiser
import com.r2h.spatiallink.discovery.BleOperationHandle
import com.r2h.spatiallink.discovery.DiscoveryFailure
import com.r2h.spatiallink.discovery.DiscoveryFailureCode
import com.r2h.spatiallink.discovery.DiscoveryProtocol

class AndroidBleAdvertiser private constructor(
    private val platform: AndroidBleAdvertiserPlatform,
) : BleAdvertiser {
    constructor(
        context: android.content.Context,
        adapterProvider: AndroidBleAdapterProvider,
    ) : this(TypedAndroidBleAdvertiserPlatform(adapterProvider))

    override suspend fun start(
        payload: ByteArray,
        onFailure: (DiscoveryFailure) -> Unit,
    ): BleOperationHandle {
        val spec = runCatching { LegacyAdvertisementSpec.forPayload(payload) }
            .getOrElse {
                val failure = DiscoveryFailure(
                    code = DiscoveryFailureCode.DATA_TOO_LARGE,
                    detail = "SpatialLink v1 payload must be exactly nine bytes",
                )
                onFailure(failure)
                return NoOpBleOperationHandle
            }
        if (!spec.fitsLegacyBudget()) {
            onFailure(
                DiscoveryFailure(
                    code = DiscoveryFailureCode.DATA_TOO_LARGE,
                    detail = "SpatialLink service-data structure exceeds the legacy budget",
                ),
            )
            return NoOpBleOperationHandle
        }

        val handle = IdempotentBleOperationHandle()
        val platformHandle = try {
            platform.start(spec) { errorCode ->
                handle.stopBlocking()
                onFailure(
                    DiscoveryFailure(
                        code = mapAdvertiseFailure(errorCode),
                        detail = "Bluetooth advertise callback failure: $errorCode",
                    ),
                )
            }
        } catch (throwable: Throwable) {
            onFailure(
                DiscoveryFailure(
                    code = DiscoveryFailureCode.ADVERTISE_START_FAILED,
                    detail = throwable.javaClass.simpleName,
                ),
            )
            return NoOpBleOperationHandle
        }
        return handle.also { it.attach(platformHandle) }
    }

    companion object {
        internal fun forTest(platform: AndroidBleAdvertiserPlatform): AndroidBleAdvertiser =
            AndroidBleAdvertiser(platform)
    }
}

private fun mapAdvertiseFailure(errorCode: Int): DiscoveryFailureCode =
    if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE) {
        DiscoveryFailureCode.DATA_TOO_LARGE
    } else {
        DiscoveryFailureCode.ADVERTISE_START_FAILED
    }

private class TypedAndroidBleAdvertiserPlatform(
    private val adapterProvider: AndroidBleAdapterProvider,
) : AndroidBleAdvertiserPlatform {
    override fun start(
        spec: LegacyAdvertisementSpec,
        onFailure: (Int) -> Unit,
    ): AndroidBlePlatformHandle {
        val advertiser: BluetoothLeAdvertiser =
            adapterProvider.bluetoothAdapter()?.bluetoothLeAdvertiser
                ?: throw IllegalStateException("Bluetooth LE advertiser unavailable")
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(false)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceData(ParcelUuid(spec.serviceUuid), spec.serviceData.copyOf())
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()
        val callback = object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                onFailure(errorCode)
            }
        }
        advertiser.startAdvertising(settings, data, callback)
        return object : AndroidBlePlatformHandle {
            @SuppressLint("MissingPermission")
            override fun stop() {
                advertiser.stopAdvertising(callback)
            }
        }
    }
}
