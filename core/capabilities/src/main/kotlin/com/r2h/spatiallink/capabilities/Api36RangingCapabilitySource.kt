package com.r2h.spatiallink.capabilities

import android.content.Context
import android.ranging.RangingCapabilities
import android.ranging.RangingManager
import androidx.annotation.RequiresApi
import com.r2h.spatiallink.model.CapabilityIssue
import com.r2h.spatiallink.model.CapabilityIssueCode
import com.r2h.spatiallink.model.CapabilitySubsystem
import kotlinx.coroutines.CancellationException
import java.util.concurrent.Executor

data class PlatformRangingInspection(
    val availability: Map<Int, Int> = emptyMap(),
    val issues: List<CapabilityIssue> = emptyList(),
)

interface PlatformRangingCapabilitySource {
    suspend fun inspect(): PlatformRangingInspection
}

@RequiresApi(36)
class Api36RangingCapabilitySource(
    private val manager: RangingManager,
    private val executor: Executor,
) : PlatformRangingCapabilitySource {
    override suspend fun inspect(): PlatformRangingInspection {
        val registration = AndroidRangingCallbackRegistration(manager, executor)
        return try {
            PlatformRangingInspection(
                availability = CancellableRangingCapabilityReader(registration).read(),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            PlatformRangingInspection(
                issues = listOf(issue(CapabilityIssueCode.SECURITY_FAILURE)),
            )
        } catch (_: ServiceUnavailableException) {
            PlatformRangingInspection(
                issues = listOf(issue(CapabilityIssueCode.SERVICE_UNAVAILABLE)),
            )
        } catch (_: Exception) {
            PlatformRangingInspection(
                issues = listOf(issue(CapabilityIssueCode.CALLBACK_FAILURE)),
            )
        }
    }

    private fun issue(code: CapabilityIssueCode) = CapabilityIssue(
        subsystem = CapabilitySubsystem.PLATFORM_RANGING,
        code = code,
    )
}

@RequiresApi(36)
private class AndroidRangingCallbackRegistration(
    private val manager: RangingManager,
    private val executor: Executor,
) : RangingCallbackRegistration {
    private val callbackLock = Any()
    private var registeredCallback: RangingManager.RangingCapabilitiesCallback? = null

    override fun register(callback: (Map<Int, Int>) -> Unit) {
        val platformCallback = object : RangingManager.RangingCapabilitiesCallback {
            override fun onRangingCapabilities(capabilities: RangingCapabilities) {
                callback(capabilities.technologyAvailability.toMap())
            }
        }

        synchronized(callbackLock) {
            check(registeredCallback == null) { "Ranging callback registration is already active." }
            registeredCallback = platformCallback
        }

        manager.registerCapabilitiesCallback(executor, platformCallback)
    }

    override fun unregister() {
        val callbackToUnregister = synchronized(callbackLock) { registeredCallback }
        if (callbackToUnregister == null) {
            return
        }

        try {
            manager.unregisterCapabilitiesCallback(callbackToUnregister)
        } finally {
            synchronized(callbackLock) {
                if (registeredCallback === callbackToUnregister) {
                    registeredCallback = null
                }
            }
        }
    }
}

@RequiresApi(36)
fun createApi36RangingCapabilityProbe(
    context: Context,
    sdkInt: Int,
): CapabilityProbe {
    val applicationContext = context.applicationContext
    val manager = applicationContext.getSystemService(RangingManager::class.java)
        ?: return UnavailablePlatformRangingCapabilityProbe(
            issue = CapabilityIssue(
                subsystem = CapabilitySubsystem.PLATFORM_RANGING,
                code = CapabilityIssueCode.SERVICE_UNAVAILABLE,
            ),
        )

    val wifiProximityDetectionId = if (android.os.Build.VERSION.SDK_INT >= 37) {
        api37WifiProximityDetectionId()
    } else {
        null
    }

    return AndroidPlatformRangingCapabilityProbe(
        source = Api36RangingCapabilitySource(
            manager = manager,
            executor = applicationContext.mainExecutor,
        ),
        sdkInt = sdkInt,
        wifiProximityDetectionId = wifiProximityDetectionId,
    )
}

@RequiresApi(37)
private fun api37WifiProximityDetectionId(): Int = Api37RangingTechnologyIds.WIFI_PD
