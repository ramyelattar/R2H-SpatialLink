package com.r2h.spatiallink.identity

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import com.r2h.spatiallink.model.KeySecurityLevel
import java.security.PrivateKey

class KeySecurityLevelInspector(
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) {
    fun inspect(privateKey: PrivateKey): KeySecurityLevel = try {
        if (isApi31OrHigher()) {
            inspectApi31(privateKey)
        } else {
            LegacyKeySecurityLevelReader().read(privateKey)
        }
    } catch (_: Exception) {
        KeySecurityLevel.UNKNOWN
    }

    @RequiresApi(31)
    private fun inspectApi31(privateKey: PrivateKey): KeySecurityLevel =
        Api31KeySecurityLevelReader().read(privateKey)

    @ChecksSdkIntAtLeast(api = 31)
    private fun isApi31OrHigher(): Boolean = sdkInt >= 31
}
