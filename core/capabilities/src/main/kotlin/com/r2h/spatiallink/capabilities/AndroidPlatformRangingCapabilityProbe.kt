package com.r2h.spatiallink.capabilities

import android.ranging.RangingCapabilities
import androidx.annotation.RequiresApi
import com.r2h.spatiallink.model.CapabilityIssue
import com.r2h.spatiallink.model.CapabilityIssueCode
import com.r2h.spatiallink.model.CapabilityProbeResult
import com.r2h.spatiallink.model.CapabilityState
import com.r2h.spatiallink.model.CapabilitySubsystem
import com.r2h.spatiallink.model.RangingTechnology
import com.r2h.spatiallink.model.SpatialCapability

@RequiresApi(36)
class AndroidPlatformRangingCapabilityProbe(
    private val source: PlatformRangingCapabilitySource,
    private val sdkInt: Int,
    private val wifiProximityDetectionId: Int?,
) : CapabilityProbe {
    override val subsystem: CapabilitySubsystem = CapabilitySubsystem.PLATFORM_RANGING
    override val spatialCapabilities: Set<SpatialCapability> = setOf(SpatialCapability.PLATFORM_RANGING)
    override val rangingTechnologies: Set<RangingTechnology> = RangingTechnology.values().toSet()

    override suspend fun inspect(): CapabilityProbeResult {
        val inspection = source.inspect()
        val issues = inspection.issues.toMutableList()
        val states = RangingTechnology.values().associateWith { technology ->
            stateFor(technology, inspection.availability, inspection.issues.isNotEmpty(), issues)
        }

        val platformState = when {
            inspection.availability.values.any { it == RangingCapabilities.ENABLED } -> CapabilityState.AVAILABLE
            inspection.issues.isNotEmpty() -> CapabilityState.UNKNOWN
            states.values.any { it == CapabilityState.UNKNOWN } -> CapabilityState.UNKNOWN
            else -> CapabilityState.UNAVAILABLE
        }

        return CapabilityProbeResult(
            subsystem = subsystem,
            spatialCapabilities = mapOf(SpatialCapability.PLATFORM_RANGING to platformState),
            rangingTechnologies = states,
            issues = issues.toList(),
        )
    }

    private fun stateFor(
        technology: RangingTechnology,
        availability: Map<Int, Int>,
        sourceFailed: Boolean,
        issues: MutableList<CapabilityIssue>,
    ): CapabilityState {
        val id = idFor(technology)
            ?: return CapabilityState.UNAVAILABLE
        val status = availability[id]
            ?: return if (sourceFailed) CapabilityState.UNKNOWN else CapabilityState.UNAVAILABLE

        return when (status) {
            RangingCapabilities.ENABLED -> CapabilityState.AVAILABLE
            RangingCapabilities.NOT_SUPPORTED,
            RangingCapabilities.DISABLED_REGULATORY,
            RangingCapabilities.DISABLED_USER,
            RangingCapabilities.DISABLED_USER_RESTRICTIONS,
            -> CapabilityState.UNAVAILABLE

            else -> {
                issues += CapabilityIssue(
                    subsystem = subsystem,
                    code = CapabilityIssueCode.UNKNOWN_STATUS,
                    detail = technology.name,
                )
                CapabilityState.UNKNOWN
            }
        }
    }

    private fun idFor(technology: RangingTechnology): Int? = when (technology) {
        RangingTechnology.UWB -> Api36RangingTechnologyIds.UWB
        RangingTechnology.BLE_CHANNEL_SOUNDING -> Api36RangingTechnologyIds.BLE_CS
        RangingTechnology.WIFI_NAN_RTT -> Api36RangingTechnologyIds.WIFI_NAN_RTT
        RangingTechnology.BLE_RSSI -> Api36RangingTechnologyIds.BLE_RSSI
        RangingTechnology.WIFI_PROXIMITY_DETECTION -> if (sdkInt >= 37) {
            wifiProximityDetectionId
        } else {
            null
        }
    }
}
