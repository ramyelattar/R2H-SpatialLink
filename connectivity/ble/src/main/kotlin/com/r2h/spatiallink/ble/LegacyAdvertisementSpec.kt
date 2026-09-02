package com.r2h.spatiallink.ble

import com.r2h.spatiallink.discovery.DiscoveryProtocol
import java.util.UUID

data class LegacyAdvertisementSpec(
    val serviceUuid: UUID,
    val serviceData: ByteArray,
    val includeDeviceName: Boolean,
    val includeTxPowerLevel: Boolean,
    val manufacturerDataEntries: Int,
    val serviceUuidEntries: Int,
    val scanResponsePresent: Boolean,
) {
    val ownedServiceDataBytes: Int
        get() = BleAndroidConstants.ownedServiceDataOverheadBytes + serviceData.size

    fun fitsLegacyBudget(): Boolean =
        ownedServiceDataBytes <= BleAndroidConstants.legacyAdvertisingLimitBytes

    companion object {
        fun forPayload(payload: ByteArray): LegacyAdvertisementSpec {
            require(payload.size == DiscoveryProtocol.PAYLOAD_SIZE) {
                "SpatialLink v1 service data must be exactly nine bytes"
            }
            require(payload[0] == DiscoveryProtocol.V1_VERSION) {
                "SpatialLink service data must use protocol version 1"
            }
            return LegacyAdvertisementSpec(
                serviceUuid = BleAndroidConstants.serviceUuid,
                serviceData = payload.copyOf(),
                includeDeviceName = false,
                includeTxPowerLevel = false,
                manufacturerDataEntries = 0,
                serviceUuidEntries = 0,
                scanResponsePresent = false,
            )
        }
    }
}
