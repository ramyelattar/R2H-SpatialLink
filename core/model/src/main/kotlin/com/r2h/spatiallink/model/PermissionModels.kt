package com.r2h.spatiallink.model

enum class PermissionKey {
    BLUETOOTH_SCAN,
    BLUETOOTH_ADVERTISE,
    BLUETOOTH_CONNECT,
    NEARBY_WIFI_DEVICES,
    ACCESS_LOCAL_NETWORK,
    RANGING,
}

enum class PermissionState {
    GRANTED,
    DENIED,
    NOT_REQUIRED_ON_THIS_OS,
    NOT_DECLARED,
    UNKNOWN,
}

data class PermissionSnapshot(
    val bluetoothScan: PermissionState,
    val bluetoothAdvertise: PermissionState,
    val bluetoothConnect: PermissionState,
    val nearbyWifi: PermissionState,
    val localNetwork: PermissionState,
    val ranging: PermissionState,
) {
    operator fun get(key: PermissionKey): PermissionState = when (key) {
        PermissionKey.BLUETOOTH_SCAN -> bluetoothScan
        PermissionKey.BLUETOOTH_ADVERTISE -> bluetoothAdvertise
        PermissionKey.BLUETOOTH_CONNECT -> bluetoothConnect
        PermissionKey.NEARBY_WIFI_DEVICES -> nearbyWifi
        PermissionKey.ACCESS_LOCAL_NETWORK -> localNetwork
        PermissionKey.RANGING -> ranging
    }

    fun asMap(): Map<PermissionKey, PermissionState> = PermissionKey.values().associateWith(::get)
}
