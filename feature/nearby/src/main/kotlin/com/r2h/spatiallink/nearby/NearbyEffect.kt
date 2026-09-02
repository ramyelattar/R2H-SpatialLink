package com.r2h.spatiallink.nearby

import com.r2h.spatiallink.discovery.DiscoveryPermission

sealed interface NearbyEffect {
    data class RequestRuntimePermissions(
        val permissions: Set<DiscoveryPermission>,
    ) : NearbyEffect
    data object RequestBluetoothEnable : NearbyEffect
    data object LeaveCompleted : NearbyEffect
}
