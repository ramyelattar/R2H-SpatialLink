package com.r2h.spatiallink.identity

import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import com.r2h.spatiallink.model.KeySecurityLevel
import java.security.KeyFactory
import java.security.PrivateKey

@Suppress("DEPRECATION")
class LegacyKeySecurityLevelReader {
    fun read(privateKey: PrivateKey): KeySecurityLevel {
        val keyInfo = KeyFactory
            .getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
            .getKeySpec(privateKey, KeyInfo::class.java)
        return if (keyInfo.isInsideSecureHardware) {
            KeySecurityLevel.UNKNOWN_SECURE
        } else {
            KeySecurityLevel.SOFTWARE
        }
    }
}
