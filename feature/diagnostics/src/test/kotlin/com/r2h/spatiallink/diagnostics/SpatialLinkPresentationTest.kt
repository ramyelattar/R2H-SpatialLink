package com.r2h.spatiallink.diagnostics

import com.r2h.spatiallink.designsystem.SpatialLinkTone
import com.r2h.spatiallink.model.PermissionState
import com.r2h.spatiallink.testing.CapabilityFixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class SpatialLinkPresentationTest {
    @Test
    fun ready_state_projects_into_trusted_overview() {
        val state = DiagnosticsUiStateReducer.success(
            capabilities = CapabilityFixtures.fullyKnown(),
            permissions = CapabilityFixtures.allPermissions(),
            identity = CapabilityFixtures.availableIdentity(),
        )

        val model = state.toOverviewModel()

        assertEquals("READY", model.foundation.label)
        assertEquals(SpatialLinkTone.HEALTHY, model.foundation.tone)
        assertEquals("TRUSTED", model.identity.label)
        assertEquals("READY", model.localLink.label)
        assertEquals("ECDSA P-256 / SHA-256", model.identityAlgorithm)
        assertEquals(SpatialLinkTone.ACTIVE, model.fieldState.tone)
    }

    @Test
    fun unavailable_hardware_remains_unavailable_not_error() {
        val state = DiagnosticsUiStateReducer.success(
            capabilities = CapabilityFixtures.fullyUnavailable(),
            permissions = CapabilityFixtures.allPermissions(),
        )

        val tiles = state.toSystemClusters().flatMap { it.tiles }

        assertEquals("UNAVAILABLE", tiles.first { it.title == "UWB" }.status.label)
        assertEquals(SpatialLinkTone.QUIET, tiles.first { it.title == "UWB" }.status.tone)
    }

    @Test
    fun not_required_permission_is_human_readable_and_not_error() {
        val state = DiagnosticsUiStateReducer.success(
            capabilities = CapabilityFixtures.fullyKnown(sdkInt = 34),
            permissions = CapabilityFixtures.allPermissions(PermissionState.NOT_REQUIRED_ON_THIS_OS),
        )

        val permissionTiles = state.toSystemClusters()
            .first { it.title == "Runtime permissions" }
            .tiles

        assertEquals(
            "NOT REQUIRED",
            permissionTiles.first { it.title == "Local network" }.status.label,
        )
        assertEquals(
            SpatialLinkTone.QUIET,
            permissionTiles.first { it.title == "Local network" }.status.tone,
        )
    }

    @Test
    fun diagnostics_projection_remains_separate_from_anonymous_nearby_presence() {
        val state = DiagnosticsUiStateReducer.success(
            capabilities = CapabilityFixtures.fullyKnown(),
            permissions = CapabilityFixtures.allPermissions(),
            identity = CapabilityFixtures.availableIdentity(),
        )

        val overview = state.toOverviewModel()
        val systemText = state.toSystemClusters().toString()

        assertEquals("READY", overview.foundation.label)
        assertEquals("READY", overview.localLink.label)
        org.junit.Assert.assertFalse(systemText.contains("anonymous", ignoreCase = true))
        org.junit.Assert.assertFalse(systemText.contains("presence", ignoreCase = true))
    }
}
