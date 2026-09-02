package com.r2h.spatiallink.ble

import android.Manifest
import android.content.pm.PackageManager
import com.r2h.spatiallink.discovery.DiscoveryOperation
import com.r2h.spatiallink.discovery.DiscoveryPermission
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidDiscoveryPermissionReaderTest {
    @Test
    fun api_29_and_30_scan_use_only_fine_location() {
        val reader = AndroidDiscoveryPermissionReader.forTest(30) { permission ->
            if (permission == Manifest.permission.ACCESS_FINE_LOCATION) {
                PackageManager.PERMISSION_DENIED
            } else {
                PackageManager.PERMISSION_GRANTED
            }
        }

        assertEquals(
            setOf(DiscoveryPermission.LEGACY_FINE_LOCATION),
            reader.missing(DiscoveryOperation.SCAN),
        )
        assertEquals(emptySet<DiscoveryPermission>(), reader.missing(DiscoveryOperation.ADVERTISE))
    }

    @Test
    fun api_31_scan_and_advertise_do_not_request_connect() {
        val reader = AndroidDiscoveryPermissionReader.forTest(34) { permission ->
            if (permission == Manifest.permission.BLUETOOTH_SCAN ||
                permission == Manifest.permission.BLUETOOTH_ADVERTISE
            ) {
                PackageManager.PERMISSION_DENIED
            } else {
                PackageManager.PERMISSION_GRANTED
            }
        }

        assertEquals(
            setOf(DiscoveryPermission.BLUETOOTH_SCAN),
            reader.missing(DiscoveryOperation.SCAN),
        )
        assertEquals(
            setOf(DiscoveryPermission.BLUETOOTH_ADVERTISE),
            reader.missing(DiscoveryOperation.ADVERTISE),
        )
        assertEquals(
            emptySet<DiscoveryPermission>(),
            reader.missing(DiscoveryOperation.ENABLE_BLUETOOTH),
        )
    }

    @Test
    fun connect_is_only_checked_for_the_explicit_enable_operation() {
        val reader = AndroidDiscoveryPermissionReader.forTest(34) { permission ->
            if (permission == Manifest.permission.BLUETOOTH_CONNECT) {
                PackageManager.PERMISSION_DENIED
            } else {
                PackageManager.PERMISSION_GRANTED
            }
        }

        assertEquals(
            setOf(DiscoveryPermission.BLUETOOTH_CONNECT),
            reader.missing(DiscoveryOperation.ENABLE_BLUETOOTH),
        )
        assertEquals(emptySet<DiscoveryPermission>(), reader.missing(DiscoveryOperation.SCAN))
        assertEquals(emptySet<DiscoveryPermission>(), reader.missing(DiscoveryOperation.ADVERTISE))
    }
}
