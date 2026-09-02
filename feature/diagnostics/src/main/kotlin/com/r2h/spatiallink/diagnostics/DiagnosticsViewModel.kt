package com.r2h.spatiallink.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.r2h.spatiallink.common.DispatcherProvider
import com.r2h.spatiallink.model.DeviceCapabilities
import com.r2h.spatiallink.model.DeviceCapabilityDetector
import com.r2h.spatiallink.model.DeviceIdentityRepository
import com.r2h.spatiallink.model.IdentityRepositoryResult
import com.r2h.spatiallink.model.PermissionSnapshot
import com.r2h.spatiallink.model.PermissionStateReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class DiagnosticsViewModel(
    private val capabilityDetector: DeviceCapabilityDetector,
    private val permissionStateReader: PermissionStateReader,
    private val identityRepository: DeviceIdentityRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiagnosticsUiStateReducer.loading())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = DiagnosticsUiStateReducer.loading()
        viewModelScope.launch(dispatchers.default) {
            supervisorScope {
                val capabilities = async { inspectCapabilities() }
                val permissions = async { inspectPermissions() }
                val identity = async { inspectIdentity() }
                publish(capabilities.await(), permissions.await(), identity.await())
            }
        }
    }

    private suspend fun inspectCapabilities(): InspectionResult<DeviceCapabilities> = try {
        InspectionResult.Success(capabilityDetector.detect())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        InspectionResult.Failed
    }

    private suspend fun inspectPermissions(): InspectionResult<PermissionSnapshot> = try {
        InspectionResult.Success(permissionStateReader.snapshot())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        InspectionResult.Failed
    }

    private suspend fun inspectIdentity(): InspectionResult<IdentityRepositoryResult> = try {
        InspectionResult.Success(identityRepository.getOrCreate())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        InspectionResult.Failed
    }

    private fun publish(
        capabilities: InspectionResult<DeviceCapabilities>,
        permissions: InspectionResult<PermissionSnapshot>,
        identity: InspectionResult<IdentityRepositoryResult>,
    ) {
        val capabilitiesValue = (capabilities as? InspectionResult.Success)?.value
        val permissionsValue = (permissions as? InspectionResult.Success)?.value
        val identityValue = (identity as? InspectionResult.Success)?.value
            ?: IdentityRepositoryResult.NotCreated
        val error = when {
            capabilities is InspectionResult.Failed -> DiagnosticsError.CapabilityInspectionFailed
            permissions is InspectionResult.Failed -> DiagnosticsError.PermissionInspectionFailed
            identity is InspectionResult.Failed -> DiagnosticsError.IdentityInspectionFailed
            else -> null
        }

        _uiState.value = if (
            capabilitiesValue != null &&
                permissionsValue != null &&
                error == null
        ) {
            DiagnosticsUiStateReducer.success(
                capabilities = capabilitiesValue,
                permissions = permissionsValue,
                identity = identityValue,
            )
        } else {
            DiagnosticsUiStateReducer.failure(
                capabilities = capabilitiesValue,
                permissions = permissionsValue,
                error = error ?: DiagnosticsError.CapabilityInspectionFailed,
                identity = identityValue,
            )
        }
    }

    private sealed interface InspectionResult<out T> {
        data class Success<T>(val value: T) : InspectionResult<T>
        data object Failed : InspectionResult<Nothing>
    }
}
