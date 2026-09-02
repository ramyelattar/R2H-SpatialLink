package com.r2h.spatiallink.capabilities

import com.r2h.spatiallink.model.PermissionKey
import com.r2h.spatiallink.model.PermissionPolicy
import com.r2h.spatiallink.model.PermissionSnapshot
import com.r2h.spatiallink.model.PermissionState
import com.r2h.spatiallink.model.PermissionStateReader

class AndroidPermissionStateReader(
    private val sdkInt: Int,
    private val targetSdk: Int,
    private val access: PermissionPlatformAccess,
) : PermissionStateReader {
    override fun snapshot(): PermissionSnapshot = PermissionSnapshot(
        bluetoothScan = read(PermissionKey.BLUETOOTH_SCAN),
        bluetoothAdvertise = read(PermissionKey.BLUETOOTH_ADVERTISE),
        bluetoothConnect = read(PermissionKey.BLUETOOTH_CONNECT),
        nearbyWifi = read(PermissionKey.NEARBY_WIFI_DEVICES),
        localNetwork = read(PermissionKey.ACCESS_LOCAL_NETWORK),
        ranging = read(PermissionKey.RANGING),
    )

    private fun read(key: PermissionKey): PermissionState {
        if (!PermissionPolicy.isRequired(key, sdkInt, targetSdk)) {
            return PermissionState.NOT_REQUIRED_ON_THIS_OS
        }

        return when (access.lookup(permissionName(key))) {
            PermissionLookupState.GRANTED -> PermissionState.GRANTED
            PermissionLookupState.DENIED -> PermissionState.DENIED
            PermissionLookupState.NOT_DECLARED -> PermissionState.NOT_DECLARED
            PermissionLookupState.UNKNOWN -> PermissionState.UNKNOWN
        }
    }
}
