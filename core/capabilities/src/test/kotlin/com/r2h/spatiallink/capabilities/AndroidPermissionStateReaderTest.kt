package com.r2h.spatiallink.capabilities

import com.r2h.spatiallink.model.PermissionKey
import com.r2h.spatiallink.model.PermissionState
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidPermissionStateReaderTest {
    @Test
    fun required_granted_denied_and_not_declared_states_are_preserved() {
        val reader = AndroidPermissionStateReader(
            sdkInt = 37,
            targetSdk = 37,
            access = FakePermissionAccess(
                mapOf(
                    PermissionKey.BLUETOOTH_SCAN to PermissionLookupState.GRANTED,
                    PermissionKey.BLUETOOTH_ADVERTISE to PermissionLookupState.DENIED,
                    PermissionKey.BLUETOOTH_CONNECT to PermissionLookupState.NOT_DECLARED,
                    PermissionKey.NEARBY_WIFI_DEVICES to PermissionLookupState.UNKNOWN,
                    PermissionKey.ACCESS_LOCAL_NETWORK to PermissionLookupState.GRANTED,
                    PermissionKey.RANGING to PermissionLookupState.GRANTED,
                ),
            ),
        )

        val snapshot = reader.snapshot()

        assertEquals(PermissionState.GRANTED, snapshot.bluetoothScan)
        assertEquals(PermissionState.DENIED, snapshot.bluetoothAdvertise)
        assertEquals(PermissionState.NOT_DECLARED, snapshot.bluetoothConnect)
        assertEquals(PermissionState.UNKNOWN, snapshot.nearbyWifi)
        assertEquals(PermissionState.GRANTED, snapshot.localNetwork)
        assertEquals(PermissionState.GRANTED, snapshot.ranging)
    }

    @Test
    fun bluetooth_permissions_are_not_required_before_api_31() {
        val snapshot = AndroidPermissionStateReader(
            sdkInt = 29,
            targetSdk = 37,
            access = FakePermissionAccess.allGranted(),
        ).snapshot()

        assertEquals(PermissionState.NOT_REQUIRED_ON_THIS_OS, snapshot.bluetoothScan)
        assertEquals(PermissionState.NOT_REQUIRED_ON_THIS_OS, snapshot.bluetoothAdvertise)
        assertEquals(PermissionState.NOT_REQUIRED_ON_THIS_OS, snapshot.bluetoothConnect)
    }

    @Test
    fun nearby_wifi_is_required_from_api_33() {
        assertEquals(
            PermissionState.NOT_REQUIRED_ON_THIS_OS,
            AndroidPermissionStateReader(32, 37, FakePermissionAccess.allGranted()).snapshot().nearbyWifi,
        )
        assertEquals(
            PermissionState.GRANTED,
            AndroidPermissionStateReader(33, 37, FakePermissionAccess.allGranted()).snapshot().nearbyWifi,
        )
    }

    @Test
    fun ranging_is_required_from_api_36() {
        assertEquals(
            PermissionState.NOT_REQUIRED_ON_THIS_OS,
            AndroidPermissionStateReader(35, 37, FakePermissionAccess.allGranted()).snapshot().ranging,
        )
        assertEquals(
            PermissionState.GRANTED,
            AndroidPermissionStateReader(36, 37, FakePermissionAccess.allGranted()).snapshot().ranging,
        )
    }

    @Test
    fun local_network_requires_api_37_and_target_37() {
        assertEquals(
            PermissionState.NOT_REQUIRED_ON_THIS_OS,
            AndroidPermissionStateReader(37, 36, FakePermissionAccess.allGranted()).snapshot().localNetwork,
        )
        assertEquals(
            PermissionState.NOT_REQUIRED_ON_THIS_OS,
            AndroidPermissionStateReader(36, 37, FakePermissionAccess.allGranted()).snapshot().localNetwork,
        )
        assertEquals(
            PermissionState.GRANTED,
            AndroidPermissionStateReader(37, 37, FakePermissionAccess.allGranted()).snapshot().localNetwork,
        )
    }

    private class FakePermissionAccess(
        private val states: Map<PermissionKey, PermissionLookupState>,
    ) : PermissionPlatformAccess {
        override fun lookup(permissionName: String): PermissionLookupState =
            states[permissionKeyFor(permissionName)] ?: PermissionLookupState.UNKNOWN

        private fun permissionKeyFor(permissionName: String): PermissionKey = when (permissionName) {
            "android.permission.BLUETOOTH_SCAN" -> PermissionKey.BLUETOOTH_SCAN
            "android.permission.BLUETOOTH_ADVERTISE" -> PermissionKey.BLUETOOTH_ADVERTISE
            "android.permission.BLUETOOTH_CONNECT" -> PermissionKey.BLUETOOTH_CONNECT
            "android.permission.NEARBY_WIFI_DEVICES" -> PermissionKey.NEARBY_WIFI_DEVICES
            "android.permission.ACCESS_LOCAL_NETWORK" -> PermissionKey.ACCESS_LOCAL_NETWORK
            "android.permission.RANGING" -> PermissionKey.RANGING
            else -> error("Unexpected permission: $permissionName")
        }

        companion object {
            fun allGranted() = FakePermissionAccess(
                PermissionKey.values().associateWith { PermissionLookupState.GRANTED },
            )
        }
    }
}
