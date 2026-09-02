package com.r2h.spatiallink.capabilities

import com.r2h.spatiallink.model.CapabilityIssue
import com.r2h.spatiallink.model.CapabilityProbeResult
import com.r2h.spatiallink.model.CapabilityState
import com.r2h.spatiallink.model.CapabilitySubsystem
import com.r2h.spatiallink.model.RangingTechnology
import com.r2h.spatiallink.model.SpatialCapability

class UnsupportedPlatformRangingCapabilityProbe : CapabilityProbe {
    override val subsystem: CapabilitySubsystem = CapabilitySubsystem.PLATFORM_RANGING
    override val spatialCapabilities: Set<SpatialCapability> = setOf(SpatialCapability.PLATFORM_RANGING)
    override val rangingTechnologies: Set<RangingTechnology> = RangingTechnology.values().toSet()

    override suspend fun inspect(): CapabilityProbeResult = CapabilityProbeResult(
        subsystem = subsystem,
        spatialCapabilities = mapOf(SpatialCapability.PLATFORM_RANGING to CapabilityState.UNAVAILABLE),
        rangingTechnologies = RangingTechnology.values().associateWith { CapabilityState.UNAVAILABLE },
    )
}

class UnavailablePlatformRangingCapabilityProbe(
    private val issue: CapabilityIssue,
) : CapabilityProbe {
    override val subsystem: CapabilitySubsystem = CapabilitySubsystem.PLATFORM_RANGING
    override val spatialCapabilities: Set<SpatialCapability> = setOf(SpatialCapability.PLATFORM_RANGING)
    override val rangingTechnologies: Set<RangingTechnology> = RangingTechnology.values().toSet()

    override suspend fun inspect(): CapabilityProbeResult = CapabilityProbeResult(
        subsystem = subsystem,
        spatialCapabilities = mapOf(SpatialCapability.PLATFORM_RANGING to CapabilityState.UNKNOWN),
        rangingTechnologies = RangingTechnology.values().associateWith { CapabilityState.UNKNOWN },
        issues = listOf(issue),
    )
}
