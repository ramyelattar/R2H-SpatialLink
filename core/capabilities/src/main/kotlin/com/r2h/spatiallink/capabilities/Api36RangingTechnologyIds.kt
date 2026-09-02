package com.r2h.spatiallink.capabilities

import android.ranging.RangingManager
import androidx.annotation.RequiresApi

@RequiresApi(36)
object Api36RangingTechnologyIds {
    const val UWB: Int = RangingManager.UWB
    const val BLE_CS: Int = RangingManager.BLE_CS
    const val WIFI_NAN_RTT: Int = RangingManager.WIFI_NAN_RTT
    const val BLE_RSSI: Int = RangingManager.BLE_RSSI
}
