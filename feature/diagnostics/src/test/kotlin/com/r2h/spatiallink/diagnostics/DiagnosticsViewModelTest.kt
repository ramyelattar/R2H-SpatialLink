package com.r2h.spatiallink.diagnostics

import com.r2h.spatiallink.testing.CapabilityFixtures
import com.r2h.spatiallink.testing.FakeDeviceCapabilityDetector
import com.r2h.spatiallink.testing.FakeDeviceIdentityRepository
import com.r2h.spatiallink.testing.FakePermissionStateReader
import com.r2h.spatiallink.testing.TestDispatcherProvider
import com.r2h.spatiallink.model.FoundationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticsViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun state_flows_from_loading_to_success() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val viewModel = DiagnosticsViewModel(
            capabilityDetector = FakeDeviceCapabilityDetector(CapabilityFixtures.fullyKnown()),
            permissionStateReader = FakePermissionStateReader(CapabilityFixtures.allPermissions()),
            identityRepository = FakeDeviceIdentityRepository(CapabilityFixtures.availableIdentity()),
            dispatchers = TestDispatcherProvider(dispatcher),
        )

        assertEquals(DiagnosticsUiStateReducer.loading(), viewModel.uiState.value)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(FoundationStatus.READY, viewModel.uiState.value.foundationStatus)
        assertNotNull(viewModel.uiState.value.capabilities)
        assertNotNull(viewModel.uiState.value.permissions)
    }

    @Test
    fun detector_failure_keeps_permission_result_and_uses_typed_error() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val viewModel = DiagnosticsViewModel(
            capabilityDetector = FakeDeviceCapabilityDetector(
                failure = IllegalStateException("not rendered"),
            ),
            permissionStateReader = FakePermissionStateReader(CapabilityFixtures.allPermissions()),
            identityRepository = FakeDeviceIdentityRepository(CapabilityFixtures.availableIdentity()),
            dispatchers = TestDispatcherProvider(dispatcher),
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(DiagnosticsError.CapabilityInspectionFailed, viewModel.uiState.value.error)
        assertNotNull(viewModel.uiState.value.permissions)
        assertEquals(FoundationStatus.ERROR, viewModel.uiState.value.foundationStatus)
    }

    @Test
    fun permission_failure_keeps_capability_result_and_derives_partial() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val viewModel = DiagnosticsViewModel(
            capabilityDetector = FakeDeviceCapabilityDetector(CapabilityFixtures.fullyKnown()),
            permissionStateReader = FakePermissionStateReader(
                failure = IllegalStateException("not rendered"),
            ),
            identityRepository = FakeDeviceIdentityRepository(CapabilityFixtures.availableIdentity()),
            dispatchers = TestDispatcherProvider(dispatcher),
        )

        advanceUntilIdle()

        assertEquals(DiagnosticsError.PermissionInspectionFailed, viewModel.uiState.value.error)
        assertNotNull(viewModel.uiState.value.capabilities)
        assertEquals(FoundationStatus.PARTIAL, viewModel.uiState.value.foundationStatus)
    }

    @Test
    fun identity_failure_preserves_capabilities_and_permissions() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val viewModel = DiagnosticsViewModel(
            capabilityDetector = FakeDeviceCapabilityDetector(CapabilityFixtures.fullyKnown()),
            permissionStateReader = FakePermissionStateReader(CapabilityFixtures.allPermissions()),
            identityRepository = FakeDeviceIdentityRepository(
                throwable = IllegalStateException("identity boundary failed"),
            ),
            dispatchers = TestDispatcherProvider(dispatcher),
        )

        advanceUntilIdle()

        assertEquals(DiagnosticsError.IdentityInspectionFailed, viewModel.uiState.value.error)
        assertNotNull(viewModel.uiState.value.capabilities)
        assertNotNull(viewModel.uiState.value.permissions)
        assertEquals(IdentityStatus.ERROR, viewModel.uiState.value.identity.status)
    }
}
