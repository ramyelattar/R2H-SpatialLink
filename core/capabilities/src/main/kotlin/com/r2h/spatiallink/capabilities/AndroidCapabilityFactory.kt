package com.r2h.spatiallink.capabilities

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.r2h.spatiallink.common.DefaultDispatcherProvider
import com.r2h.spatiallink.common.DispatcherProvider
import com.r2h.spatiallink.model.DeviceCapabilityDetector

class AndroidCapabilityFactory(
    context: Context,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
) {
    private val applicationContext: Context = context.applicationContext

    fun createDetector(): DeviceCapabilityDetector {
        val sdkInt = Build.VERSION.SDK_INT
        val featureReader = AndroidSystemFeatureReader(applicationContext.packageManager)
        val probes = listOf(
            AndroidBluetoothCapabilityProbe(
                AndroidBluetoothPlatformAccess(applicationContext, featureReader),
            ),
            AndroidWifiCapabilityProbe(
                AndroidWifiPlatformAccess(applicationContext, featureReader),
            ),
            AndroidNfcUwbCapabilityProbe(
                sdkInt = sdkInt,
                access = AndroidNfcUwbPlatformAccess(
                    context = applicationContext,
                    sdkInt = sdkInt,
                    featureReader = featureReader,
                ),
            ),
            createRangingProbe(applicationContext, sdkInt),
        )

        return AndroidDeviceCapabilityDetector(
            sdkInt = sdkInt,
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
            probes = probes,
            dispatchers = dispatchers,
        )
    }

    private fun createRangingProbe(
        context: Context,
        sdkInt: Int,
    ): CapabilityProbe = if (Build.VERSION.SDK_INT >= 36) {
        createGuardedRangingProbe(context, sdkInt)
    } else {
        UnsupportedPlatformRangingCapabilityProbe()
    }

    @RequiresApi(36)
    private fun createGuardedRangingProbe(
        context: Context,
        sdkInt: Int,
    ): CapabilityProbe = createApi36RangingCapabilityProbe(context, sdkInt)
}
