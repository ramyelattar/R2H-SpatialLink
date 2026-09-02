package com.r2h.spatiallink.capabilities

import android.content.Context
import android.annotation.SuppressLint
import android.net.wifi.WifiManager
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.rtt.WifiRttManager
import com.r2h.spatiallink.model.CapabilityIssue
import com.r2h.spatiallink.model.CapabilityIssueCode
import com.r2h.spatiallink.model.CapabilityProbeResult
import com.r2h.spatiallink.model.CapabilityState
import com.r2h.spatiallink.model.CapabilitySubsystem
import com.r2h.spatiallink.model.RangingTechnology
import com.r2h.spatiallink.model.SpatialCapability

data class WifiInspection(
    val wifiDirect: CapabilityState,
    val wifiAware: CapabilityState,
    val wifiRtt: CapabilityState,
    val issues: List<CapabilityIssue> = emptyList(),
)

interface WifiPlatformAccess {
    fun inspect(): WifiInspection
}

class AndroidWifiPlatformAccess(
    context: Context,
    private val featureReader: SystemFeatureReader,
) : WifiPlatformAccess {
    private val applicationContext: Context = context.applicationContext

    override fun inspect(): WifiInspection {
        val issues = mutableListOf<CapabilityIssue>()
        val direct = inspectWifiDirect(issues)
        val aware = inspectWifiAware(issues)
        val rtt = inspectWifiRtt(issues)
        return WifiInspection(
            wifiDirect = direct,
            wifiAware = aware,
            wifiRtt = rtt,
            issues = issues.toList(),
        )
    }

    @SuppressLint("MissingPermission")
    private fun inspectWifiDirect(issues: MutableList<CapabilityIssue>): CapabilityState {
        if (!featureReader.hasFeature(AndroidFeatureNames.WIFI_DIRECT)) {
            return CapabilityState.UNAVAILABLE
        }

        val manager = applicationContext.getSystemService(WifiManager::class.java)
        if (manager == null) {
            issues += serviceIssue()
            return CapabilityState.UNKNOWN
        }

        return try {
            if (manager.isP2pSupported) CapabilityState.AVAILABLE else CapabilityState.UNAVAILABLE
        } catch (_: SecurityException) {
            issues += securityIssue()
            CapabilityState.UNKNOWN
        } catch (_: Exception) {
            issues += frameworkIssue()
            CapabilityState.UNKNOWN
        }
    }

    @SuppressLint("MissingPermission")
    private fun inspectWifiAware(issues: MutableList<CapabilityIssue>): CapabilityState {
        if (!featureReader.hasFeature(AndroidFeatureNames.WIFI_AWARE)) {
            return CapabilityState.UNAVAILABLE
        }

        val manager = applicationContext.getSystemService(WifiAwareManager::class.java)
        if (manager == null) {
            issues += serviceIssue()
            return CapabilityState.UNKNOWN
        }

        return try {
            if (manager.isAvailable) CapabilityState.AVAILABLE else CapabilityState.UNAVAILABLE
        } catch (_: SecurityException) {
            issues += securityIssue()
            CapabilityState.UNKNOWN
        } catch (_: Exception) {
            issues += frameworkIssue()
            CapabilityState.UNKNOWN
        }
    }

    @SuppressLint("MissingPermission")
    private fun inspectWifiRtt(issues: MutableList<CapabilityIssue>): CapabilityState {
        if (!featureReader.hasFeature(AndroidFeatureNames.WIFI_RTT)) {
            return CapabilityState.UNAVAILABLE
        }

        val manager = applicationContext.getSystemService(WifiRttManager::class.java)
        if (manager == null) {
            issues += serviceIssue()
            return CapabilityState.UNKNOWN
        }

        return try {
            if (manager.isAvailable) CapabilityState.AVAILABLE else CapabilityState.UNAVAILABLE
        } catch (_: SecurityException) {
            issues += securityIssue()
            CapabilityState.UNKNOWN
        } catch (_: Exception) {
            issues += frameworkIssue()
            CapabilityState.UNKNOWN
        }
    }

    private fun serviceIssue() = CapabilityIssue(
        subsystem = CapabilitySubsystem.WIFI,
        code = CapabilityIssueCode.SERVICE_UNAVAILABLE,
    )

    private fun securityIssue() = CapabilityIssue(
        subsystem = CapabilitySubsystem.WIFI,
        code = CapabilityIssueCode.SECURITY_FAILURE,
    )

    private fun frameworkIssue() = CapabilityIssue(
        subsystem = CapabilitySubsystem.WIFI,
        code = CapabilityIssueCode.UNEXPECTED_FAILURE,
    )
}

class AndroidWifiCapabilityProbe(
    private val access: WifiPlatformAccess,
) : CapabilityProbe {
    override val subsystem: CapabilitySubsystem = CapabilitySubsystem.WIFI
    override val spatialCapabilities: Set<SpatialCapability> = setOf(
        SpatialCapability.WIFI_DIRECT,
        SpatialCapability.WIFI_AWARE,
        SpatialCapability.WIFI_RTT,
    )
    override val rangingTechnologies: Set<RangingTechnology> = emptySet()

    override suspend fun inspect(): CapabilityProbeResult {
        val inspection = access.inspect()
        return CapabilityProbeResult(
            subsystem = subsystem,
            spatialCapabilities = mapOf(
                SpatialCapability.WIFI_DIRECT to inspection.wifiDirect,
                SpatialCapability.WIFI_AWARE to inspection.wifiAware,
                SpatialCapability.WIFI_RTT to inspection.wifiRtt,
            ),
            issues = inspection.issues.toList(),
        )
    }
}
