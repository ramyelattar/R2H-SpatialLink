package com.r2h.spatiallink.discovery

enum class DiscoveryOperation {
    SCAN,
    ADVERTISE,
    ENABLE_BLUETOOTH,
}

enum class DiscoveryPermission {
    LEGACY_FINE_LOCATION,
    BLUETOOTH_SCAN,
    BLUETOOTH_ADVERTISE,
    BLUETOOTH_CONNECT,
}

object DiscoveryPermissionPolicy {
    fun required(sdkInt: Int, operation: DiscoveryOperation): Set<DiscoveryPermission> =
        when (operation) {
            DiscoveryOperation.SCAN -> {
                when {
                    sdkInt <= 30 -> setOf(DiscoveryPermission.LEGACY_FINE_LOCATION)
                    else -> setOf(DiscoveryPermission.BLUETOOTH_SCAN)
                }
            }

            DiscoveryOperation.ADVERTISE -> {
                when {
                    sdkInt <= 30 -> emptySet()
                    else -> setOf(DiscoveryPermission.BLUETOOTH_ADVERTISE)
                }
            }

            DiscoveryOperation.ENABLE_BLUETOOTH -> {
                when {
                    sdkInt <= 30 -> emptySet()
                    else -> setOf(DiscoveryPermission.BLUETOOTH_CONNECT)
                }
            }
        }

    fun requiredForDiscoverySession(sdkInt: Int): Set<DiscoveryPermission> =
        required(sdkInt, DiscoveryOperation.SCAN) +
            required(sdkInt, DiscoveryOperation.ADVERTISE)
}
