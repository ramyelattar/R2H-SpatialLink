package com.r2h.spatiallink.nearby

import com.r2h.spatiallink.discovery.DiscoveryFailure
import com.r2h.spatiallink.discovery.DiscoveryFailureCode
import com.r2h.spatiallink.discovery.DiscoveryPermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NearbyPresentationTest {
    @Test
    fun every_discovery_state_has_a_stable_product_status() {
        val states = listOf(
            NearbyUiState.Idle,
            NearbyUiState.Unavailable,
            NearbyUiState.AwaitingPermission(setOf(DiscoveryPermission.BLUETOOTH_SCAN)),
            NearbyUiState.BluetoothDisabled,
            NearbyUiState.Starting,
            NearbyUiState.Active(remainingDurationMs = 12_000L, peerCount = 2, strongestPeerRssiDbm = -48),
            NearbyUiState.Stopping,
            NearbyUiState.Completed,
            NearbyUiState.Error(DiscoveryFailure(DiscoveryFailureCode.SCAN_START_FAILED)),
        )

        assertEquals(
            listOf(
                "IDLE",
                "UNAVAILABLE",
                "AWAITING PERMISSION",
                "BLUETOOTH DISABLED",
                "STARTING",
                "ACTIVE",
                "STOPPING",
                "COMPLETED",
                "ERROR",
            ),
            states.map { NearbyFieldStateMapper.map(it).status.label },
        )
    }

    @Test
    fun active_presence_is_bounded_and_contains_no_identity_or_spatial_claims() {
        val presentation = NearbyFieldStateMapper.map(
            NearbyUiState.Active(
                remainingDurationMs = 12_000L,
                peerCount = 100,
                strongestPeerRssiDbm = -42,
            ),
        )

        assertEquals(64, presentation.presence.count)
        assertEquals("64 ANONYMOUS PRESENCES", presentation.presence.label)
        assertEquals("-42 dBm", presentation.presence.strengthLabel)
        val serialized = presentation.toString()
        listOf("token", "address", "name", "alias", "bond", "distance", "direction").forEach { forbidden ->
            assertFalse("presentation must not expose $forbidden", serialized.contains(forbidden, ignoreCase = true))
        }
    }

    @Test
    fun inactive_presence_has_a_quiet_anonymous_readout() {
        val presentation = NearbyFieldStateMapper.map(NearbyUiState.Idle)

        assertEquals(0, presentation.presence.count)
        assertEquals("NO ANONYMOUS PRESENCE", presentation.presence.label)
        assertEquals(null, presentation.presence.strengthLabel)
    }
}
