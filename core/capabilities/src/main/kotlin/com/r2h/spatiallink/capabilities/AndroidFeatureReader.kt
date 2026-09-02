package com.r2h.spatiallink.capabilities

import android.content.pm.PackageManager

object AndroidFeatureNames {
    const val BLUETOOTH_LE = "android.hardware.bluetooth_le"
    const val WIFI_DIRECT = "android.hardware.wifi.direct"
    const val WIFI_AWARE = "android.hardware.wifi.aware"
    const val WIFI_RTT = "android.hardware.wifi.rtt"
    const val NFC = "android.hardware.nfc"
    const val NFC_HCE = "android.hardware.nfc.hce"
    const val UWB = "android.hardware.uwb"
}

interface SystemFeatureReader {
    fun hasFeature(featureName: String): Boolean
}

class AndroidSystemFeatureReader(
    private val packageManager: PackageManager,
) : SystemFeatureReader {
    override fun hasFeature(featureName: String): Boolean = packageManager.hasSystemFeature(featureName)
}
