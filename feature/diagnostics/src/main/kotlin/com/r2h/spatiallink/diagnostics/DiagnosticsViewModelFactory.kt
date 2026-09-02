package com.r2h.spatiallink.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.r2h.spatiallink.common.DispatcherProvider
import com.r2h.spatiallink.model.DeviceCapabilityDetector
import com.r2h.spatiallink.model.DeviceIdentityRepository
import com.r2h.spatiallink.model.PermissionStateReader

class DiagnosticsViewModelFactory(
    private val capabilityDetector: DeviceCapabilityDetector,
    private val permissionStateReader: PermissionStateReader,
    private val identityRepository: DeviceIdentityRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(DiagnosticsViewModel::class.java)) {
            throw IllegalArgumentException("Unsupported ViewModel: ${modelClass.name}")
        }

        @Suppress("UNCHECKED_CAST")
        return DiagnosticsViewModel(
            capabilityDetector = capabilityDetector,
            permissionStateReader = permissionStateReader,
            identityRepository = identityRepository,
            dispatchers = dispatcherProvider,
        ) as T
    }
}
