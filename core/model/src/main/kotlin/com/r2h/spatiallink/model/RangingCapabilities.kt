package com.r2h.spatiallink.model

data class RangingCapabilities(
    val technologies: Map<RangingTechnology, CapabilityState>,
    val issues: List<CapabilityIssue> = emptyList(),
) {
    operator fun get(technology: RangingTechnology): CapabilityState? = technologies[technology]
}
