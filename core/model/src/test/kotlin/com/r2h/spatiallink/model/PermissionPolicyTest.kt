package com.r2h.spatiallink.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionPolicyTest {
    @Test
    fun modern_bluetooth_permissions_start_at_api_31() {
        assertFalse(PermissionPolicy.isRequired(PermissionKey.BLUETOOTH_SCAN, 30, 37))
        assertTrue(PermissionPolicy.isRequired(PermissionKey.BLUETOOTH_SCAN, 31, 37))
        assertTrue(PermissionPolicy.isRequired(PermissionKey.BLUETOOTH_ADVERTISE, 31, 37))
        assertTrue(PermissionPolicy.isRequired(PermissionKey.BLUETOOTH_CONNECT, 31, 37))
    }

    @Test
    fun nearby_wifi_permission_starts_at_api_33() {
        assertFalse(PermissionPolicy.isRequired(PermissionKey.NEARBY_WIFI_DEVICES, 32, 37))
        assertTrue(PermissionPolicy.isRequired(PermissionKey.NEARBY_WIFI_DEVICES, 33, 37))
    }

    @Test
    fun ranging_permission_starts_at_api_36() {
        assertFalse(PermissionPolicy.isRequired(PermissionKey.RANGING, 35, 37))
        assertTrue(PermissionPolicy.isRequired(PermissionKey.RANGING, 36, 37))
    }

    @Test
    fun local_network_permission_requires_os_and_target_api_37() {
        assertFalse(PermissionPolicy.isRequired(PermissionKey.ACCESS_LOCAL_NETWORK, 36, 37))
        assertFalse(PermissionPolicy.isRequired(PermissionKey.ACCESS_LOCAL_NETWORK, 37, 36))
        assertTrue(PermissionPolicy.isRequired(PermissionKey.ACCESS_LOCAL_NETWORK, 37, 37))
    }
}
