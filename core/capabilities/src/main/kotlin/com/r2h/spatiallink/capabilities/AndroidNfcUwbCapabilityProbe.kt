package com.r2h.spatiallink.capabilities

import android.content.Context
import android.nfc.NfcAdapter
import com.r2h.spatiallink.model.CapabilityIssue
import com.r2h.spatiallink.model.CapabilityIssueCode
import com.r2h.spatiallink.model.CapabilityProbeResult
import com.r2h.spatiallink.model.CapabilityState
import com.r2h.spatiallink.model.CapabilitySubsystem
import com.r2h.spatiallink.model.RangingTechnology
import com.r2h.spatiallink.model.SpatialCapability

data class NfcUwbInspection(
    val nfc: CapabilityState,
    val nfcHce: CapabilityState,
    val uwb: CapabilityState,
    val issues: List<CapabilityIssue> = emptyList(),
)

interface NfcUwbPlatformAccess {
    fun inspect(): NfcUwbInspection
}

class AndroidNfcUwbPlatformAccess(
    context: Context,
    private val sdkInt: Int,
    private val featureReader: SystemFeatureReader,
) : NfcUwbPlatformAccess {
    private val applicationContext: Context = context.applicationContext

    override fun inspect(): NfcUwbInspection {
        val issues = mutableListOf<CapabilityIssue>()
        val nfcFeaturePresent = featureReader.hasFeature(AndroidFeatureNames.NFC)
        val nfc = if (!nfcFeaturePresent) {
            CapabilityState.UNAVAILABLE
        } else {
            try {
                if (NfcAdapter.getDefaultAdapter(applicationContext) != null) {
                    CapabilityState.AVAILABLE
                } else {
                    issues += serviceIssue()
                    CapabilityState.UNKNOWN
                }
            } catch (_: SecurityException) {
                issues += securityIssue()
                CapabilityState.UNKNOWN
            } catch (_: Exception) {
                issues += frameworkIssue()
                CapabilityState.UNKNOWN
            }
        }

        val nfcHce = if (featureReader.hasFeature(AndroidFeatureNames.NFC_HCE)) {
            CapabilityState.AVAILABLE
        } else {
            CapabilityState.UNAVAILABLE
        }

        val uwb = if (sdkInt < 31) {
            CapabilityState.UNAVAILABLE
        } else if (featureReader.hasFeature(AndroidFeatureNames.UWB)) {
            CapabilityState.AVAILABLE
        } else {
            CapabilityState.UNAVAILABLE
        }

        return NfcUwbInspection(
            nfc = nfc,
            nfcHce = nfcHce,
            uwb = uwb,
            issues = issues.toList(),
        )
    }

    private fun serviceIssue() = CapabilityIssue(
        subsystem = CapabilitySubsystem.NFC_AND_UWB,
        code = CapabilityIssueCode.SERVICE_UNAVAILABLE,
    )

    private fun securityIssue() = CapabilityIssue(
        subsystem = CapabilitySubsystem.NFC_AND_UWB,
        code = CapabilityIssueCode.SECURITY_FAILURE,
    )

    private fun frameworkIssue() = CapabilityIssue(
        subsystem = CapabilitySubsystem.NFC_AND_UWB,
        code = CapabilityIssueCode.UNEXPECTED_FAILURE,
    )
}

class AndroidNfcUwbCapabilityProbe(
    private val sdkInt: Int,
    private val access: NfcUwbPlatformAccess,
) : CapabilityProbe {
    override val subsystem: CapabilitySubsystem = CapabilitySubsystem.NFC_AND_UWB
    override val spatialCapabilities: Set<SpatialCapability> = setOf(
        SpatialCapability.NFC,
        SpatialCapability.NFC_HCE,
        SpatialCapability.UWB,
    )
    override val rangingTechnologies: Set<RangingTechnology> = emptySet()

    override suspend fun inspect(): CapabilityProbeResult {
        val inspection = access.inspect()
        return CapabilityProbeResult(
            subsystem = subsystem,
            spatialCapabilities = mapOf(
                SpatialCapability.NFC to inspection.nfc,
                SpatialCapability.NFC_HCE to inspection.nfcHce,
                SpatialCapability.UWB to if (sdkInt < 31) CapabilityState.UNAVAILABLE else inspection.uwb,
            ),
            issues = inspection.issues.toList(),
        )
    }
}
