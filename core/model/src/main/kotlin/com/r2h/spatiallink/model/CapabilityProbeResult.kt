package com.r2h.spatiallink.model

data class CapabilityProbeResult(
    val subsystem: CapabilitySubsystem,
    val spatialCapabilities: Map<SpatialCapability, CapabilityState> = emptyMap(),
    val rangingTechnologies: Map<RangingTechnology, CapabilityState> = emptyMap(),
    val issues: List<CapabilityIssue> = emptyList(),
) {
    companion object {
        fun allUnavailable(): CapabilityProbeResult = CapabilityProbeResult(
            subsystem = CapabilitySubsystem.SYNTHETIC,
            spatialCapabilities = SpatialCapability.values().associateWith { CapabilityState.UNAVAILABLE },
            rangingTechnologies = RangingTechnology.values().associateWith { CapabilityState.UNAVAILABLE },
        )
    }
}
