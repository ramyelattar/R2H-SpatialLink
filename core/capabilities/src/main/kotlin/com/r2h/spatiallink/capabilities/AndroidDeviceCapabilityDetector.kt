package com.r2h.spatiallink.capabilities

import com.r2h.spatiallink.common.DispatcherProvider
import com.r2h.spatiallink.model.CapabilityIssueCode
import com.r2h.spatiallink.model.CapabilitySnapshotAssembler
import com.r2h.spatiallink.model.DeviceCapabilities
import com.r2h.spatiallink.model.DeviceCapabilityDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

class AndroidDeviceCapabilityDetector(
    private val sdkInt: Int,
    private val androidVersion: String,
    private val probes: List<CapabilityProbe>,
    private val dispatchers: DispatcherProvider,
) : DeviceCapabilityDetector {
    override suspend fun detect(): DeviceCapabilities = withContext(dispatchers.default) {
        supervisorScope {
            val probeJobs = probes.map { probe ->
                async { inspectSafely(probe) }
            }

            CapabilitySnapshotAssembler.assemble(
                sdkInt = sdkInt,
                androidVersion = androidVersion,
                results = probeJobs.awaitAll(),
            )
        }
    }

    private suspend fun inspectSafely(probe: CapabilityProbe) = try {
        probe.inspect()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SecurityException) {
        unknownResultFor(probe, CapabilityIssueCode.SECURITY_FAILURE)
    } catch (_: ServiceUnavailableException) {
        unknownResultFor(probe, CapabilityIssueCode.SERVICE_UNAVAILABLE)
    } catch (_: Exception) {
        unknownResultFor(probe, CapabilityIssueCode.UNEXPECTED_FAILURE)
    }
}
