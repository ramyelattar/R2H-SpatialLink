package com.r2h.spatiallink.model

enum class FoundationStatus {
    READY,
    PARTIAL,
    ERROR,
}

fun deriveFoundationStatus(
    capabilities: DeviceCapabilities?,
    fatalError: Boolean = false,
): FoundationStatus {
    if (fatalError || capabilities == null) {
        return FoundationStatus.ERROR
    }

    val hasUnknownState = capabilities.spatialStates().any { it == CapabilityState.UNKNOWN } ||
        capabilities.ranging.technologies.values.any { it == CapabilityState.UNKNOWN }

    return if (hasUnknownState || capabilities.issues.isNotEmpty()) {
        FoundationStatus.PARTIAL
    } else {
        FoundationStatus.READY
    }
}
