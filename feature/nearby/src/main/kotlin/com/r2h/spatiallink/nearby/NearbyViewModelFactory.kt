package com.r2h.spatiallink.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.r2h.spatiallink.common.DispatcherProvider
import com.r2h.spatiallink.discovery.DiscoveryControllerFactory
import com.r2h.spatiallink.discovery.DiscoveryPermissionReader

class NearbyViewModelFactory(
    private val controllerFactory: DiscoveryControllerFactory,
    private val permissionReader: DiscoveryPermissionReader,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(NearbyViewModel::class.java)) {
            throw IllegalArgumentException("Unsupported ViewModel: \${modelClass.name}")
        }

        @Suppress("UNCHECKED_CAST")
        return NearbyViewModel(
            controller = controllerFactory.create(),
            permissionReader = permissionReader,
            dispatchers = dispatcherProvider,
        ) as T
    }
}
