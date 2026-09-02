package com.r2h.spatiallink.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryPermissionPolicyTest {
    @Test
    fun api_29_scan_uses_fine_location_and_advertise_has_no_modern_runtime_permission() {
        assertEquals(
            setOf(DiscoveryPermission.LEGACY_FINE_LOCATION),
            DiscoveryPermissionPolicy.required(29, DiscoveryOperation.SCAN),
        )
        assertTrue(DiscoveryPermissionPolicy.required(29, DiscoveryOperation.ADVERTISE).isEmpty())
        assertTrue(DiscoveryPermissionPolicy.requiredForDiscoverySession(29).contains(DiscoveryPermission.LEGACY_FINE_LOCATION))
    }

    @Test
    fun api_30_matches_the_legacy_scan_boundary() {
        assertEquals(
            setOf(DiscoveryPermission.LEGACY_FINE_LOCATION),
            DiscoveryPermissionPolicy.required(30, DiscoveryOperation.SCAN),
        )
        assertTrue(DiscoveryPermissionPolicy.required(30, DiscoveryOperation.ADVERTISE).isEmpty())
        assertEquals(
            setOf(DiscoveryPermission.LEGACY_FINE_LOCATION),
            DiscoveryPermissionPolicy.requiredForDiscoverySession(30),
        )
    }

    @Test
    fun api_31_already_enabled_discovery_requests_scan_and_advertise_only() {
        assertEquals(
            setOf(
                DiscoveryPermission.BLUETOOTH_SCAN,
                DiscoveryPermission.BLUETOOTH_ADVERTISE,
            ),
            DiscoveryPermissionPolicy.requiredForDiscoverySession(31),
        )
    }

    @Test
    fun api_31_bluetooth_enable_consent_is_the_connect_boundary() {
        assertEquals(
            setOf(DiscoveryPermission.BLUETOOTH_CONNECT),
            DiscoveryPermissionPolicy.required(31, DiscoveryOperation.ENABLE_BLUETOOTH),
        )
    }

    @Test
    fun api_31_plus_discovery_never_requires_connect() {
        listOf(31, 33, 34, 36, 37).forEach { sdkInt ->
            val requiredForSession = DiscoveryPermissionPolicy.requiredForDiscoverySession(sdkInt)

            assertEquals(
                setOf(
                    DiscoveryPermission.BLUETOOTH_SCAN,
                    DiscoveryPermission.BLUETOOTH_ADVERTISE,
                ),
                requiredForSession,
            )
            assertFalse(requiredForSession.contains(DiscoveryPermission.BLUETOOTH_CONNECT))
        }
    }

    @Test
    fun only_the_four_discovery_permissions_exist_in_the_public_policy_model() {
        assertEquals(
            setOf(
                "LEGACY_FINE_LOCATION",
                "BLUETOOTH_SCAN",
                "BLUETOOTH_ADVERTISE",
                "BLUETOOTH_CONNECT",
            ),
            DiscoveryPermission.entries.map { it.name }.toSet(),
        )
        assertFalse(DiscoveryPermission.entries.any { it.name == "NEARBY_WIFI_DEVICES" })
        assertFalse(DiscoveryPermission.entries.any { it.name == "ACCESS_LOCAL_NETWORK" })
        assertFalse(DiscoveryPermission.entries.any { it.name == "RANGING" })
    }

    @Test
    fun no_operation_returns_permissions_outside_the_expected_scan_advertise_and_connect_sets() {
        val expectedBySdk = mapOf(
            29 to mapOf(
                DiscoveryOperation.SCAN to setOf(DiscoveryPermission.LEGACY_FINE_LOCATION),
                DiscoveryOperation.ADVERTISE to emptySet(),
                DiscoveryOperation.ENABLE_BLUETOOTH to emptySet(),
            ),
            30 to mapOf(
                DiscoveryOperation.SCAN to setOf(DiscoveryPermission.LEGACY_FINE_LOCATION),
                DiscoveryOperation.ADVERTISE to emptySet(),
                DiscoveryOperation.ENABLE_BLUETOOTH to emptySet(),
            ),
            31 to mapOf(
                DiscoveryOperation.SCAN to setOf(DiscoveryPermission.BLUETOOTH_SCAN),
                DiscoveryOperation.ADVERTISE to setOf(DiscoveryPermission.BLUETOOTH_ADVERTISE),
                DiscoveryOperation.ENABLE_BLUETOOTH to setOf(DiscoveryPermission.BLUETOOTH_CONNECT),
            ),
            33 to mapOf(
                DiscoveryOperation.SCAN to setOf(DiscoveryPermission.BLUETOOTH_SCAN),
                DiscoveryOperation.ADVERTISE to setOf(DiscoveryPermission.BLUETOOTH_ADVERTISE),
                DiscoveryOperation.ENABLE_BLUETOOTH to setOf(DiscoveryPermission.BLUETOOTH_CONNECT),
            ),
            34 to mapOf(
                DiscoveryOperation.SCAN to setOf(DiscoveryPermission.BLUETOOTH_SCAN),
                DiscoveryOperation.ADVERTISE to setOf(DiscoveryPermission.BLUETOOTH_ADVERTISE),
                DiscoveryOperation.ENABLE_BLUETOOTH to setOf(DiscoveryPermission.BLUETOOTH_CONNECT),
            ),
            36 to mapOf(
                DiscoveryOperation.SCAN to setOf(DiscoveryPermission.BLUETOOTH_SCAN),
                DiscoveryOperation.ADVERTISE to setOf(DiscoveryPermission.BLUETOOTH_ADVERTISE),
                DiscoveryOperation.ENABLE_BLUETOOTH to setOf(DiscoveryPermission.BLUETOOTH_CONNECT),
            ),
            37 to mapOf(
                DiscoveryOperation.SCAN to setOf(DiscoveryPermission.BLUETOOTH_SCAN),
                DiscoveryOperation.ADVERTISE to setOf(DiscoveryPermission.BLUETOOTH_ADVERTISE),
                DiscoveryOperation.ENABLE_BLUETOOTH to setOf(DiscoveryPermission.BLUETOOTH_CONNECT),
            ),
        )

        expectedBySdk.forEach { (sdkInt, operations) ->
            operations.forEach { (operation, expected) ->
                assertEquals(expected, DiscoveryPermissionPolicy.required(sdkInt, operation))
            }
        }
    }
}
