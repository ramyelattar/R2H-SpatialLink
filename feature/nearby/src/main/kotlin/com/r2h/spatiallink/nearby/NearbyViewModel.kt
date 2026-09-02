package com.r2h.spatiallink.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.r2h.spatiallink.common.DispatcherProvider
import com.r2h.spatiallink.discovery.DiscoveryController
import com.r2h.spatiallink.discovery.DiscoveryOperation
import com.r2h.spatiallink.discovery.DiscoveryPermission
import com.r2h.spatiallink.discovery.DiscoveryPermissionReader
import com.r2h.spatiallink.discovery.DiscoveryState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NearbyViewModel(
    private val controller: DiscoveryController,
    private val permissionReader: DiscoveryPermissionReader,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<NearbyUiState>(NearbyUiState.Idle)
    val uiState: StateFlow<NearbyUiState> = mutableUiState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<NearbyEffect>(
        extraBufferCapacity = EFFECT_BUFFER_CAPACITY,
    )
    val effects: Flow<NearbyEffect> = mutableEffects.asSharedFlow()

    private var fieldOpenRequested = false
    private var bluetoothEnableRequestInFlight = false

    init {
        viewModelScope.launch(dispatchers.default) {
            controller.state.collect { state ->
                mutableUiState.value = state.toUiState()
                if (state is DiscoveryState.BluetoothDisabled &&
                    fieldOpenRequested &&
                    !bluetoothEnableRequestInFlight
                ) {
                    requestBluetoothEnable()
                }
            }
        }
    }

    fun openField() {
        fieldOpenRequested = true
        bluetoothEnableRequestInFlight = false
        viewModelScope.launch(dispatchers.default) {
            beginOpenField()
        }
    }

    fun stopField() {
        viewModelScope.launch(dispatchers.default) {
            controller.close()
        }
    }

    fun requestClose() {
        viewModelScope.launch(dispatchers.default) {
            controller.close()
            mutableEffects.emit(NearbyEffect.LeaveCompleted)
        }
    }

    fun onForegroundLost() {
        fieldOpenRequested = false
        bluetoothEnableRequestInFlight = false
        viewModelScope.launch(dispatchers.default) {
            controller.close()
        }
    }

    fun onRuntimePermissionsResult() {
        bluetoothEnableRequestInFlight = false
        viewModelScope.launch(dispatchers.default) {
            if (controller.state.value is DiscoveryState.BluetoothDisabled) {
                requestBluetoothEnable()
            } else {
                beginOpenField()
            }
        }
    }

    fun onBluetoothEnableResult() {
        onBluetoothEnableResult(enabled = true)
    }

    fun onBluetoothEnableResult(enabled: Boolean) {
        bluetoothEnableRequestInFlight = false
        if (enabled) {
            fieldOpenRequested = true
            viewModelScope.launch(dispatchers.default) {
                beginOpenField()
            }
        } else {
            fieldOpenRequested = false
            mutableUiState.value = NearbyUiState.BluetoothDisabled
        }
    }

    override fun onCleared() {
        runBlocking {
            try {
                controller.close()
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
        super.onCleared()
    }

    private suspend fun beginOpenField() {
        val missingPermissions = requiredDiscoveryPermissions()
        if (missingPermissions.isNotEmpty()) {
            mutableUiState.value = NearbyUiState.AwaitingPermission(missingPermissions)
            mutableEffects.emit(
                NearbyEffect.RequestRuntimePermissions(missingPermissions),
            )
            return
        }
        controller.open()
    }

    private fun requestBluetoothEnable() {
        bluetoothEnableRequestInFlight = true
        val missingPermissions = permissionReader.missing(DiscoveryOperation.ENABLE_BLUETOOTH)
        if (missingPermissions.isNotEmpty()) {
            mutableUiState.value = NearbyUiState.AwaitingPermission(missingPermissions)
            viewModelScope.launch(dispatchers.default) {
                mutableEffects.emit(
                    NearbyEffect.RequestRuntimePermissions(missingPermissions),
                )
            }
        } else {
            mutableEffects.tryEmit(NearbyEffect.RequestBluetoothEnable)
        }
    }

    private fun requiredDiscoveryPermissions(): Set<DiscoveryPermission> =
        permissionReader.missing(DiscoveryOperation.SCAN) +
            permissionReader.missing(DiscoveryOperation.ADVERTISE)

    private fun DiscoveryState.toUiState(): NearbyUiState = when (this) {
        DiscoveryState.Idle -> NearbyUiState.Idle
        DiscoveryState.Starting -> NearbyUiState.Starting
        DiscoveryState.Stopping -> NearbyUiState.Stopping
        DiscoveryState.Completed -> NearbyUiState.Completed
        DiscoveryState.Unavailable -> NearbyUiState.Unavailable
        DiscoveryState.BluetoothDisabled -> NearbyUiState.BluetoothDisabled
        is DiscoveryState.AwaitingPermission ->
            NearbyUiState.AwaitingPermission(missingPermissions)
        is DiscoveryState.Active -> NearbyUiState.Active(
            remainingDurationMs = remainingDurationMs,
            peerCount = peers.size,
            strongestPeerRssiDbm = peers.maxOfOrNull { it.rssiDbm },
        )
        is DiscoveryState.Error -> NearbyUiState.Error(failure)
    }

    private companion object {
        const val EFFECT_BUFFER_CAPACITY = 8
    }
}
