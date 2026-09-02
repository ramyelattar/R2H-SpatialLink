package com.r2h.spatiallink.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FoundationStatusTest {
    @Test
    fun unknown_state_derives_partial() {
        val capabilities = CapabilityProbeResult.allUnavailable()
            .let {
                CapabilitySnapshotAssembler.assemble(
                    sdkInt = 29,
                    androidVersion = "10",
                    results = listOf(it),
                )
            }
            .copy(bluetoothLe = CapabilityState.UNKNOWN)

        assertEquals(FoundationStatus.PARTIAL, deriveFoundationStatus(capabilities))
    }

    @Test
    fun recoverable_issue_derives_partial() {
        val capabilities = CapabilitySnapshotAssembler.assemble(
            sdkInt = 29,
            androidVersion = "10",
            results = listOf(
                CapabilityProbeResult.allUnavailable().copy(
                    issues = listOf(
                        CapabilityIssue(
                            subsystem = CapabilitySubsystem.BLUETOOTH,
                            code = CapabilityIssueCode.SECURITY_FAILURE,
                        ),
                    ),
                ),
            ),
        )

        assertEquals(FoundationStatus.PARTIAL, deriveFoundationStatus(capabilities))
    }

    @Test
    fun absent_snapshot_or_fatal_failure_derives_error() {
        assertEquals(FoundationStatus.ERROR, deriveFoundationStatus(null))
        assertEquals(
            FoundationStatus.ERROR,
            deriveFoundationStatus(CapabilityProbeResult.allUnavailable().let {
                CapabilitySnapshotAssembler.assemble(29, "10", listOf(it))
            }, fatalError = true),
        )
    }
}
