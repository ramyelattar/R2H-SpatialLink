package com.r2h.spatiallink.model

interface DeviceCapabilityDetector {
    suspend fun detect(): DeviceCapabilities
}

interface PermissionStateReader {
    fun snapshot(): PermissionSnapshot
}
