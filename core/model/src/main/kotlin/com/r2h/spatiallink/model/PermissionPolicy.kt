package com.r2h.spatiallink.model

object PermissionPolicy {
    fun isRequired(
        key: PermissionKey,
        sdkInt: Int,
        targetSdk: Int,
    ): Boolean = when (key) {
        PermissionKey.BLUETOOTH_SCAN,
        PermissionKey.BLUETOOTH_ADVERTISE,
        PermissionKey.BLUETOOTH_CONNECT,
        -> sdkInt >= 31

        PermissionKey.NEARBY_WIFI_DEVICES -> sdkInt >= 33
        PermissionKey.RANGING -> sdkInt >= 36
        PermissionKey.ACCESS_LOCAL_NETWORK -> sdkInt >= 37 && targetSdk >= 37
    }
}
