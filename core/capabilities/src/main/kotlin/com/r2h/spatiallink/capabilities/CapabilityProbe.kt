package com.r2h.spatiallink.capabilities

import com.r2h.spatiallink.model.CapabilityProbeResult
import com.r2h.spatiallink.model.CapabilitySubsystem
import com.r2h.spatiallink.model.RangingTechnology
import com.r2h.spatiallink.model.SpatialCapability

interface CapabilityProbe {
    val subsystem: CapabilitySubsystem
    val spatialCapabilities: Set<SpatialCapability>
    val rangingTechnologies: Set<RangingTechnology>

    suspend fun inspect(): CapabilityProbeResult
}

class ServiceUnavailableException(
    message: String,
) : IllegalStateException(message)

internal fun unknownResultFor(probe: CapabilityProbe, issueCode: com.r2h.spatiallink.model.CapabilityIssueCode): CapabilityProbeResult =
    CapabilityProbeResult(
        subsystem = probe.subsystem,
        spatialCapabilities = probe.spatialCapabilities.associateWith { com.r2h.spatiallink.model.CapabilityState.UNKNOWN },
        rangingTechnologies = probe.rangingTechnologies.associateWith { com.r2h.spatiallink.model.CapabilityState.UNKNOWN },
        issues = listOf(
            com.r2h.spatiallink.model.CapabilityIssue(
                subsystem = probe.subsystem,
                code = issueCode,
            ),
        ),
    )
