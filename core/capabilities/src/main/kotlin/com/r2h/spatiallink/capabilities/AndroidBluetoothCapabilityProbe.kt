package com.r2h.spatiallink.capabilities

import android.bluetooth.BluetoothManager
import android.content.Context
import com.r2h.spatiallink.model.CapabilityIssue
import com.r2h.spatiallink.model.CapabilityIssueCode
import com.r2h.spatiallink.model.CapabilityProbeResult
import com.r2h.spatiallink.model.CapabilityState
import com.r2h.spatiallink.model.CapabilitySubsystem
import com.r2h.spatiallink.model.RangingTechnology
import com.r2h.spatiallink.model.SpatialCapability

data class BluetoothInspection(
    val lowEnergyFeaturePresent: Boolean,
    val adapterPresent: Boolean,
    val advertisingSupported: Boolean?,
    val issues: List<CapabilityIssue> = emptyList(),
)

interface BluetoothPlatformAccess {
    fun inspect(): BluetoothInspection
}

class AndroidBluetoothPlatformAccess(
    context: Context,
    private val featureReader: SystemFeatureReader,
) : BluetoothPlatformAccess {
    private val applicationContext: Context = context.applicationContext

    override fun inspect(): BluetoothInspection {
        val featurePresent = featureReader.hasFeature(AndroidFeatureNames.BLUETOOTH_LE)
        if (!featurePresent) {
            return BluetoothInspection(
                lowEnergyFeaturePresent = false,
                adapterPresent = false,
                advertisingSupported = null,
            )
        }

        val manager = applicationContext.getSystemService(BluetoothManager::class.java)
        if (manager == null) {
            return BluetoothInspection(
                lowEnergyFeaturePresent = true,
                adapterPresent = false,
                advertisingSupported = null,
                issues = listOf(
                    CapabilityIssue(
                        subsystem = CapabilitySubsystem.BLUETOOTH,
                        code = CapabilityIssueCode.SERVICE_UNAVAILABLE,
                    ),
                ),
            )
        }

        val adapter = manager.adapter
        return BluetoothInspection(
            lowEnergyFeaturePresent = true,
            adapterPresent = adapter != null,
            advertisingSupported = adapter?.isMultipleAdvertisementSupported,
        )
    }
}

class AndroidBluetoothCapabilityProbe(
    private val access: BluetoothPlatformAccess,
) : CapabilityProbe {
    override val subsystem: CapabilitySubsystem = CapabilitySubsystem.BLUETOOTH
    override val spatialCapabilities: Set<SpatialCapability> = setOf(
        SpatialCapability.BLUETOOTH_LE,
        SpatialCapability.BLE_ADVERTISING,
    )
    override val rangingTechnologies: Set<RangingTechnology> = emptySet()

    override suspend fun inspect(): CapabilityProbeResult {
        val inspection = access.inspect()
        val bluetoothLe = when {
            !inspection.lowEnergyFeaturePresent -> CapabilityState.UNAVAILABLE
            inspection.adapterPresent -> CapabilityState.AVAILABLE
            else -> CapabilityState.UNKNOWN
        }
        val advertising = when {
            !inspection.lowEnergyFeaturePresent -> CapabilityState.UNAVAILABLE
            !inspection.adapterPresent -> CapabilityState.UNKNOWN
            inspection.advertisingSupported == true -> CapabilityState.AVAILABLE
            inspection.advertisingSupported == false -> CapabilityState.UNAVAILABLE
            else -> CapabilityState.UNKNOWN
        }

        return CapabilityProbeResult(
            subsystem = subsystem,
            spatialCapabilities = mapOf(
                SpatialCapability.BLUETOOTH_LE to bluetoothLe,
                SpatialCapability.BLE_ADVERTISING to advertising,
            ),
            issues = inspection.issues.toList(),
        )
    }
}
