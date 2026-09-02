package com.r2h.spatiallink.diagnostics

import com.r2h.spatiallink.model.FoundationStatus
import com.r2h.spatiallink.model.IdentityFailure
import com.r2h.spatiallink.model.IdentityFailureCode
import com.r2h.spatiallink.model.IdentityRepositoryResult
import com.r2h.spatiallink.testing.CapabilityFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsUiStateReducerTest {
    @Test
    fun loading_state_has_no_partial_data() {
        val state = DiagnosticsUiStateReducer.loading()

        assertTrue(state.isLoading)
        assertNull(state.capabilities)
        assertNull(state.permissions)
        assertNull(state.error)
        assertEquals(FoundationStatus.ERROR, state.foundationStatus)
        assertEquals(IdentityStatus.LOADING, state.identity.status)
    }

    @Test
    fun successful_known_snapshot_is_ready() {
        val state = DiagnosticsUiStateReducer.success(
            capabilities = CapabilityFixtures.fullyKnown(),
            permissions = CapabilityFixtures.allPermissions(),
        )

        assertFalse(state.isLoading)
        assertEquals(FoundationStatus.READY, state.foundationStatus)
        assertNull(state.error)
        assertEquals(IdentityStatus.NOT_CREATED, state.identity.status)
    }

    @Test
    fun successful_partial_snapshot_is_partial() {
        val state = DiagnosticsUiStateReducer.success(
            capabilities = CapabilityFixtures.partial(),
            permissions = CapabilityFixtures.allPermissions(),
        )

        assertEquals(FoundationStatus.PARTIAL, state.foundationStatus)
    }

    @Test
    fun capability_failure_without_snapshot_is_error_and_is_typed() {
        val state = DiagnosticsUiStateReducer.failure(
            capabilities = null,
            permissions = CapabilityFixtures.allPermissions(),
            error = DiagnosticsError.CapabilityInspectionFailed,
        )

        assertEquals(FoundationStatus.ERROR, state.foundationStatus)
        assertEquals(DiagnosticsError.CapabilityInspectionFailed, state.error)
        assertNull(state.capabilities)
    }

    @Test
    fun permission_failure_preserves_capabilities_as_partial() {
        val capabilities = CapabilityFixtures.fullyKnown()
        val state = DiagnosticsUiStateReducer.failure(
            capabilities = capabilities,
            permissions = null,
            error = DiagnosticsError.PermissionInspectionFailed,
        )

        assertEquals(capabilities, state.capabilities)
        assertEquals(FoundationStatus.PARTIAL, state.foundationStatus)
        assertEquals(DiagnosticsError.PermissionInspectionFailed, state.error)
    }

    @Test
    fun available_identity_is_ready_with_safe_display_metadata() {
        val state = DiagnosticsUiStateReducer.success(
            capabilities = CapabilityFixtures.fullyKnown(),
            permissions = CapabilityFixtures.allPermissions(),
            identity = CapabilityFixtures.availableIdentity(),
        )

        assertEquals(IdentityStatus.READY, state.identity.status)
        assertEquals("0390-58C6-F2C0", state.identity.shortId)
        assertEquals("ECDSA_P256_SHA256", state.identity.algorithm)
        assertEquals("SOFTWARE", state.identity.securityLevel)
        assertEquals(com.r2h.spatiallink.model.IdentitySelfTestStatus.PASSED, state.identity.selfTest)
    }

    @Test
    fun recovery_required_identity_is_not_presented_as_ready() {
        val state = DiagnosticsUiStateReducer.failure(
            capabilities = CapabilityFixtures.fullyKnown(),
            permissions = CapabilityFixtures.allPermissions(),
            identity = IdentityRepositoryResult.Failed(
                IdentityFailure(IdentityFailureCode.RECOVERY_REQUIRED),
            ),
            error = DiagnosticsError.IdentityInspectionFailed,
        )

        assertEquals(IdentityStatus.RECOVERY_REQUIRED, state.identity.status)
        assertEquals(IdentityFailureCode.RECOVERY_REQUIRED, state.identity.failureCode)
    }
}
